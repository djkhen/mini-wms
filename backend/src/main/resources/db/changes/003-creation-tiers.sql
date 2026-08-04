--liquibase formatted sql

--changeset dk:003-creation-tiers
-- Table tiers : le partenaire polyvalent (fournisseur / transporteur / client via rôles booléens).
-- tiers_SEQ (increment 50) = convention Hibernate 6 pour PanacheEntity.
-- Colonnes multi-mots en snake_case (mappées via @Column dans l'entité).

CREATE SEQUENCE tiers_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE tiers (
                       id               BIGINT       NOT NULL PRIMARY KEY,
                       code             VARCHAR(255) NOT NULL UNIQUE,
                       raison_sociale   VARCHAR(255) NOT NULL,
                       siret            VARCHAR(255),
                       email            VARCHAR(255),
                       telephone        VARCHAR(255),
                       est_fournisseur  BOOLEAN      NOT NULL,
                       est_transporteur BOOLEAN      NOT NULL,
                       est_client       BOOLEAN      NOT NULL,
                       actif            BOOLEAN      NOT NULL
);

--rollback DROP TABLE tiers; DROP SEQUENCE tiers_SEQ;
