package com.fluxo.flux.mouvement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ce que l'API ACCEPTE pour créer un mouvement — le contrat d'ENTRÉE.
 *
 * ⭐ Pourquoi un DTO séparé de {@link MouvementDto} (contrairement à Tiers/Article, qui
 * n'en ont qu'un) ? Parce qu'ici l'entrée et la sortie DIVERGENT :
 *
 *   ce que le client envoie  : article, source, destination, quantité, lot, date d'effet
 *   ce que le SERVEUR pose   : id · unite (copiée de l'article) · etat (BROUILLON) · creeLe
 *
 * Les exposer en entrée serait un contrat qui ment : le client les remplirait, on les
 * ignorerait en silence. Ils sont donc absents d'ici — et c'est visible dans le Swagger.
 *
 * 🔤 Les relations arrivent APLATIES : on reçoit `"PLANCHE-22"` et `"QUAI-01"`, pas des
 * objets Article/Emplacement imbriqués. La Resource les résout (et refuse si introuvables).
 */
public record MouvementCreationDto(

        /** Référence métier de l'article (ex. "PLANCHE-22"). Obligatoire. */
        String article,

        /** N° de lot — OBLIGATOIRE si l'article est suivi en LOT ou SERIE, sinon ignoré. */
        String numeroLot,

        /** Quantité, toujours > 0 : le sens vient des emplacements, jamais d'un signe. */
        BigDecimal quantite,

        /** Code de l'emplacement d'où ça part (ex. "FOURNISSEUR"). Obligatoire. */
        String source,

        /** Code de l'emplacement où ça va (ex. "QUAI-01"). Obligatoire, ≠ source. */
        String destination,

        /**
         * Quand le mouvement a RÉELLEMENT eu lieu. Facultatif → maintenant par défaut.
         * Sert à régulariser le lundi une livraison du vendredi (cf. les 2 dates, règle 4).
         */
        LocalDateTime dateEffet,

        /** D'où vient ce mouvement (ex. "RECEPTION"). Facultatif : un mouvement peut naître de rien. */
        String origineType,

        Long origineId) {
}
