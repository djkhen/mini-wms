package com.fluxo.referentiel.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Article — le « QUOI » du référentiel, partagé par TOUS les domaines
 * (flux, gpao, commercial). Volontairement PUR :
 *   - PAS de stock ici — et pas ailleurs non plus : le stock n'est stocké NULLE PART,
 *     il se CALCULE (Σ des Mouvements, cf. Mouvement.stockPhysique). Ne jamais ajouter
 *     de champ `quantite` mutable ici : ce serait le compteur, donc l'anti-pattern (§3).
 *   - PAS de prix ici (la tarification = domaine commercial, RegleTarification)
 */
@Entity
public class Article extends PanacheEntity {

	/** Identifiant métier unique et lisible (ex. "PLANCHE-22"). */
	@Column(nullable = false, unique = true)
	public String reference;

	@Column(nullable = false)
	public String designation;

	public String description;

	/** Unité de gestion (ex. "piece", "m", "kg"). */
	@Column(nullable = false)
	public String unite;

	/** Traçabilité OPTIONNELLE, décidée article par article. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	public ModeTracabilite tracabilite = ModeTracabilite.AUCUN;

	/** Désactiver plutôt que supprimer (préserve l'historique). */
	@Column(nullable = false)
	public boolean actif = true;

	public static Article findByReference(String reference) {
		return find("reference", reference).firstResult();
	}
}