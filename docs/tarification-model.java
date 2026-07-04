// ============================================================
// MODÈLE — TARIFICATION (domaine com.fluxo.commercial)
// Quarkus + Panache — fichier de CONCEPTION (réf, 1 fichier/entité en vrai)
// Reprend la logique métier du legacy GPAO Uniface de l'auteur :
//   prix = TARIF (résolu selon une PORTÉE) × QUANTITÉ (selon une BASE choisie au chiffrage)
//   + prix exceptionnel « par lien » (client) prioritaire sur le tarif standard.
// Références externes : Client, Article (com.fluxo.referentiel), ModeleCaisse (com.fluxo.gpao)
// ============================================================

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;   // ⚠️ ARGENT = BigDecimal, jamais double (arrondis !)
import java.time.LocalDate;

// ------------------------------------------------------------
// 1. UO — UNITÉ D'ŒUVRE (terme du legacy Uniface de l'auteur)
//    = la mesure UNIVERSELLE de facturation, partagée par TOUT ce qui se facture :
//      caisses (GPAO), prestations, articles, colis.
//    (choisie par l'utilisateur AU MOMENT du chiffrage, pas figée par modèle)
//    prix = tarif (€/UO) × quantité mesurée dans cette UO
//    La quantité vient de L'OBJET : la caisse calcule son m²/volume depuis ses
//    dimensions ; l'article porte son poids/volume ; la prestation déclare sa qté d'UO.
//    La CAISSE se facture au poids/volume/unité POUR ELLE-MÊME, indépendamment de
//    l'article éventuel qu'elle contient : chaque élément du colis = SA propre ligne
//    facturable (caisse | article | prestation), chacune résout SON tarif.
//    ✅ CONFIRMÉ (utilisateur, 2026-07-04) : caisse « au poids » = LE POIDS DE LA CAISSE
//    elle-même (le bois) — la GPAO conçoit/vend la caisse, pas le contenu. Ce poids est
//    DÉRIVABLE de la fiche de débit : Σ(sections × longueurs × quantités) × densité du bois
//    → calculé automatiquement, pas de pesée. (Le poids NET saisi par l'opérateur sert au
//    DIMENSIONNEMENT via les formules, pas à la facturation.)
//    Reste à confirmer : volume = extérieur (encombrement) ou intérieur utile ?
// ------------------------------------------------------------
enum UniteOeuvre {
    LONGUEUR,    // au mètre linéaire (L)
    LARGEUR,     // (l)
    HAUTEUR,     // (h)
    M2_SOL,      // surface au sol
    SURFACE,     // surface (développée ?) — ⚠️ distinction M² vs Surface à confirmer avec le legacy
    POIDS,       // (P)
    VOLUME,      // (V)
    UNITE        // à la caisse / à l'unité / au colis
}

// ------------------------------------------------------------
// 2. RÈGLE DE TARIFICATION — une ligne de la grille
//    La PORTÉE = à qui/quoi s'applique le tarif. Plus elle est
//    spécifique, plus elle est prioritaire (voir résolution).
// ------------------------------------------------------------
@Entity
public class RegleTarification extends PanacheEntity {

    // ----- PORTÉE (tous nullable : on remplit ce qui s'applique) -----
    @ManyToOne public Client client;          // tarif négocié pour CE client (le « lien »)
    @ManyToOne public ModeleCaisse modele;    // tarif pour un type de caisse (T16…)
    @ManyToOne public Article article;        // tarif pour un article (négoce / prestation)
    public String codeCaisse;                 // ou un code caisse précis

    // ----- CE QU'ON FACTURE -----
    @Enumerated(EnumType.STRING)
    public UniteOeuvre uo;                    // l'unité d'œuvre de CE tarif (€/UO)

    @Column(precision = 12, scale = 4)
    public BigDecimal prixUnitaire;           // prix par unité de base (€/m², €/kg, €/caisse…)

    /** Prix EXCEPTIONNEL (négocié) : prioritaire sur les tarifs standards.
     *  NB (legacy confirmé 2026-07-04) : le préférentiel se définit par couple
     *  (client × UO) — un client peut avoir SON prix au poids ET son prix à la
     *  surface, etc. Chaque combinaison = une ligne de la grille. */
    public boolean exceptionnel = false;

