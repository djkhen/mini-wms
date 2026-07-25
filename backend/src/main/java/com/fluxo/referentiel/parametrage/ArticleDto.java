package com.fluxo.referentiel.parametrage;   // avec le Resource

import com.fluxo.referentiel.domain.Article;
import com.fluxo.referentiel.domain.ModeTracabilite;

// le CONTRAT JSON de l'API /articles — record + mapper statique
public record ArticleDto(
		Long id,
		String reference,
		String designation,
		String description,
		String unite,
		ModeTracabilite tracabilite,
		boolean actif) {

	static ArticleDto de(Article art) {
		return new ArticleDto(
				art.id,
				art.reference,
				art.designation,   // ⚠️ 3e param = designation (etait INVERSE avec description !)
				art.description,   // ⚠️ 4e param = description
				art.unite,
				art.tracabilite,
				art.actif);
	}
}