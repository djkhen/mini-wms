package com.fluxo.flux.reception;

/**
 * Cycle de vie d'une réception — c'est la VALIDATION qui fait naître le stock.
 *
 * Un document n'est pas un fait (§ parcours ①) : tant que la réception est en
 * BROUILLON, rien n'existe en stock. C'est le passage à VALIDEE qui génère les
 * mouvements FOURNISSEUR → Quai, et donc fait apparaître la marchandise.
 */
public enum EtatReception {

    /**
     * En cours de saisie. Le réceptionnaire ajoute/corrige ses lignes librement :
     * aucun mouvement n'existe encore, donc aucun stock n'est impacté.
     */
    BROUILLON,

    /**
     * Réceptionnée : les mouvements ont été générés, le stock existe.
     * ⚠️ On ne « dé-valide » pas : les mouvements produits sont immuables. Une erreur
     * se corrige en inversant les mouvements concernés (règle 6), pas en revenant en arrière.
     */
    VALIDEE,

    /** Abandonnée avant validation (camion reparti, erreur de saisie). Aucun mouvement produit. */
    ANNULEE
}
