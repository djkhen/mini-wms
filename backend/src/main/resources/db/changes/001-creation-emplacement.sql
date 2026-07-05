--liquibase formatted sql

--changeset dk:001-creation-emplacement
-- Table des emplacements (le "OÙ" de l'entrepôt) + séquence d'ids.
-- emplacement_SEQ (increment 50) = convention Hibernate 6 pour PanacheEntity.

CREATE SEQUENCE emplacement_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE emplacement (
                             id      BIGINT       NOT NULL PRIMARY KEY,
                             code    VARCHAR(255) NOT NULL UNIQUE,
                             libelle VARCHAR(255),
                             type    VARCHAR(255) NOT NULL,
                             zone    VARCHAR(255),
                             allee   VARCHAR(255),
                             travee  VARCHAR(255),
                             niveau  VARCHAR(255),
                             actif   BOOLEAN      NOT NULL
);

--rollback DROP TABLE emplacement; DROP SEQUENCE emplacement_SEQ;