package com.fluxo.referentiel.domain;

/**
 * Comment un article est suivi dans l'entrepôt.
 * Le n° de lot / série est OPTIONNEL et se décide PAR ARTICLE :
 *   AUCUN : pas de traçabilité (consommables, visserie...)
 *   LOT   : suivi par lot (matières, péremption, rappel ciblé)
 *   SERIE : chaque unité est unique (pièces critiques, n° de série)
 */
public enum ModeTracabilite {
	AUCUN,
	LOT,
	SERIE
}