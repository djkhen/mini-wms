--liquibase formatted sql

--changeset dk:005-creation-mouvement
-- LE CŒUR : le journal des mouvements (§4). Le stock en est DÉRIVÉ — il n'existe
-- volontairement AUCUNE table `stock` : elle serait un compteur, donc l'anti-pattern.
--
-- Les 2 CHECK ci-dessous ne sont pas décoratifs : ils gravent les règles d'or DANS la
-- base. Même un script d'import ou une correction SQL à la main ne pourra pas les violer.
-- Une règle garantie par la base vaut mieux qu'une règle rappelée dans un commentaire.

CREATE SEQUENCE mouvement_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE mouvement (
                           id             BIGINT        NOT NULL PRIMARY KEY,
                           article_id     BIGINT        NOT NULL REFERENCES article(id),
                           numero_lot     VARCHAR(255),
                           quantite       NUMERIC(19,4) NOT NULL,
                           unite          VARCHAR(255)  NOT NULL,
                           source_id      BIGINT        NOT NULL REFERENCES emplacement(id),
                           destination_id BIGINT        NOT NULL REFERENCES emplacement(id),
                           etat           VARCHAR(255)  NOT NULL,
                           date_effet     TIMESTAMP     NOT NULL,
                           cree_le        TIMESTAMP     NOT NULL,
                           origine_type   VARCHAR(255),
                           origine_id     BIGINT,

    -- Règle 1 : la quantité est TOUJOURS positive. Le sens vient des emplacements,
    -- jamais d'un signe -> une quantité négative n'a aucun sens dans ce modèle.
                           CONSTRAINT ck_mouvement_quantite_positive CHECK (quantite > 0),

    -- Un mouvement va forcément d'un endroit vers un AUTRE : sinon rien ne bouge.
                           CONSTRAINT ck_mouvement_source_differente CHECK (source_id <> destination_id)
);

-- La dérivation du stock somme par (article, emplacement, état) dans les DEUX sens :
-- un index par sens, sinon chaque calcul balaierait toute la table.
CREATE INDEX idx_mouvement_entrees ON mouvement (article_id, destination_id, etat);
CREATE INDEX idx_mouvement_sorties ON mouvement (article_id, source_id, etat);

-- Pour le stock « à une date passée » (filtre sur la date du fait réel).
CREATE INDEX idx_mouvement_date_effet ON mouvement (date_effet);

--rollback DROP TABLE mouvement; DROP SEQUENCE mouvement_SEQ;
