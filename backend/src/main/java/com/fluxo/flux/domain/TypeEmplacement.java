package com.fluxo.flux.domain;

/**
 * Nature fonctionnelle d'un emplacement dans l'entrepôt.
 *
 * Un emplacement n'est pas qu'une case de stockage : selon sa zone, il joue un
 * rôle dans le flux (réception à l'entrée, expédition en sortie, tri/quai…).
 * On modélise ce rôle par un enum plutôt qu'un texte libre (cf. legacy : statuts
 * en varchar non contraints) → seules ces valeurs sont possibles.
 *
 * ⭐ DEUX FAMILLES (§6ter) :
 *  - les emplacements PHYSIQUES, qui existent dans l'entrepôt ;
 *  - les emplacements VIRTUELS, qui n'existent pas physiquement mais représentent
 *    le « dehors » : le fournisseur, le client, l'atelier, la benne à rebut.
 *
 * Ce sont eux qui donnent son SENS à un Mouvement, et c'est l'astuce centrale du
 * modèle : tout mouvement a TOUJOURS une source ET une destination, donc le journal
 * reste équilibré — comme une comptabilité en partie double. Un achat n'est plus
 * « une entrée » : c'est FOURNISSEUR → Quai. Une casse, c'est Zone → PERTE.
 * Résultat : UNE seule mécanique pour achat / vente / production / casse / inventaire,
 * sans jamais de quantité négative ni de type ENTREE/SORTIE en dur.
 */
public enum TypeEmplacement {

    // --- PHYSIQUES : ils existent vraiment dans l'entrepôt ---
    RECEPTION,   // zone d'arrivée des marchandises (avant rangement)
    STOCKAGE,    // emplacement de stock "classique"
    EXPEDITION,  // zone de départ (colis prêts à expédier)
    QUAI,        // quai de chargement / déchargement
    TRI,         // zone de tri / contrôle qualité

    // --- VIRTUELS : le « dehors », jamais un lieu réel ---
    FOURNISSEUR, // d'où viennent les achats     : FOURNISSEUR → Quai
    CLIENT,      // où partent les ventes        : Zone → CLIENT
    PRODUCTION,  // l'atelier (conso ET sortie)  : Zone → PRODUCTION, puis PRODUCTION → Zone
    PERTE,       // casse, rebut, démarque       : Zone → PERTE
    INVENTAIRE;  // écart d'inventaire (dans les 2 sens, sans faire disparaître la trace)

    /**
     * Cet emplacement est-il VIRTUEL (hors de l'entrepôt physique) ?
     *
     * Une seule source de vérité : la famille se déduit du type, on n'ajoute pas de
     * booléen `virtuel` qui pourrait un jour contredire le type (deux vérités = un bug
     * qui attend). Sert au filtrage IHM (ne pas proposer PERTE comme lieu de rangement)
     * et aux futurs contrôles métier.
     */
    public boolean estVirtuel() {
        return this == FOURNISSEUR || this == CLIENT || this == PRODUCTION
                || this == PERTE || this == INVENTAIRE;
    }

    /** Emplacement réel de l'entrepôt (l'inverse de {@link #estVirtuel()}). */
    public boolean estPhysique() {
        return !estVirtuel();
    }
}
