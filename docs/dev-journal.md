# Journal de dev — mini-WMS

Notes chronologiques (décisions, blocages, idées). Le plus récent en haut.

> ⚠️ **Anonymisation obligatoire** : ce journal est versionné. On n'y écrit que des
> **codenames** (FLUX, TRACK, PRESTA) et des termes génériques — jamais de nom réel
> d'entreprise, site, client, personne ou appli. Table de correspondance : fichier
> local gitignoré `_legacy/SOURCES.md`.

---

## 2026-06-28 — Réception des sources legacy (4 applis)

Sources fournies (locales, hors repo, `D:\migtre\*`, jamais commitées) :

- **FLUX** — gestion de flux logistique d'entrepôt (anomalies réception, bons de
  sortie matière, inventaire, flux direct, réappro urgent, retour tri, tri qualité,
  mise en conformité…). PHP + dump SQL dispo. → **candidat principal** du mini-WMS.
- **FLUX-OLD** — version antérieure de FLUX (utile pour comparer l'évolution).
- **TRACK** — suivi entrepôt + bornes d'appel : réception, expédition, items,
  mouvements, colisage, caisses. **Migration Quarkus déjà entamée** dans la source.
- **PRESTA** — gestion de prestations facturées à l'UO, multi-sites (domaine
  facturation, distinct du WMS → parking pour plus tard).

Protocole confidentialité acté (cf. `_legacy/SOURCES.md`) : lecture seule des
sources, reconstruction du concept générique, zéro donnée/identifiant réel dans le
repo. Dumps SQL = lecture **structure** uniquement.

**Décision en attente** : quelle(s) appli(s) pilote(nt) le mini-WMS générique, et
que fait-on de la migration Quarkus déjà commencée dans TRACK.

## 2026-06-28 — Reverse FLUX + TRACK terminé → analyse consignée

Reverse-engineering des deux apps fait (lecture seule, anonymisé). Synthèse complète
dans [legacy-analysis.md](legacy-analysis.md). Points saillants :

- **TRACK = WMS physique complet** (entrepôts/emplacements/stock/mouvements + récep.
  → items → colisage → caisses → expédition → transport), multi-tenant, facturation
  UO. **Aucun code Java** : la « migration » est un refactor PHP (Service/Manager +
  embryon d'API REST mobile). Notre schéma proposé est validé et enrichi par TRACK.
- **FLUX = couche demandes/workflow** : ~13 tables clonées (aléa, sortie matière,
  réappro, transfert, tri qualité, inventaire, anomalie récep., mise en conformité…)
  avec tronc commun + machine à états ; stock réel délégué à un ERP externe.
- **Synthèse cible** : `Demande` générique (FLUX) qui génère des `Mouvement`
  physiques (TRACK), sur socle Article/Emplacement/Stock.
- 🔴 **Sécurité** : FLUX versionnait des secrets de prod en clair → signalé à
  l'utilisateur (hors de notre repo, gitignoré).

**Décisions de périmètre à trancher avant de coder** : positionnement (socle
physique seul / + couche workflow), multi-tenant oui/non, frontière du MVP.

## 2026-06-28 — Initialisation du repo

- Création du repo `mini-wms` (dossier voisin de `mini-projet`).
- Cadrage posé dans [migration-wms-scoping.md](migration-wms-scoping.md) :
  domaine, schéma ER (périmètre A), décisions « modernes vs legacy ».
- Confidentialité : `_legacy/` gitignoré, on reconstruit le concept pas le code.
- **À faire ensuite** : déposer le PHP source dans `_legacy/`, reverse de la BDD,
  comparer avec le schéma proposé, puis coder l'entité `Emplacement`.

### Infos techniques legacy
- **SGBD source** : MySQL, migré en **MariaDB** (compatible MySQL pour la lecture
  du schéma : `SHOW CREATE TABLE`, `mysqldump`).
- **Cible** : PostgreSQL (cohérent avec gestion-stock). On ne porte PAS le SQL ligne
  à ligne → reconstruction en entités Panache, Hibernate génère le DDL Postgres.
  Le dump MariaDB sert uniquement à comprendre le modèle.
- Export structure seule recommandé : `mysqldump --no-data --skip-comments`.
- Traductions au passage : `AUTO_INCREMENT`→id Panache, `ENUM` colonne→enum Java
  `@Enumerated(STRING)`, `TINYINT(1)`→`boolean`, FK absentes (MyISAM)→`@ManyToOne`.
