--liquibase formatted sql

--changeset dk:007-creation-reception
-- Le 1er VRAI flux : un camion arrive -> UN document, N lignes (§6sexies).
--
-- La réception ne « met pas à jour le stock » : à sa validation elle GÉNÈRE des mouvements
-- FOURNISSEUR -> Quai, et le stock apparaît par dérivation. Aucune quantité n'est stockée ici.
--
-- Deux séquences distinctes :
--   reception_SEQ         -> les ids techniques (convention Hibernate, blocs de 50)
--   reception_numero_SEQ  -> le NUMÉRO MÉTIER lisible (REC-26-0001), incrément de 1.
-- Les mélanger donnerait des numéros à trous (1, 51, 101...) : l'id technique et
-- l'identifiant métier n'ont pas les mêmes exigences.

CREATE SEQUENCE reception_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE reception_numero_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE reception (
                           id                     BIGINT       NOT NULL PRIMARY KEY,
                           numero                 VARCHAR(255) NOT NULL UNIQUE,
                           fournisseur_id         BIGINT       NOT NULL REFERENCES tiers(id),
                           transporteur_id        BIGINT       REFERENCES tiers(id),
                           numero_bl_fournisseur  VARCHAR(255),
                           date_reception         TIMESTAMP    NOT NULL,
                           emplacement_arrivee_id BIGINT       NOT NULL REFERENCES emplacement(id),
                           etat                   VARCHAR(255) NOT NULL,
                           poids_total_pese       NUMERIC(19,4),

    -- Un poids négatif n'existe pas ; 0 non plus (on n'a pas pesé du vide).
                           CONSTRAINT ck_reception_poids_positif CHECK (poids_total_pese IS NULL OR poids_total_pese > 0)
);

CREATE SEQUENCE ligne_reception_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE ligne_reception (
                                 id                         BIGINT        NOT NULL PRIMARY KEY,
                                 reception_id               BIGINT        NOT NULL REFERENCES reception(id),
                                 article_id                 BIGINT        NOT NULL REFERENCES article(id),
                                 quantite                   NUMERIC(19,4) NOT NULL,
                                 numero_lot                 VARCHAR(255),
                                 poids_pese                 NUMERIC(19,4),
                                 emplacement_destination_id BIGINT        REFERENCES emplacement(id),

    -- Même règle que sur mouvement : on ne reçoit jamais une quantité négative ou nulle.
                                 CONSTRAINT ck_ligne_reception_quantite_positive CHECK (quantite > 0),
                                 CONSTRAINT ck_ligne_reception_poids_positif     CHECK (poids_pese IS NULL OR poids_pese > 0)
);

-- Lire les lignes d'une réception (le cas d'usage permanent) et retrouver les
-- réceptions d'un fournisseur.
CREATE INDEX idx_ligne_reception_reception ON ligne_reception (reception_id);
CREATE INDEX idx_reception_fournisseur     ON reception (fournisseur_id);
CREATE INDEX idx_reception_date            ON reception (date_reception);

--rollback DROP TABLE ligne_reception; DROP TABLE reception; DROP SEQUENCE ligne_reception_SEQ; DROP SEQUENCE reception_numero_SEQ; DROP SEQUENCE reception_SEQ;
