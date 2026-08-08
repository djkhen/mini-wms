package com.fluxo.flux.mouvement;

import com.fluxo.flux.domain.Emplacement;
import com.fluxo.flux.domain.EtatMouvement;
import com.fluxo.flux.domain.Mouvement;
import com.fluxo.referentiel.domain.Article;
import com.fluxo.referentiel.domain.ModeTracabilite;

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
 * API REST du JOURNAL des mouvements — le cœur du flux.
 *
 *  GET   /mouvements                 liste (filtres ?article= &emplacement= &etat= &depuis= &jusqua=)
 *  GET   /mouvements/{id}            détail
 *  POST  /mouvements                 création — toujours en BROUILLON (201)
 *  POST  /mouvements/{id}/valider    le mouvement devient un FAIT et compte dans le stock
 *  POST  /mouvements/{id}/inverser   corrige une erreur en écrivant son miroir (201)
 *
 * ⛔ VOLONTAIREMENT ABSENTS : PUT, PATCH et DELETE.
 * Un tiers se corrige (il déménage, il change de nom) ; un mouvement VALIDÉ, non : c'est un
 * FAIT PASSÉ. On ne réécrit pas l'histoire, on ajoute une ligne qui la compense (/inverser).
 * L'immutabilité (règle 6) doit se lire dans le CONTRAT, pas seulement dans un commentaire :
 * un `DELETE /mouvements/12` répondra 405 Method Not Allowed, et c'est le message voulu.
 *
 * 🔀 Pourquoi la création est TOUJOURS en BROUILLON : une seule porte d'entrée vers l'état
 * VALIDE (`/valider`), donc un seul endroit à sécuriser et à faire évoluer. Les futurs
 * services (Réception, OF) enchaîneront les deux en interne, sans repasser par HTTP.
 */
