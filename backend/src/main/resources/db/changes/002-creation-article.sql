--liquibase formatted sql

--changeset dk:002-creation-article
-- Table des article (le "OÙ" de l'entrepôt) + séquence d'ids.
-- article_SEQ (increment 50) = convention Hibernate 6 pour PanacheEntity.

CREATE SEQUENCE article_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE article (
                             id      BIGINT       NOT NULL PRIMARY KEY,
                             reference      VARCHAR(255) NOT NULL UNIQUE,
                             designation    VARCHAR(255) NOT NULL,
                             description    VARCHAR(255),
                             unite          VARCHAR(255) NOT NULL,
                             tracabilite    VARCHAR(255) NOT NULL CHECK (tracabilite IN ('AUCUN', 'LOT', 'SERIE')) ,
                             actif   BOOLEAN      NOT NULL
);

--rollback DROP TABLE article; DROP SEQUENCE article_SEQ;