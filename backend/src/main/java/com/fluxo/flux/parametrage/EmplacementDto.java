package com.fluxo.flux.parametrage;   // même package que le Resource

import com.fluxo.flux.domain.Emplacement;
import com.fluxo.flux.domain.TypeEmplacement;

// le CONTRAT JSON de l'API /emplacements — record + mapper statique (même pattern qu'ArticleDto).
// Un record = classe immuable ultra-compacte : les champs listés sont AUSSI les paramètres du
// constructeur ET les accesseurs JSON. L'ordre des champs = l'ordre des arguments de 'de()'.
public record EmplacementDto(
        Long id,
        String code,
        String libelle,
        TypeEmplacement type,
        String zone,
        String allee,
        String travee,
        String niveau,
        boolean actif) {

    // Transforme l'ENTITÉ (interne) en DTO (exposé). ⚠️ Arguments POSITIONNELS : respecter
    // l'ordre des champs ci-dessus (piège rencontré sur ArticleDto : designation/description inversés).
    static EmplacementDto de(Emplacement e) {
        return new EmplacementDto(
                e.id,
                e.code,
                e.libelle,
                e.type,
                e.zone,
                e.allee,
                e.travee,
                e.niveau,
                e.actif);
    }
}
