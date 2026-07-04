// ============================================================
// MODÈLE — TRAÇABILITÉ (lot & n° de série)
// Quarkus + Hibernate ORM Panache — fichier de CONCEPTION (réf, 1 fichier/entité en vrai)
// Entités référencées définies ailleurs : Article, Fournisseur, Emplacement, OrdreFabrication
//   → modules Flux/WMS & GPAO
// ============================================================

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ------------------------------------------------------------
// 0. MODE DE SUIVI — à ajouter sur l'entité Article :
//      @Enumerated(EnumType.STRING)
//      public ModeTracabilite tracabilite = ModeTracabilite.AUCUN;
//    AUCUN → bois banal / consommables
//    LOT   → suivi par lot (bois par coulée/livraison, matières)
//    SERIE → suivi individuel (pièces critiques, marchandise aéro client)
// enum ModeTracabilite { AUCUN, LOT, SERIE }
// ------------------------------------------------------------

// ------------------------------------------------------------
// 1. LOT — un groupe reçu OU produit ensemble (n° lot = clé de voûte)
// ------------------------------------------------------------
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "numeroLot"}))
public class Lot extends PanacheEntity {
    @ManyToOne(optional = false) public Article article;
    @Column(nullable = false) public String numeroLot;   // "L-2026-0345"  (unique PAR article)
    public LocalDate dateReception;      // ou date de fabrication
    public LocalDate datePeremption;     // optionnel (agro / pharma)
    @ManyToOne public Fournisseur fournisseur;   // origine si reçu (null si produit)
    @Enumerated(EnumType.STRING) public OrigineLot origine = OrigineLot.RECEPTION;
    public enum OrigineLot { RECEPTION, FABRICATION }
}

// ------------------------------------------------------------
// 2. UNITÉ SÉRIALISÉE — un objet unique (mode SERIE)
// ------------------------------------------------------------
@Entity
public class UniteSerie extends PanacheEntity {
    @ManyToOne(optional = false) public Article article;
    @Column(nullable = false, unique = true) public String numeroSerie; // "SN-00427"
    @ManyToOne public Lot lot;                 // lot d'origine (optionnel)
    @ManyToOne public Emplacement emplacement; // où il est MAINTENANT
    @Enumerated(EnumType.STRING) public StatutUnite statut = StatutUnite.EN_STOCK;
    public enum StatutUnite { EN_STOCK, RESERVE, EXPEDIE, REBUTE }
}

// ------------------------------------------------------------
// 3. STOCK — quantité par (article, emplacement, LOT)
//    (enrichit le Stock du WMS : on ajoute la dimension lot)
// ------------------------------------------------------------
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "emplacement_id", "lot_id"}))
public class Stock extends PanacheEntity {
    @ManyToOne(optional = false) public Article article;
    @ManyToOne(optional = false) public Emplacement emplacement;
    @ManyToOne public Lot lot;           // null si article non-loti
    public double quantite;
}

// ------------------------------------------------------------
// 4. MOUVEMENT — journal universel = CŒUR de la traçabilité
//    Chaque mouvement ESTAMPILLE le lot → toute l'histoire se reconstitue ici.
// ------------------------------------------------------------
@Entity
public class Mouvement extends PanacheEntity {
    @ManyToOne(optional = false) public Article article;
    @ManyToOne public Lot lot;              // le lot concerné (si loti)
    @ManyToOne public UniteSerie unite;     // ou l'unité série
    @ManyToOne public Emplacement source;       // null = entrée (réception)
    @ManyToOne public Emplacement destination;  // null = sortie (expédition)
    public double quantite;
    @Enumerated(EnumType.STRING) public TypeMouvement type;
    public LocalDateTime date;
    public String reference;   // n° réception / n° OF / n° colis → relie au contexte
    public enum TypeMouvement {
        RECEPTION, RANGEMENT, TRANSFERT, CONSO_OF, PRODUCTION, EXPEDITION, AJUSTEMENT, REBUT
    }
}

// ------------------------------------------------------------
// 5. LIEN DE GÉNÉALOGIE — parent/enfant (permet le RAPPEL ciblé)
//    lotProduit ← lotConsomme, via un OF
// ------------------------------------------------------------
@Entity
public class LienGenealogie extends PanacheEntity {
    @ManyToOne(optional = false) public Lot lotProduit;   // ce qu'on a fabriqué
    @ManyToOne(optional = false) public Lot lotConsomme;  // une matière consommée
    @ManyToOne public OrdreFabrication of;
    public double quantiteConsommee;
}

// ------------------------------------------------------------
// REQUÊTES CLÉS
//   ⬆️ Ascendante — « de quoi est fait ce produit ? »
//        LienGenealogie.list("lotProduit", x);
//   ⬇️ Descendante — LE RAPPEL — « qui est touché par le lot pourri ? »
//        var impactes = LienGenealogie.list("lotConsomme", lotPourri);   // → lots produits
//        // puis, pour ces lots :
//        Mouvement.list("lot in ?1 and type = ?2", lotsProduits, TypeMouvement.EXPEDITION);
//        // → destinations / colis / clients concernés (et EUX SEULEMENT)
// ------------------------------------------------------------
