package com.example;

/**
 * Nature fonctionnelle d'un emplacement dans l'entrepôt.
 *
 * Un emplacement n'est pas qu'une case de stockage : selon sa zone, il joue un
 * rôle dans le flux (réception à l'entrée, expédition en sortie, tri/quai…).
 * On modélise ce rôle par un enum plutôt qu'un texte libre (cf. legacy : statuts
 * en varchar non contraints) → seules ces valeurs sont possibles.
 */
public enum TypeEmplacement {
    RECEPTION,   // zone d'arrivée des marchandises (avant rangement)
    STOCKAGE,    // emplacement de stock "classique"
    EXPEDITION,  // zone de départ (colis prêts à expédier)
    QUAI,        // quai de chargement / déchargement
    TRI          // zone de tri / contrôle qualité
}
