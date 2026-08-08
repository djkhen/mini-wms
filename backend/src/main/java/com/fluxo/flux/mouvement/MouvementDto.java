package com.fluxo.flux.mouvement;

import com.fluxo.flux.domain.EtatMouvement;
import com.fluxo.flux.domain.Mouvement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ce que l'API RENVOIE — le contrat de SORTIE.
 *
 * 🔤 Relations APLATIES : `article`, `source` et `destination` sont des entités en base,
 * mais sortent en simples CODES. Sinon un mouvement renverrait trois fiches complètes
 * (article + 2 emplacements, avec tous leurs champs) pour dire « 150 planches du
 * fournisseur au quai » : lourd, illisible, et ça expose l'intérieur du modèle.
 *
 * `articleDesignation` est le seul ajout de confort : il évite au front un second appel
 * juste pour afficher un libellé lisible dans une liste.
 */
public record MouvementDto(
        Long id,
        String article,             // référence métier, pas l'objet
        String articleDesignation,  // confort d'affichage
        String numeroLot,
        BigDecimal quantite,
        String unite,
        String source,              // code de l'emplacement
        String destination,
        EtatMouvement etat,
        LocalDateTime dateEffet,
        LocalDateTime creeLe,
        String origineType,
        Long origineId) {

    static MouvementDto de(Mouvement m) {
        return new MouvementDto(
                m.id,
                m.article.reference,
                m.article.designation,
                m.numeroLot,
                m.quantite,
                m.unite,
                m.source.code,
                m.destination.code,
                m.etat,
                m.dateEffet,
                m.creeLe,
                m.origineType,
                m.origineId);
    }
}
