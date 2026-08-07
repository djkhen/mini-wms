package com.fluxo.flux.domain;

import com.fluxo.referentiel.domain.Article;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mouvement — ⭐ LE CŒUR de Fluxo. Le journal de tout ce qui bouge.
 *
 * ══════════════════════════════════════════════════════════════════════════════
 *  LE PRINCIPE NON NÉGOCIABLE (§3) : le stock n'est JAMAIS stocké, il est DÉRIVÉ.
 *  Il n'existe aucune table `Stock`, aucun compteur `quantite` qu'on incrémente.
 *  Le stock d'un article à un endroit = la SOMME de ses mouvements (cf. stockPhysique).
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * Ce que ce modèle offre GRATUITEMENT, et qu'un compteur ne permet jamais :
 *  • le stock à n'importe quelle DATE PASSÉE (filtrer sur dateEffet) ;
 *  • la distinction physique (VALIDE) / prévisionnel (VALIDE + BROUILLON) ;
 *  • un historique auditable : on voit QUI a fait bouger QUOI, QUAND et POURQUOI.
 *
 * LES 7 RÈGLES D'OR (§4) :
 *  1. `quantite` TOUJOURS > 0 — le sens vient des emplacements, jamais d'un signe.
 *  2. La sémantique vient des EMPLACEMENTS (dont les virtuels), pas d'un type en dur :
 *        achat        FOURNISSEUR → Entrepôt      vente     Entrepôt → CLIENT
 *        conso prod   Entrepôt    → PRODUCTION    sortie    PRODUCTION → Entrepôt
 *        casse        Entrepôt    → PERTE         inventaire INVENTAIRE ↔ Entrepôt
 *  3. Pas de champ `type` source de vérité (une étiquette dérivée pour l'IHM, à la rigueur).
 *  4. DEUX dates : `dateEffet` (le fait réel) et `creeLe` (la saisie). Elles diffèrent
 *     dès qu'on régularise un lundi une livraison du vendredi.
 *  5. État BROUILLON | VALIDE | ANNULE — un brouillon n'impacte pas le physique.
 *  6. IMMUABLE une fois VALIDE : jamais d'édition/suppression → mouvement INVERSE.
 *  7. Lien à l'origine (`origineType`/`origineId`) pour remonter à la réception, l'OF…
 *
 * ⛔ Cette table est la SEULE où les champs personnalisés (`champs_custom`) sont
 *    INTERDITS (§6octies) : un journal auditable ne se négocie pas client par client.
 */
@Entity
public class Mouvement extends PanacheEntity {

    /** QUOI bouge. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    public Article article;

    /**
     * Numéro de lot — la colonne vertébrale de la traçabilité (obligatoire si
     * `article.tracabilite` vaut LOT ou SERIE ; le contrôle viendra dans le service).
     * Simple texte pour l'instant : l'entité `Lot` (avec généalogie amont/aval)
     * arrivera quand on traitera le rappel ciblé.
     */
    @Column(name = "numero_lot")
    public String numeroLot;

    /**
     * COMBIEN — ⚠️ TOUJOURS strictement positif (contrainte CHECK en base aussi).
     * BigDecimal et non double : une quantité entre dans des calculs et des totaux,
     * l'arithmétique binaire approximative y est proscrite (même règle que l'argent).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal quantite;

    /**
     * Unité au moment du mouvement, COPIÉE depuis l'article (snapshot §6septies).
     * Tous les mouvements d'un article étant comptés dans la même unité, la dérivation
     * `Σ mouvements` reste une addition bête — jamais une conversion à la volée.
     */
    @Column(nullable = false)
    public String unite;

    /** D'OÙ ça vient. Avec `destination`, c'est ce couple qui porte TOUT le sens. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    public Emplacement source;

    /** OÙ ça va. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    public Emplacement destination;

    /** Compte-t-il dans le stock ? (cf. {@link EtatMouvement}) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EtatMouvement etat = EtatMouvement.BROUILLON;

    /** QUAND le mouvement a réellement eu lieu (peut être antérieur à la saisie). */
    @Column(name = "date_effet", nullable = false)
    public LocalDateTime dateEffet;

    /** QUAND la ligne a été écrite en base. Jamais modifiée : c'est la trace d'audit. */
    @Column(name = "cree_le", nullable = false)
    public LocalDateTime creeLe = LocalDateTime.now();

    /**
     * POURQUOI : d'où ce mouvement provient-il (ex. "RECEPTION", "ORDRE_FABRICATION").
     * ⚠️ NULLABLE, et c'est capital : un mouvement peut naître d'une réception, d'un OF…
     * ou de RIEN (correction manuelle, inventaire). Rendre ce champ obligatoire
     * casserait la discipline « le flux tourne complet SANS aucun OF » (§6quinquies A).
     * Texte libre volontairement : les entités d'origine n'existent pas encore, figer
     * un enum maintenant serait une devinette.
     */
    @Column(name = "origine_type")
    public String origineType;

    @Column(name = "origine_id")
    public Long origineId;

    // ═══════════════════════════ LA DÉRIVATION ═══════════════════════════

    /**
     * ⭐ LE STOCK, CALCULÉ : Σ des quantités ENTRÉES − Σ des quantités SORTIES,
     * en ne comptant que les mouvements VALIDE.
     *
     * C'est ici que se matérialise « le stock n'est nulle part » : cette méthode
     * remplace à elle seule la table `Stock` du modèle abandonné.
     */
    public static BigDecimal stockPhysique(Article article, Emplacement emplacement) {
        return sommeEntrees(article, emplacement, EtatMouvement.VALIDE)
                .subtract(sommeSorties(article, emplacement, EtatMouvement.VALIDE));
    }

    /**
     * Le stock DISPONIBLE = physique − ce qui est déjà promis (brouillons sortants).
     * C'est la valeur à regarder avant d'accepter une commande : elle empêche de
     * promettre deux fois la même palette, sans aucune table « réservation ».
     */
    public static BigDecimal stockDisponible(Article article, Emplacement emplacement) {
        return stockPhysique(article, emplacement)
                .subtract(sommeSorties(article, emplacement, EtatMouvement.BROUILLON));
    }

    /** Tout ce qui est ENTRÉ à cet emplacement (il en est la destination). */
    private static BigDecimal sommeEntrees(Article article, Emplacement emplacement, EtatMouvement etat) {
        return somme("select coalesce(sum(m.quantite), 0) from Mouvement m "
                + "where m.article = :article and m.destination = :emplacement and m.etat = :etat",
                article, emplacement, etat);
    }

    /** Tout ce qui en est SORTI (il en est la source). */
    private static BigDecimal sommeSorties(Article article, Emplacement emplacement, EtatMouvement etat) {
        return somme("select coalesce(sum(m.quantite), 0) from Mouvement m "
                + "where m.article = :article and m.source = :emplacement and m.etat = :etat",
                article, emplacement, etat);
    }

    /** `coalesce(..., 0)` : une somme sans ligne vaut ZÉRO, jamais null. */
    private static BigDecimal somme(String jpql, Article article, Emplacement emplacement, EtatMouvement etat) {
        return getEntityManager().createQuery(jpql, BigDecimal.class)
                .setParameter("article", article)
                .setParameter("emplacement", emplacement)
                .setParameter("etat", etat)
                .getSingleResult();
    }

    // ═══════════════════════════ CYCLE DE VIE ═══════════════════════════

    /**
     * Fait passer un BROUILLON à VALIDE : le mouvement devient un fait, il compte
     * dans le stock. Opération à sens unique — après quoi la ligne est figée.
     */
    public void valider() {
        if (etat != EtatMouvement.BROUILLON) {
            throw new IllegalStateException(
                    "Seul un mouvement BROUILLON peut être validé (état actuel : " + etat + ").");
        }
        etat = EtatMouvement.VALIDE;
    }

    /**
     * Corriger une erreur ne se fait JAMAIS en modifiant la ligne fautive (règle 6) :
     * on écrit son MIROIR, source et destination inversées. Le journal conserve alors
     * l'erreur ET sa correction — c'est ce qui le rend auditable.
     */
    public Mouvement inverse() {
        if (etat != EtatMouvement.VALIDE) {
            throw new IllegalStateException("On n'inverse qu'un mouvement VALIDE.");
        }
        Mouvement m = new Mouvement();
        m.article = this.article;
        m.numeroLot = this.numeroLot;
        m.quantite = this.quantite;          // toujours > 0 : c'est le SENS qui s'inverse
        m.unite = this.unite;
        m.source = this.destination;         // ⭐ inversion
        m.destination = this.source;
        m.etat = EtatMouvement.VALIDE;
        m.dateEffet = LocalDateTime.now();
        m.origineType = "CORRECTION";
        m.origineId = this.id;               // on garde le lien vers le mouvement corrigé
        return m;
    }
}
