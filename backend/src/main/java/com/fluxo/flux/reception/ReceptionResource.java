package com.fluxo.flux.reception;

import com.fluxo.flux.domain.Emplacement;
import com.fluxo.flux.mouvement.MouvementDto;
import com.fluxo.referentiel.domain.Article;
import com.fluxo.referentiel.domain.Tiers;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST des RÉCEPTIONS — le premier flux complet.
 *
 *  GET    /receptions                        liste (?fournisseur= &etat= &depuis= &jusqua=)
 *  GET    /receptions/{id}                   détail, lignes comprises
 *  POST   /receptions                        création (lignes facultatives) — 201
 *  PATCH  /receptions/{id}                   modifier l'en-tête (BL, transporteur, pesée)
 *  DELETE /receptions/{id}                   supprimer — seulement en BROUILLON
 *  POST   /receptions/{id}/lignes            ajouter une ligne — 201
 *  PUT    /receptions/{id}/lignes/{ligneId}  corriger une ligne
 *  DELETE /receptions/{id}/lignes/{ligneId}  retirer une ligne
 *  POST   /receptions/{id}/valider           ⭐ génère les mouvements — le stock apparaît
 *  POST   /receptions/{id}/annuler           abandonner (aucun mouvement produit)
 *
 * ⭐ LA RÈGLE QUI STRUCTURE TOUT : tant que la réception est en BROUILLON, tout est
 * modifiable ; dès qu'elle est VALIDEE, plus RIEN ne l'est (409). Le réceptionnaire
 * corrige librement pendant qu'il déballe, et le document se fige au moment où il
 * devient un fait.
 *
 * 📌 Le contraste avec MouvementResource est VOLONTAIRE :
 *      Reception → mutable jusqu'à validation (c'est un brouillon de travail)
 *      Mouvement → JAMAIS modifiable (c'est un fait passé)
 * Une erreur découverte après validation ne se corrige donc pas ici : on inverse les
 * mouvements concernés (POST /mouvements/{id}/inverser).
 *
 * 🧩 La Reception est la RACINE : les lignes n'ont pas d'existence propre, d'où leurs URLs
 * imbriquées (/receptions/{id}/lignes/...) et jamais un /lignes-reception de premier niveau.
 */
@Path("/receptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReceptionResource {

    // ─────────────────────────────── LECTURE ───────────────────────────────

    @GET
    @Transactional
    public List<ReceptionDto> liste(@QueryParam("fournisseur") String fournisseur,
                                    @QueryParam("etat") EtatReception etat,
                                    @QueryParam("depuis") LocalDateTime depuis,
                                    @QueryParam("jusqua") LocalDateTime jusqua) {
        StringBuilder jpql = new StringBuilder("from Reception r");
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (fournisseur != null) {
            conditions.add("r.fournisseur.code = :fournisseur");
            params.put("fournisseur", fournisseur);
        }
        if (etat != null)   { conditions.add("r.etat = :etat");                 params.put("etat", etat); }
        if (depuis != null) { conditions.add("r.dateReception >= :depuis");     params.put("depuis", depuis); }
        if (jusqua != null) { conditions.add("r.dateReception <= :jusqua");     params.put("jusqua", jusqua); }

        if (!conditions.isEmpty()) {
            jpql.append(" where ").append(String.join(" and ", conditions));
        }
        jpql.append(" order by r.dateReception desc, r.id desc"); // la plus récente d'abord

        return Reception.<Reception>find(jpql.toString(), params).list()
                .stream().map(ReceptionDto::de).toList();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public ReceptionDto detail(@PathParam("id") long id) {
        return ReceptionDto.de(trouver(id));
    }

    // ─────────────────────────────── EN-TÊTE ───────────────────────────────

    /** Crée une réception en BROUILLON. Les lignes sont facultatives à ce stade. */
    @POST
    @Transactional
    public Response creer(ReceptionCreationDto dto) {
        Reception r = new Reception();
        r.numero = Reception.prochainNumero();
        r.fournisseur = fournisseur(dto.fournisseur());
        r.transporteur = dto.transporteur() != null ? tiers(dto.transporteur()) : null;
        r.numeroBlFournisseur = dto.numeroBlFournisseur();
        r.dateReception = dto.dateReception() != null ? dto.dateReception() : LocalDateTime.now();
        r.emplacementArrivee = emplacementDeRangement(dto.emplacementArrivee());
        r.poidsTotalPese = poidsValide(dto.poidsTotalPese(), "poidsTotalPese");
        r.etat = EtatReception.BROUILLON;

        if (dto.lignes() != null) {
            dto.lignes().forEach(l -> r.lignes.add(construireLigne(r, l)));
        }
        r.persist(); // cascade ALL : les lignes suivent
        return Response.status(Response.Status.CREATED).entity(ReceptionDto.de(r)).build();
    }

    /**
     * Modifie l'EN-TÊTE seulement (transporteur, n° BL, date, pesée globale, quai).
     * PATCH et non PUT : on ne renvoie que ce qui change — typiquement la pesée, saisie
     * après coup quand le camion repasse sur le pont-bascule.
     */
    @PATCH
    @Path("/{id}")
    @Transactional
    public ReceptionDto modifierEntete(@PathParam("id") long id, ReceptionCreationDto dto) {
        Reception r = exigerBrouillon(trouver(id), "modifier");

        if (dto.transporteur() != null)        r.transporteur = tiers(dto.transporteur());
        if (dto.numeroBlFournisseur() != null) r.numeroBlFournisseur = dto.numeroBlFournisseur();
        if (dto.dateReception() != null)       r.dateReception = dto.dateReception();
        if (dto.emplacementArrivee() != null)  r.emplacementArrivee = emplacementDeRangement(dto.emplacementArrivee());
        if (dto.poidsTotalPese() != null)      r.poidsTotalPese = poidsValide(dto.poidsTotalPese(), "poidsTotalPese");
        // fournisseur volontairement NON modifiable : changer qui a livré, c'est une autre
        // réception, pas une correction.
        return ReceptionDto.de(r);
    }

    /** Supprime une réception — impossible dès qu'elle est validée (des mouvements existent). */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response supprimer(@PathParam("id") long id) {
        Reception r = exigerBrouillon(trouver(id), "supprimer");
        r.delete(); // cascade + orphanRemoval : les lignes partent avec
        return Response.noContent().build();
    }

    // ─────────────────────────────── LIGNES ───────────────────────────────

    @POST
    @Path("/{id}/lignes")
    @Transactional
    public Response ajouterLigne(@PathParam("id") long id, ReceptionCreationDto.Ligne dto) {
        Reception r = exigerBrouillon(trouver(id), "ajouter une ligne à");
        LigneReception ligne = construireLigne(r, dto);
        r.lignes.add(ligne);
        ligne.persist();
        return Response.status(Response.Status.CREATED).entity(ReceptionDto.de(r)).build();
    }

    @PUT
    @Path("/{id}/lignes/{ligneId}")
    @Transactional
    public ReceptionDto modifierLigne(@PathParam("id") long id,
                                      @PathParam("ligneId") long ligneId,
                                      ReceptionCreationDto.Ligne dto) {
        Reception r = exigerBrouillon(trouver(id), "modifier une ligne de");
        LigneReception ligne = ligneDe(r, ligneId);

        ligne.article = article(dto.article());
        ligne.quantite = quantiteValide(dto.quantite());
        ligne.numeroLot = dto.numeroLot();
        ligne.poidsPese = poidsValide(dto.poidsPese(), "poidsPese");
        ligne.emplacementDestination = dto.emplacementDestination() != null
                ? emplacementDeRangement(dto.emplacementDestination()) : null;
        return ReceptionDto.de(r);
    }

    @DELETE
    @Path("/{id}/lignes/{ligneId}")
    @Transactional
    public ReceptionDto supprimerLigne(@PathParam("id") long id, @PathParam("ligneId") long ligneId) {
        Reception r = exigerBrouillon(trouver(id), "retirer une ligne de");
        // orphanRemoval : sortir la ligne de la liste suffit à la supprimer en base.
        r.lignes.remove(ligneDe(r, ligneId));
        return ReceptionDto.de(r);
    }

    // ─────────────────────────── LE GESTE CENTRAL ───────────────────────────

    /**
     * ⭐ VALIDER : chaque ligne devient un Mouvement FOURNISSEUR → destination, et le stock
     * apparaît. Tout se joue dans UNE transaction : si une seule ligne est invalide, RIEN
     * n'est écrit — pas de réception à moitié réceptionnée.
     *
     * @return les mouvements générés, pour que le client voie immédiatement ce qui est entré
     */
    @POST
    @Path("/{id}/valider")
    @Transactional
    public List<MouvementDto> validerReception(@PathParam("id") long id) {
        Reception r = trouver(id);
        try {
            return r.valider().stream().map(MouvementDto::de).toList();
        } catch (IllegalStateException e) {
            // 409 CONFLICT plutôt que 422 : le corps de la requête est vide et correct —
            // c'est l'ÉTAT de la réception qui interdit l'opération (déjà validée, aucune
            // ligne, lot manquant sur un article suivi...). Le message dit lequel.
            throw new WebApplicationException(e.getMessage(), 409);
        }
    }

    /** Abandonne une réception non validée (camion reparti, erreur). Aucun mouvement produit. */
    @POST
    @Path("/{id}/annuler")
    @Transactional
    public ReceptionDto annuler(@PathParam("id") long id) {
        Reception r = exigerBrouillon(trouver(id), "annuler");
        r.etat = EtatReception.ANNULEE;
        return ReceptionDto.de(r);
    }

    // ─────────────────────────────── OUTILLAGE ───────────────────────────────

    private Reception trouver(long id) {
        Reception r = Reception.findById(id);
        if (r == null) {
            throw new WebApplicationException("Réception " + id + " introuvable", 404);
        }
        return r;
    }

    /** Toute modification exige l'état BROUILLON — sinon 409 avec un message explicite. */
    private Reception exigerBrouillon(Reception r, String action) {
        if (r.etat != EtatReception.BROUILLON) {
            throw new WebApplicationException("Impossible de " + action + " la réception "
                    + r.numero + " : elle est " + r.etat
                    + ". Une erreur après validation se corrige en inversant les mouvements.", 409);
        }
        return r;
    }

    private LigneReception ligneDe(Reception r, long ligneId) {
        return r.lignes.stream()
                .filter(l -> l.id != null && l.id == ligneId)
                .findFirst()
                .orElseThrow(() -> new WebApplicationException(
                        "Ligne " + ligneId + " introuvable dans la réception " + r.numero, 404));
    }

    private LigneReception construireLigne(Reception r, ReceptionCreationDto.Ligne dto) {
        LigneReception l = new LigneReception();
        l.reception = r;
        l.article = article(dto.article());
        l.quantite = quantiteValide(dto.quantite());
        l.numeroLot = dto.numeroLot();
        l.poidsPese = poidsValide(dto.poidsPese(), "poidsPese");
        l.emplacementDestination = dto.emplacementDestination() != null
                ? emplacementDeRangement(dto.emplacementDestination()) : null;
        return l;
    }

    // --- résolution des références (422 si la donnée fournie ne correspond à rien) ---

    private Tiers fournisseur(String code) {
        Tiers t = tiers(code);
        if (!t.estFournisseur) {
            throw new WebApplicationException(
                    "Le tiers '" + code + "' n'a pas le rôle fournisseur", 422);
        }
        return t;
    }

    private Tiers tiers(String code) {
        if (code == null || code.isBlank()) {
            throw new WebApplicationException("Le code du tiers est obligatoire", 422);
        }
        Tiers t = Tiers.findByCode(code);
        if (t == null) {
            throw new WebApplicationException("Tiers inconnu : '" + code + "'", 422);
        }
        return t;
    }

    private Article article(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new WebApplicationException("L'article est obligatoire", 422);
        }
        Article a = Article.findByReference(reference);
        if (a == null) {
            throw new WebApplicationException("Article inconnu : '" + reference + "'", 422);
        }
        return a;
    }

    /** Un emplacement où l'on peut RANGER : donc jamais un virtuel (FOURNISSEUR, PERTE…). */
    private Emplacement emplacementDeRangement(String code) {
        if (code == null || code.isBlank()) {
            throw new WebApplicationException("L'emplacement est obligatoire", 422);
        }
        Emplacement e = Emplacement.findByCode(code);
        if (e == null) {
            throw new WebApplicationException("Emplacement inconnu : '" + code + "'", 422);
        }
        if (e.type.estVirtuel()) {
            throw new WebApplicationException(
                    "'" + code + "' est un emplacement VIRTUEL : on n'y range pas de marchandise", 422);
        }
        return e;
    }

    private BigDecimal quantiteValide(BigDecimal quantite) {
        if (quantite == null || quantite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WebApplicationException("La quantité doit être strictement positive", 422);
        }
        return quantite;
    }

    private BigDecimal poidsValide(BigDecimal poids, String champ) {
        if (poids != null && poids.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WebApplicationException(champ + " doit être strictement positif", 422);
        }
        return poids;
    }
}
