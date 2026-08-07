package com.fluxo.flux.domain;

/**
 * État d'un Mouvement — c'est lui qui décide si le mouvement COMPTE dans le stock.
 *
 * 🔤 Nommage FRANÇAIS, cohérent avec les autres enums du projet (TypeEmplacement,
 * ModeTracabilite). Correspondance avec le schéma source (Odoo-like) :
 * BROUILLON = draft · VALIDE = done · ANNULE = cancelled.
 */
public enum EtatMouvement {

    /**
     * Prévu / réservé, PAS encore fait. N'impacte pas le stock physique, mais
     * ampute le DISPONIBLE (dispo = physique − brouillons sortants).
     * C'est ainsi qu'on alloue une palette à une commande sans inventer de table
     * « réservation » : l'intention est déjà un mouvement, simplement non validé.
     */
    BROUILLON,

    /**
     * Le mouvement a EU LIEU. Il compte dans le stock physique.
     * ⚠️ IMMUABLE : on ne modifie ni ne supprime jamais un mouvement validé —
     * une erreur se corrige par un MOUVEMENT INVERSE, pour que le journal montre
     * à la fois la faute et sa correction (historique auditable).
     */
    VALIDE,

    /**
     * Abandonné avant validation (commande annulée, réservation levée).
     * Ne compte nulle part, mais on garde la ligne : la trace vaut mieux que l'oubli.
     */
    ANNULE
}
