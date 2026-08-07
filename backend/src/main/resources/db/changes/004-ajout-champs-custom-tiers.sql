--liquibase formatted sql

--changeset dk:004-ajout-champs-custom-tiers
-- Colonne JSONB pour les champs PERSONNALISÉS par client (§6octies).
-- Réservoir de champs libres (clés différentes selon le client) ; la GOUVERNANCE
-- (quels champs, obligatoires, rendu du formulaire) viendra via config_champ, plus tard.
-- JSONB (pas JSON) : typé + indexable nativement en PostgreSQL (index GIN si besoin).

ALTER TABLE tiers ADD COLUMN champs_custom JSONB;

--rollback ALTER TABLE tiers DROP COLUMN champs_custom;
