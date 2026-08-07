package com.fluxo.referentiel.parametrage;

import com.fluxo.referentiel.domain.Tiers;

import java.util.Map;

// Le CONTRAT JSON de l'API /tiers — record + mapper statique (même pattern qu'ArticleDto).
public record TiersDto(
        Long id,
        String code,
        String raisonSociale,
        String siret,
        String email,
        String telephone,
        boolean estFournisseur,
        boolean estTransporteur,
        boolean estClient,
        boolean actif,
        Map<String, Object> champsCustom) {

    static TiersDto de(Tiers t) {
        return new TiersDto(
                t.id,
                t.code,
                t.raisonSociale,
                t.siret,
                t.email,
                t.telephone,
                t.estFournisseur,
                t.estTransporteur,
                t.estClient,
                t.actif,
                t.champsCustom);
    }
}