@Path("/mouvements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MouvementResource {

    // ─────────────────────────────── LECTURE ───────────────────────────────

    /**
     * Liste filtrable, la plus récente d'abord.
     * `?emplacement=` cherche des DEUX côtés (source OU destination) : c'est ce qu'on veut
     * quand on demande « tout ce qui a bougé sur A-01-03 », entrées comme sorties.
     */
    @GET
    @Transactional
    public List<MouvementDto> liste(@QueryParam("article") String article,
                                    @QueryParam("emplacement") String emplacement,
                                    @QueryParam("etat") EtatMouvement etat,
                                    @QueryParam("depuis") LocalDateTime depuis,
                                    @QueryParam("jusqua") LocalDateTime jusqua) {
        StringBuilder jpql = new StringBuilder("from Mouvement m");
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (article != null) {
            conditions.add("m.article.reference = :article");
            params.put("article", article);
        }
        if (emplacement != null) {
            conditions.add("(m.source.code = :emplacement or m.destination.code = :emplacement)");
            params.put("emplacement", emplacement);
        }
        if (etat != null)   { conditions.add("m.etat = :etat");            params.put("etat", etat); }
        if (depuis != null) { conditions.add("m.dateEffet >= :depuis");    params.put("depuis", depuis); }
        if (jusqua != null) { conditions.add("m.dateEffet <= :jusqua");    params.put("jusqua", jusqua); }

        if (!conditions.isEmpty()) {
            jpql.append(" where ").append(String.join(" and ", conditions));
        }
        // Un journal se lit du plus récent au plus ancien. Pas de ?tri= ici : l'ordre
        // chronologique EST le sens de l'objet (contrairement à un référentiel).
        jpql.append(" order by m.dateEffet desc, m.id desc");

        return Mouvement.<Mouvement>find(jpql.toString(), params).list()
                .stream().map(MouvementDto::de).toList();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public MouvementDto detail(@PathParam("id") long id) {
        return MouvementDto.de(trouver(id));
    }

    // ─────────────────────────────── ÉCRITURE ───────────────────────────────

    /** Crée un mouvement en BROUILLON (il ne compte pas encore dans le stock). */
    @POST
    @Transactional
    public Response creer(MouvementCreationDto dto) {
        Mouvement m = new Mouvement();

        m.article = article(dto.article());
        m.source = emplacement(dto.source(), "source");
        m.destination = emplacement(dto.destination(), "destination");
        m.quantite = dto.quantite();
        m.numeroLot = dto.numeroLot();
        m.origineType = dto.origineType();
        m.origineId = dto.origineId();
        // Date d'effet facultative : par défaut, le mouvement a lieu maintenant.
        m.dateEffet = dto.dateEffet() != null ? dto.dateEffet() : LocalDateTime.now();
        // `unite` est COPIÉE de l'article (snapshot) : tous les mouvements d'un article sont
        // ainsi comptés dans la même unité, et la dérivation reste une addition bête.
        m.unite = m.article.unite;
        m.etat = EtatMouvement.BROUILLON;

        valider(m);
        m.persist();
        return Response.status(Response.Status.CREATED).entity(MouvementDto.de(m)).build();
    }

    /** BROUILLON → VALIDE : le mouvement devient un fait et compte dans le stock. */
    @POST
    @Path("/{id}/valider")
    @Transactional
    public MouvementDto validerMouvement(@PathParam("id") long id) {
        Mouvement m = trouver(id);
        try {
            m.valider();
        } catch (IllegalStateException e) {
            // Déjà validé ou annulé : ce n'est pas une faute de syntaxe (400) mais un
            // CONFLIT avec l'état courant de la ressource → 409.
            throw new WebApplicationException(e.getMessage(), 409);
        }
        return MouvementDto.de(m);
    }

    /**
     * Corrige un mouvement validé en écrivant son MIROIR (source et destination inversées).
     * Les deux lignes subsistent : le journal montre l'erreur ET sa correction.
     */
    @POST
    @Path("/{id}/inverser")
    @Transactional
    public Response inverser(@PathParam("id") long id) {
        Mouvement origine = trouver(id);
        Mouvement correction;
        try {
            correction = origine.inverse();
        } catch (IllegalStateException e) {
            throw new WebApplicationException(e.getMessage(), 409);
        }
        correction.persist();
        return Response.status(Response.Status.CREATED).entity(MouvementDto.de(correction)).build();
    }

    // ─────────────────────────────── OUTILLAGE ───────────────────────────────

    private Mouvement trouver(long id) {
        Mouvement m = Mouvement.findById(id);
        if (m == null) {
            throw new WebApplicationException("Mouvement " + id + " introuvable", 404);
        }
        return m;
    }

    /** Résout une RÉFÉRENCE d'article en entité — 422 si elle ne correspond à rien. */
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

    /** Résout un CODE d'emplacement en entité — 422 si inconnu. */
    private Emplacement emplacement(String code, String role) {
        if (code == null || code.isBlank()) {
            throw new WebApplicationException("L'emplacement " + role + " est obligatoire", 422);
        }
        Emplacement e = Emplacement.findByCode(code);
        if (e == null) {
            throw new WebApplicationException("Emplacement " + role + " inconnu : '" + code + "'", 422);
        }
        return e;
    }

    /**
     * Règles métier vérifiées AVANT l'insert (422 = donnée refusée).
     *
     * Les deux premières sont AUSSI des contraintes CHECK en base : le doublon est
     * volontaire. La base garantit qu'aucune donnée fausse n'entre, jamais ; le code
     * garantit un message clair. Une erreur 422 explicite vaut mieux qu'une erreur SQL brute.
     */
    private void valider(Mouvement m) {
        if (m.quantite == null || m.quantite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WebApplicationException(
                    "La quantité doit être strictement positive (le sens vient des emplacements)", 422);
        }
        if (m.source.id.equals(m.destination.id)) {
            throw new WebApplicationException(
                    "La source et la destination doivent être différentes", 422);
        }
        // Traçabilité : si l'article est suivi, le n° de lot n'est pas optionnel — sinon
        // la chaîne se rompt et le rappel ciblé devient impossible.
        if (m.article.tracabilite != ModeTracabilite.AUCUN
                && (m.numeroLot == null || m.numeroLot.isBlank())) {
            throw new WebApplicationException(
                    "L'article '" + m.article.reference + "' est suivi en "
                            + m.article.tracabilite + " : le numéro de lot est obligatoire", 422);
        }
    }
}
