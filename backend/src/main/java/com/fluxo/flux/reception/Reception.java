package com.fluxo.flux.reception;

import com.fluxo.flux.domain.Emplacement;
import com.fluxo.flux.domain.EtatMouvement;
import com.fluxo.flux.domain.Mouvement;
import com.fluxo.referentiel.domain.ModeTracabilite;
import com.fluxo.referentiel.domain.Tiers;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Réception — le premier VRAI flux : « un camion est arrivé ».
 *
 * C'est le DOCUMENT qui manquait : sans lui, le réceptionnaire devrait saisir un mouvement
 * par article, sans savoir qu'ils viennent du même camion, sans n° de BL, et avec le risque
 * qu'un appel échoue au milieu. Ici : UN document, N lignes, et une validation tout-ou-rien.
 *
 * ⭐ CE QU'ELLE FAIT, EXACTEMENT : elle ne « met pas à jour le stock » — ce serait le
 * compteur, l'anti-pattern. Elle GÉNÈRE des Mouvements `FOURNISSEUR → Quai` ; le stock
 * apparaît ensuite par dérivation (Σ des mouvements). La réception ne connaît même pas
 * la notion de stock.
 *
 * `commande_achat` viendra plus tard, en FK NULLABLE : recevoir sans commande doit rester
 * possible (§6sexies), et le reliquat se DÉDUIT (commandé − reçu), il ne se stocke pas.
 */
@Entity
public class Reception extends PanacheEntity {

    /** Identifiant métier lisible, généré : REC-26-0001. */
    @Column(nullable = false, unique = true)
    public String numero;

    /**
     * QUI a livré — OBLIGATOIRE. Un litige, un retour, un rappel qualité : tout remonte
     * au fournisseur. Le rendre facultatif casserait la traçabilité amont.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    public Tiers fournisseur;

    /** QUI a transporté — facultatif (le fournisseur livre parfois lui-même). */
    @ManyToOne
    @JoinColumn(name = "transporteur_id")
    public Tiers transporteur;

    /** Le n° du bon de livraison PAPIER du fournisseur (sa référence, pas la nôtre). */
    @Column(name = "numero_bl_fournisseur")
    public String numeroBlFournisseur;

    /** Quand la marchandise est arrivée (sert de date d'effet aux mouvements générés). */
    @Column(name = "date_reception", nullable = false)
    public LocalDateTime dateReception = LocalDateTime.now();

    /** Où tout arrive par défaut (le quai). Chaque ligne peut le surcharger. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "emplacement_arrivee_id", nullable = false)
    public Emplacement emplacementArrivee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EtatReception etat = EtatReception.BROUILLON;

    /**
     * ⭐ Pesée GLOBALE au pont-bascule (le camion). Indépendante des poids par ligne :
     * on ne déduit JAMAIS l'une de l'autre. L'écart entre les deux est justement
     * l'information recherchée (emballages, manquant, erreur de comptage) — le dériver
     * reviendrait à effacer ce que la pesée sert à détecter. Cf. {@link #ecartDePesee()}.
     */
    @Column(name = "poids_total_pese", precision = 19, scale = 4)
    public BigDecimal poidsTotalPese;

    /** Les articles reçus. `orphanRemoval` : retirer une ligne de la liste la supprime. */
    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<LigneReception> lignes = new ArrayList<>();

    // ═══════════════════════════ LE GESTE CENTRAL ═══════════════════════════

    /**
     * VALIDER : chaque ligne devient un Mouvement `FOURNISSEUR → destination`, et le stock
     * apparaît. Appelée dans UNE transaction → tout entre, ou rien (pas de réception
     * à moitié saisie).
     *
     * @return les mouvements générés (un par ligne)
     */
    public List<Mouvement> valider() {
        if (etat != EtatReception.BROUILLON) {
            throw new IllegalStateException(
                    "Seule une réception BROUILLON peut être validée (état actuel : " + etat + ").");
        }
        if (lignes.isEmpty()) {
            throw new IllegalStateException("Une réception sans ligne n'a rien reçu.");
        }
        lignes.forEach(Reception::controlerLigne);

        // L'emplacement virtuel d'où vient toute marchandise achetée (migration 006).
        Emplacement fournisseurVirtuel = Emplacement.findByCode("FOURNISSEUR");
        if (fournisseurVirtuel == null) {
            throw new IllegalStateException(
                    "L'emplacement virtuel FOURNISSEUR est introuvable (migration 006 non appliquée ?).");
        }

        List<Mouvement> mouvements = new ArrayList<>();
        for (LigneReception ligne : lignes) {
            Mouvement m = new Mouvement();
            m.article = ligne.article;
            m.numeroLot = ligne.numeroLot;
            m.quantite = ligne.quantite;
            m.unite = ligne.article.unite;              // snapshot de l'unité
            m.source = fournisseurVirtuel;              // ⭐ le sens vient de là
            m.destination = ligne.destinationEffective();
            m.etat = EtatMouvement.VALIDE;              // la marchandise EST là
            m.dateEffet = dateReception;                // le fait réel, pas l'heure de saisie
            m.origineType = "RECEPTION";                // règle 7 : on peut remonter ici
            m.origineId = this.id;
            m.persist();
            mouvements.add(m);
        }
        etat = EtatReception.VALIDEE;
        return mouvements;
    }

    /** Contrôles métier d'une ligne, avant de produire quoi que ce soit. */
    private static void controlerLigne(LigneReception l) {
        if (l.quantite == null || l.quantite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Quantité invalide pour l'article " + l.article.reference + " : elle doit être > 0.");
        }
        if (l.article.tracabilite != ModeTracabilite.AUCUN
                && (l.numeroLot == null || l.numeroLot.isBlank())) {
            throw new IllegalStateException("L'article " + l.article.reference + " est suivi en "
                    + l.article.tracabilite + " : le numéro de lot est obligatoire.");
        }
        if (l.destinationEffective().type.estVirtuel()) {
            throw new IllegalStateException(
                    "On ne range pas de la marchandise dans un emplacement VIRTUEL ("
                            + l.destinationEffective().code + ").");
        }
    }

    // ═══════════════════════════ PESÉE ═══════════════════════════

    /** Somme des poids saisis ligne par ligne (null ignorés). */
    public BigDecimal poidsCumuleDesLignes() {
        return lignes.stream()
                .map(l -> l.poidsPese)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Écart entre la pesée globale et le cumul des lignes — l'information de CONTRÔLE.
     * Positif : le camion pèse plus que le détail (emballages, palettes, ligne oubliée).
     * `null` si aucune pesée globale n'a été faite : pas de comparaison possible.
     */
    public BigDecimal ecartDePesee() {
        return poidsTotalPese == null ? null : poidsTotalPese.subtract(poidsCumuleDesLignes());
    }

    // ═══════════════════════════ NUMÉROTATION ═══════════════════════════

    /**
     * Génère le prochain numéro : REC-26-0001.
     *
     * Séquence PostgreSQL dédiée → pas de collision même si deux réceptions sont créées
     * en même temps (un `count(*)+1` en produirait). La séquence NE REPART PAS à 1 chaque
     * année : une numérotation continue et sans trou est plus solide pour des documents.
     */
    public static String prochainNumero() {
        Number n = (Number) getEntityManager()
                .createNativeQuery("select nextval('reception_numero_SEQ')")
                .getSingleResult();
        String annee = DateTimeFormatter.ofPattern("yy").format(LocalDate.now());
        return "REC-%s-%04d".formatted(annee, n.longValue());
    }

    public static Reception findByNumero(String numero) {
        return find("numero", numero).firstResult();
    }
}
