package com.fluxo.flux.reception;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ce que l'API RENVOIE — le contrat de SORTIE.
 *
 * Relations APLATIES en codes, avec le libellé en prime (`fournisseurRaisonSociale`,
 * `articleDesignation`) : ça évite au front un second appel juste pour afficher un nom.
 *
 * ⭐ `poidsCumuleDesLignes` et `ecartDePesee` sont CALCULÉS à la volée, jamais stockés :
 * le jour où une ligne change, ils suivent. Une valeur dérivée qu'on stockerait finirait
 * fatalement par mentir — c'est la même logique que le stock.
 */
public record ReceptionDto(
        Long id,
        String numero,
        String fournisseur,
        String fournisseurRaisonSociale,
        String transporteur,
        String numeroBlFournisseur,
        LocalDateTime dateReception,
        String emplacementArrivee,
        EtatReception etat,
        BigDecimal poidsTotalPese,
        BigDecimal poidsCumuleDesLignes,
        BigDecimal ecartDePesee,
        List<Ligne> lignes) {

    public record Ligne(
            Long id,
            String article,
            String articleDesignation,
            BigDecimal quantite,
            String unite,
            String numeroLot,
            BigDecimal poidsPese,
            /** La destination qui s'appliquera VRAIMENT (celle de la ligne, sinon l'en-tête). */
            String emplacementDestination) {

        static Ligne de(LigneReception l) {
            return new Ligne(
                    l.id,
                    l.article.reference,
                    l.article.designation,
                    l.quantite,
                    l.article.unite,
                    l.numeroLot,
                    l.poidsPese,
                    l.destinationEffective().code);
        }
    }

    static ReceptionDto de(Reception r) {
        return new ReceptionDto(
                r.id,
                r.numero,
                r.fournisseur.code,
                r.fournisseur.raisonSociale,
                r.transporteur != null ? r.transporteur.code : null,
                r.numeroBlFournisseur,
                r.dateReception,
                r.emplacementArrivee.code,
                r.etat,
                r.poidsTotalPese,
                r.poidsCumuleDesLignes(),
                r.ecartDePesee(),
                r.lignes.stream().map(Ligne::de).toList());
    }
}
