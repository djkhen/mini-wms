package com.fluxo.referentiel.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Tiers — le partenaire POLYVALENT du référentiel (fournisseur / transporteur / client).
 *
 * Idée clé (schéma appro) : UNE table, des RÔLES portés par des booléens. Un transporteur
 * qui est AUSSI fournisseur = une seule fiche (les deux booléens à true). Réutilisable
 * tel quel côté vente (est_client). Remplace des entités Fournisseur/Transporteur séparées.
 *
 * `actif` (règle snapshot §6septies) : on DÉSACTIVE un tiers obsolète, on ne le supprime pas —
 * pour ne pas altérer l'historique des documents qui le référencent (FK + copie figée).
 *
 * ⚠️ Champs multi-mots : `@Column(name="snake_case")` EXPLICITE (1ers champs multi-mots du projet)
 * pour un mapping sans ambiguïté avec la colonne, en mode Hibernate `validate`.
 */
@Entity
public class Tiers extends PanacheEntity {

    /** Identifiant métier unique et lisible (ex. "SCIERIE-DUPONT"). */
    @Column(nullable = false, unique = true)
    public String code;

    @Column(name = "raison_sociale", nullable = false)
    public String raisonSociale;

    public String siret;
    public String email;
    public String telephone;

    // --- Les RÔLES (un tiers peut en cumuler) ---
    @Column(name = "est_fournisseur", nullable = false)
    public boolean estFournisseur = false;

    @Column(name = "est_transporteur", nullable = false)
    public boolean estTransporteur = false;

    @Column(name = "est_client", nullable = false)
    public boolean estClient = false;

    /** Utilisable ? On désactive plutôt que supprimer (préserve l'historique des documents). */
    @Column(nullable = false)
    public boolean actif = true;

    /**
     * Champs PERSONNALISÉS par client (§6octies) — colonne JSONB : des clés LIBRES, différentes
     * selon le client (schéma). Réservoir de valeurs ; la gouvernance (quels champs, obligatoires…)
     * viendra via `config_champ`. `@JdbcTypeCode(JSON)` = mapping natif Map ↔ jsonb PostgreSQL.
     *
     * ⚠️ TYPES — JSON n'en connaît que 6 : texte, nombre, booléen, null, tableau, objet.
     *  • Pas de type DATE → stocker en TEXTE ISO 8601 ("2026-08-07", "2026-08-07T14:30:00") :
     *    tri alphabétique = tri chronologique, et cast SQL direct `(champs_custom->>'x')::date`.
     *    JAMAIS "07/08/2026" (ambigu, ni triable ni castable) — le format d'affichage = le FRONT.
     *  • JAMAIS d'argent / quantité / poids ici (décimal → Double = arithmétique approximative) :
     *    tout ce qui entre dans un calcul ou une contrainte mérite une vraie colonne NUMERIC.
     *  • Aucun type n'est imposé par la colonne (elle accepte "entier":"quinze") : c'est un
     *    RÉSERVOIR, pas un contrat → `config_champ.type` déclare, l'appli VALIDE avant écriture.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "champs_custom")
    public Map<String, Object> champsCustom;

    /** Recherche par code (l'identifiant métier unique). */
    public static Tiers findByCode(String code) {
        return find("code", code).firstResult();
    }
}
