package com.fluxo.flux.reception;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ce que l'API ACCEPTE pour créer une réception — le contrat d'ENTRÉE.
 *
 * Les relations arrivent APLATIES (codes métier), jamais des objets imbriqués :
 * la Resource les résout et refuse en 422 si elles n'existent pas.
 *
 * Absents volontairement : `numero` (généré : REC-26-0001) et `etat` (toujours BROUILLON
 * au départ). Les exposer serait un contrat qui ment — le client les remplirait, on les
 * ignorerait.
 *
 * `lignes` est FACULTATIF : on peut créer une réception vide puis ajouter les lignes au
 * fur et à mesure du déballage (POST /receptions/{id}/lignes), ou tout envoyer d'un coup
 * si le front a gardé son brouillon en local. Les deux usages sont réels.
 */
public record ReceptionCreationDto(

        /** Code du tiers fournisseur — OBLIGATOIRE (la traçabilité amont en dépend). */
        String fournisseur,

        /** Code du tiers transporteur — facultatif (le fournisseur livre parfois lui-même). */
        String transporteur,

        /** N° du bon de livraison PAPIER du fournisseur. */
        String numeroBlFournisseur,

        /** Quand le camion est arrivé. Facultatif → maintenant par défaut. */
        LocalDateTime dateReception,

        /** Code de l'emplacement d'arrivée (le quai) — OBLIGATOIRE. */
        String emplacementArrivee,

        /** Pesée globale au pont-bascule — facultative, INDÉPENDANTE des poids par ligne. */
        BigDecimal poidsTotalPese,

        /** Les articles reçus. Facultatif à la création. */
        List<Ligne> lignes) {

    /** Une ligne à créer : un article, une quantité, éventuellement son lot et son poids. */
    public record Ligne(
            String article,                 // référence métier
            BigDecimal quantite,
            String numeroLot,               // obligatoire si l'article est suivi
            BigDecimal poidsPese,
            String emplacementDestination   // facultatif → celui de l'en-tête
    ) {}
}
