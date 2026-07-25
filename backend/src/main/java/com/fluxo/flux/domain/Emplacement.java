package com.fluxo.flux.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Emplacement — le « OÙ » de l'entrepôt, cœur d'un WMS.
 *
 * C'est ce qui distingue un WMS d'une simple gestion de stock : on ne suit pas
 * seulement « combien d'articles », mais « où exactement » dans l'entrepôt.
 *
 * Même pattern que Article (gestion-stock) : "Active Record" de Panache. L'entité
 * hérite de PanacheEntity (champ "id" auto-généré) et expose des méthodes
 * statiques (listAll, findById, persist...). Les champs publics génèrent
 * automatiquement getters/setters.
 *
 * Adressage fin (zone → allée → travée → niveau) : le `code` est l'adresse
 * lisible et UNIQUE (ex. "A-01-03-2"), les champs décomposés permettent de
 * filtrer/trier par zone ou allée. Choix moderne vs le legacy qui se contentait
 * d'un champ texte « zone » sans structure.
 */
@Entity
public class Emplacement extends PanacheEntity {

    /** Adresse lisible et unique de l'emplacement (ex. "A-01-03-2"). */
    @Column(nullable = false, unique = true)
    public String code;

    /** Libellé humain optionnel (ex. "Allée A - rack picking"). */
    public String libelle;

    /** Rôle de l'emplacement dans le flux (réception, stockage, expédition…). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TypeEmplacement type;

    // --- Adressage décomposé (facultatif, pour le tri/filtrage) ---

    /** Zone logique (ex. "A", "FROID", "QUARANTAINE"). */
    public String zone;

    /** Allée. */
    public String allee;

    /** Travée. */
    public String travee;

    /** Niveau (hauteur dans le rack). */
    public String niveau;

    /**
     * Emplacement utilisable ? On désactive (actif=false) plutôt que de supprimer,
     * pour préserver l'historique des mouvements qui le référencent.
     */
    @Column(nullable = false)
    public boolean actif = true;

    /** Recherche par code (l'identifiant métier unique).  com.fluxo.referentiel.domain*/
    public static Emplacement findByCode(String code) {
        return find("code", code).firstResult();
    }
}
