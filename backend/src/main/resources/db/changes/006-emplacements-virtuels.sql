--liquibase formatted sql

--changeset dk:006-emplacements-virtuels
-- Les 5 emplacements VIRTUELS — le « dehors » : fournisseur, client, atelier, rebut, inventaire.
--
-- ⚠️ Ce ne sont PAS des données de démo : ce sont des données STRUCTURELLES, sans lesquelles
-- aucun mouvement ne peut exister (un achat, c'est FOURNISSEUR → Quai). D'où leur place ici,
-- dans une migration — donc présentes chez CHAQUE client, y compris en production, et créées
-- automatiquement à chaque `CREATE SCHEMA` d'un nouveau tenant. Un seed applicatif, lui, se
-- désactive en prod : ces lignes-là ne doivent jamais pouvoir manquer.
--
-- `nextval` plutôt que des ids en dur : on ne marche pas sur les plages d'ids qu'Hibernate
-- réserve par blocs de 50 (allocationSize de PanacheEntity).

INSERT INTO emplacement (id, code, libelle, type, zone, actif) VALUES
    (nextval('emplacement_SEQ'), 'FOURNISSEUR', 'Fournisseurs — hors entrepot',      'FOURNISSEUR', 'VIRTUEL', true),
    (nextval('emplacement_SEQ'), 'CLIENT',      'Clients — hors entrepot',           'CLIENT',      'VIRTUEL', true),
    (nextval('emplacement_SEQ'), 'PRODUCTION',  'Atelier de production',             'PRODUCTION',  'VIRTUEL', true),
    (nextval('emplacement_SEQ'), 'PERTE',       'Pertes, casse et rebuts',           'PERTE',       'VIRTUEL', true),
    (nextval('emplacement_SEQ'), 'INVENTAIRE',  'Ecarts d''inventaire',              'INVENTAIRE',  'VIRTUEL', true);

--rollback DELETE FROM emplacement WHERE zone = 'VIRTUEL';
