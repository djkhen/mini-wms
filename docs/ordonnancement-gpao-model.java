// ============================================================
// MODÈLE GPAO — ORDONNANCEMENT (version enrichie)
// Quarkus + Hibernate ORM Panache
// Réf de conception — en vrai : 1 fichier .java par entité (package entity)
// ✨ = ajout / correction par rapport à la version initiale (générée par IA, revue + complétée)
// NB : fichier de design pour la future plateforme (partie GPAO). À déplacer dans un
//      repo/module dédié quand le projet démarrera.
// ============================================================

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.*;
import java.util.List;
import java.util.Set;

// ------------------------------------------------------------
// 1. POSTE DE TRAVAIL — la ressource + sa capacité + son CALENDRIER
// ------------------------------------------------------------
@Entity
public class PosteDeTravail extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String code;                 // "SCIE-01"
    public String libelle;              // "Scie à ruban"

    /** ✨ Nb de ressources identiques EN PARALLÈLE (1 = une seule machine → série). */
    public int nbRessources = 1;

    /** ✨ CALENDRIER : horaires de travail = source de vérité pour poser la timeline. */
    public LocalTime heureDebut = LocalTime.of(8, 0);
    public LocalTime heureFin   = LocalTime.of(17, 0);

    /** ✨ Jours ouvrés du poste (ex : LUNDI..VENDREDI). */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    public Set<DayOfWeek> joursOuvres;

    /** Rendement moyen (1.0 = 100%). ✨ Enfin utilisé → voir OperationOF.dureeEffectiveMin(). */
    public double tauxRendement = 1.0;

    public boolean actif = true;

    /** ✨ Capacité indicative (h/jour) DÉRIVÉE des horaires — pour un calcul de charge rapide.
     *  La planif fine s'appuie sur heureDebut/heureFin + joursOuvres + Indisponibilite. */
    public double capaciteHeuresJour() {
        double heures = Duration.between(heureDebut, heureFin).toMinutes() / 60.0;
        return heures * nbRessources;
    }
}

// ------------------------------------------------------------
// ✨ 1bis. INDISPONIBILITÉ — trous dans le calendrier (maintenance, congés, férié).
//     L'algo doit SOUSTRAIRE ces plages de la capacité du poste.
// ------------------------------------------------------------
@Entity
public class Indisponibilite extends PanacheEntity {
    @ManyToOne(optional = false)
    public PosteDeTravail poste;
    public LocalDateTime debut;
    public LocalDateTime fin;
    public String motif;                // "Maintenance", "Congés", "Férié"...
}

// ------------------------------------------------------------
// 2. ARTICLE — le produit fabriqué + sa gamme (recette)
// ------------------------------------------------------------
@Entity
public class Article extends PanacheEntity {
    @Column(nullable = false, unique = true)
    public String code;
    public String designation;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroOperation ASC")
    public List<OperationGamme> gamme;
}

// ------------------------------------------------------------
// 3. OPÉRATION DE GAMME — l'étape "modèle"
// ------------------------------------------------------------
@Entity
public class OperationGamme extends PanacheEntity {
    @ManyToOne(optional = false)
    public Article article;
    public int numeroOperation;         // 10, 20, 30... (ordre d'exécution)
    public String libelle;              // "Découpe"
    @ManyToOne(optional = false)
    public PosteDeTravail poste;
    public double tempsReglageMin;      // réglage fixe
    public double tempsUnitaireMin;     // par pièce
}

// ------------------------------------------------------------
// 4. ORDRE DE FABRICATION
// ------------------------------------------------------------
@Entity
public class OrdreFabrication extends PanacheEntity {
    @Column(nullable = false, unique = true)
    public String numero;               // "OF-2026-0042"
    @ManyToOne(optional = false)
    public Article article;
    public int quantite;

    public LocalDate dateBesoin;        // due date → tri EDD

    /** ✨ Priorité manuelle optionnelle (sinon tri par dateBesoin). */
    public Integer priorite;

    /** ✨ HOOK MATIÈRE — pont avec le module Flux/WMS + IA prévisions (ta vision unifiée) :
     *  au plus tôt où la matière est dispo → contrainte de démarrage. null = déjà dispo. */
    public LocalDateTime dateDispoMatiere;

    @Enumerated(EnumType.STRING)
    public StatutOF statut = StatutOF.PLANIFIE;

    @OneToMany(mappedBy = "of", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroOperation ASC")
    public List<OperationOF> operations;

    public enum StatutOF { PLANIFIE, LANCE, EN_COURS, TERMINE, RETARD }
}

// ------------------------------------------------------------
// 5. OPÉRATION D'OF — l'étape réelle planifiée (remplie par l'algo, affichée au Gantt)
// ------------------------------------------------------------
@Entity
public class OperationOF extends PanacheEntity {
    @ManyToOne(optional = false)
    public OrdreFabrication of;

    public int numeroOperation;
    public String libelle;

    @ManyToOne(optional = false)
    public PosteDeTravail poste;

    /** Charge brute = réglage + (unitaire × quantité), en minutes. */
    public double chargeMin;

    /** ✨ Durée EFFECTIVE = charge / rendement du poste (tauxRendement enfin exploité). */
    public double dureeEffectiveMin() {
        double taux = (poste != null && poste.tauxRendement > 0) ? poste.tauxRendement : 1.0;
        return chargeMin / taux;
    }

    /** Rempli par l'algorithme d'ordonnancement : */
    public LocalDateTime debutPlanifie;
    public LocalDateTime finPlanifiee;

    /** Stocké pour le Gantt (perf). ✨ Dérivable : finPlanifiee > of.dateBesoin. */
    public boolean enRetard;

    /** ✨ Rattachement à un scénario de planning (versioning). null = plan courant/réel. */
    @ManyToOne
    public PlanOrdonnancement plan;

    /** ✨ Recalcule la charge depuis la gamme + la quantité (à appeler à la création de l'OF). */
    public void calculerCharge(OperationGamme g, int qte) {
        this.chargeMin = g.tempsReglageMin + g.tempsUnitaireMin * qte;
    }
}

// ------------------------------------------------------------
// ✨ 6. PLAN D'ORDONNANCEMENT — un scénario VERSIONNÉ (simuler / comparer)
//     Cohérent avec ta logique de "versions" (comme les devis).
// ------------------------------------------------------------
@Entity
public class PlanOrdonnancement extends PanacheEntity {
    public String libelle;              // "Plan semaine 28", "Simu +2 équipes"
    public LocalDateTime dateCreation;
    public int version;
    @Enumerated(EnumType.STRING)
    public StatutPlan statut = StatutPlan.BROUILLON;

    public enum StatutPlan { BROUILLON, SIMULE, VALIDE }
}
