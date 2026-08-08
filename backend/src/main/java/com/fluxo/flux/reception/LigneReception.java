package com.fluxo.flux.reception;

import com.fluxo.flux.domain.Emplacement;
import com.fluxo.referentiel.domain.Article;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Une ligne de réception = UN article reçu, dans UNE quantité, avec éventuellement son lot.
 *
 * C'est la réponse à « le réceptionnaire reçoit 3 colis d'articles différents » : il ne
 * saisit PAS 3 mouvements, il saisit UNE réception à N lignes. Les cartons qu'on déballe
 * ne sont pas modélisés — un colis ne devient une entité que le jour où on doit le
 * manipuler en tant qu'unité (à l'expédition).
 */
@Entity
@Table(name = "ligne_reception")
public class LigneReception extends PanacheEntity {

    /** L'en-tête à laquelle appartient cette ligne (le camion, le BL). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "reception_id", nullable = false)
    public Reception reception;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    public Article article;

    /** Quantité REÇUE (celle qu'on a comptée, pas celle qui était commandée). */
    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal quantite;

    /** N° de lot fournisseur — obligatoire si l'article est suivi en LOT ou SERIE. */
    @Column(name = "numero_lot")
    public String numeroLot;

    /**
     * ⭐ Poids RÉELLEMENT pesé de cette ligne — le différenciateur métier du bois,
     * où l'on achète au poids ce qu'on stocke à la pièce.
     *
     * ⚠️ À ne pas confondre avec {@link Reception#poidsTotalPese} (la pesée du camion au
     * pont-bascule) : les deux coexistent VOLONTAIREMENT et ne se déduisent pas l'un de
     * l'autre. Leur écart est une information de contrôle, pas une incohérence.
     */
    @Column(name = "poids_pese", precision = 19, scale = 4)
    public BigDecimal poidsPese;

    /**
     * Où ranger CETTE ligne. Facultatif : par défaut tout arrive à l'emplacement
     * d'arrivée de l'en-tête (le quai). On ne surcharge que le cas particulier —
     * par exemple un lot litigieux dirigé vers la zone de TRI.
     */
    @ManyToOne
    @JoinColumn(name = "emplacement_destination_id")
    public Emplacement emplacementDestination;

    /** La destination qui s'appliquera vraiment : celle de la ligne, sinon celle de l'en-tête. */
    public Emplacement destinationEffective() {
        return emplacementDestination != null ? emplacementDestination : reception.emplacementArrivee;
    }
}