    // ----- MODE DE CALCUL (décidé 2026-07-04) -----
    /** Comment cette règle produit le prix unitaire :
     *  PRIX_FIXE  → prixUnitaire ci-dessus (catalogue ou négocié)
     *  COUT_PLUS  → coût de revient (dérivé du DÉBIT : matière × densité × prix d'achat,
     *               + main d'œuvre plus tard) × coefficientSurCout. Marge garantie même
     *               si le bois flambe. */
    @Enumerated(EnumType.STRING)
    public ModeCalcul modeCalcul = ModeCalcul.PRIX_FIXE;
    @Column(precision = 8, scale = 4)
    public BigDecimal coefficientSurCout;     // ex : 1.45 (si COUT_PLUS)
    public enum ModeCalcul { PRIX_FIXE, COUT_PLUS }

    /** DÉGRESSIF : paliers de quantité optionnels sur CETTE règle. */
    @OneToMany(mappedBy = "regle", cascade = CascadeType.ALL, orphanRemoval = true)
    public java.util.List<PalierQuantite> paliers;

    // ----- VALIDITÉ -----
    public LocalDate dateDebut;
    public LocalDate dateFin;                 // null = sans fin
    public boolean actif = true;
}

// ------------------------------------------------------------
// 2bis. PALIER DE QUANTITÉ — le dégressif d'une règle
//    ex : 0-100 kg → 1,00 €/kg ; 100-500 → 0,80 ; 500+ → 0,65
// ------------------------------------------------------------
@Entity
public class PalierQuantite extends PanacheEntity {
    @ManyToOne(optional = false) public RegleTarification regle;
    @Column(precision = 12, scale = 3) public BigDecimal quantiteMin;   // borne incluse
    @Column(precision = 12, scale = 3) public BigDecimal quantiteMax;   // null = sans limite
    @Column(precision = 12, scale = 4) public BigDecimal prixUnitaire;  // €/UO sur ce palier
}

// ------------------------------------------------------------
// 2ter. PIPELINE DE PRIX (décidé 2026-07-04) — 3 ÉTAGES, pas de conflit :
//   ÉTAPE 1 (source du prix unitaire, une seule gagne) :
//       exceptionnel client (« lien ») > COUT_PLUS > catalogue
//   ÉTAPE 2 (ajustement quantité) : paliers dégressifs de la règle retenue
//   ÉTAPE 3 (niveau DOCUMENT) : REMISE CLIENT sur le devis/dossier
//       (existait déjà dans le legacy Uniface — on la garde au même endroit)
//   ⚠️ Anti double-réduction : prix exceptionnel = NET → la remise devis ne
//   s'applique pas dessus (ou alerte). Comportement exact du legacy à confirmer.
// ------------------------------------------------------------

// ------------------------------------------------------------
// 3. RÉSOLUTION DU PRIX — l'ordre de priorité (service, pas entité)
//
//    TarificationService.resoudre(client, modele/article, base, date) :
//      1. règle EXCEPTIONNELLE client + article/modèle  (le « lien » du legacy)
//      2. règle client + article/modèle
//      3. règle client seul
//      4. règle article/modèle seul (tarif catalogue)
//      5. sinon → pas de prix → l'utilisateur saisit manuellement
//    ⚠️ Ordre à CONFIRMER avec le comportement réel du legacy Uniface.
//
//    Le service est DANS com.fluxo.commercial ; la GPAO (négoce) et les devis
//    l'appellent via son interface — ils ne lisent jamais la grille en direct.
// ------------------------------------------------------------

// ------------------------------------------------------------
// 4. OÙ EST LE « PRIX DE L'ARTICLE » ? — réponse à la question du 2026-07-04
//    - Le prix CATALOGUE de base d'un article = une RegleTarification
//      (article=X, client=null, base=UNITE) → tout est dans UNE table, pas
//      de champ prix éparpillé. (Variante possible : prixBase sur Article
//      comme simple valeur par défaut — à trancher au moment de coder.)
//    - Le prix FIGÉ (devis, ligne de commande, prestation) = copié dans la
//      ligne au moment du chiffrage (comme dans gs : LigneCommande.prixUnitaire).
//      → la grille peut évoluer sans changer l'historique. (= snapshot)
// ------------------------------------------------------------
