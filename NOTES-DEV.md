# Notes de développement — mini-WMS

Mémo pratique du projet (migration legacy → Quarkus + Flutter) : décisions,
architecture, pièges. À lire en premier quand on (re)prend le projet.

> 🧭 **Repère d'univers** : **wms = LA PLATEFORME (nom de travail Fluxo)** = flux/WMS
> + GPAO + IA (endpoint `/emplacements` pour l'instant). À distinguer de
> **gs = gestion-stock = ARTICLES 📦** (fini, vitrine) et **mp = mini-projet** (labo).
> ⚠️ Depuis le 2026-07-05, stack **alignée sur LE BOULOT** (règle d'alignement, cf.
> `docs/conception-plateforme.md` §8) : **Gradle 9.3.1 · Quarkus 3.33 LTS · Liquibase
> · packages `com.fluxo.*` · artifact `core-metier`** (les entrées plus anciennes
> ci-dessous qui parlent de Maven/`com.example`/3.17.5 sont HISTORIQUES).
> Toujours : un seul backend sur 8080 (couper gs/mp avant), base hôte 5435.

Notes chronologiques (décisions, blocages, idées). Le plus récent en haut.

> ⚠️ **Anonymisation obligatoire** : ce journal est versionné. On n'y écrit que des
> **codenames** (FLUX, TRACK, PRESTA) et des termes génériques — jamais de nom réel
> d'entreprise, site, client, personne ou appli. Table de correspondance : fichier
> local gitignoré `_legacy/SOURCES.md`.

---

## 2026-07-05 — ⭐ RECETTE LIQUIBASE (mise en place + usage quotidien)

**Pourquoi** : `hibernate-orm.database.generation=update` laissait Hibernate modifier
le schéma tout seul → interdit chez un client (non tracé, non reproductible). Avec
Liquibase, **le schéma est du code versionné** : des fichiers SQL numérotés, joués
dans l'ordre, tracés dans la table `DATABASECHANGELOG` de la base.

### Mise en place (faite une fois — les 4 pas)
1. **Dépendance** — `build.gradle`, bloc `dependencies { }` :
   `implementation 'io.quarkus:quarkus-liquibase'`
   ⚠️ TOUJOURS dans `build.gradle` (jamais `settings.gradle` = identité/plugins).
2. **Config** — `application.properties` :
   `quarkus.liquibase.migrate-at-start=true` (joue les migrations au boot)
   `quarkus.hibernate-orm.database.generation=validate` (Hibernate ne fait plus que VÉRIFIER)
3. **Registre maître** — `resources/db/changeLog.xml` : la table des matières, un
   `<include file="db/changes/NNN-xxx.sql"/>` par évolution, dans l'ordre.
4. **Changesets** — `resources/db/changes/NNN-description.sql` en **SQL formaté** :
   ```
   --liquibase formatted sql

   --changeset dk:NNN-description
   ...SQL...
   --rollback ...comment annuler...
   ```
   Les lignes `--liquibase` et `--changeset auteur:id` sont de la SYNTAXE, pas des
   commentaires. Un changeset n'est JAMAIS rejoué (ni modifié après coup !).

### Usage quotidien (chaque évolution de schéma)
1. Nouveau fichier `db/changes/002-creation-article.sql` (numéro suivant) ;
2. L'ajouter au `changeLog.xml` (`<include ...>`) ;
3. Redémarrer → Liquibase joue ce qui manque. Log attendu :
   `ChangeSet db/changes/002-...::dk ran successfully`.

### Pièges appris
- **JAMAIS modifier un changeset déjà joué** (checksum → erreur au boot) : on écrit
  un NOUVEAU fichier qui corrige (ALTER...). L'historique est immuable, comme git.
- La séquence `xxx_SEQ INCREMENT BY 50` = convention Hibernate 6 pour les id Panache.
- Reset complet en dev : `docker compose down -v` (efface la base, le seed repeuple).
- 🚨 **Le `.gitignore` du repo contient `*.sql`** (protection anti-dumps legacy) →
  les migrations étaient INVISIBLES pour git (ni trackées ni signalées !). Fix :
  exception `!backend/src/main/resources/db/**/*.sql` juste sous la règle `*.sql`.
  Réflexe : après création d'un fichier, vérifier qu'il apparaît dans `git status`.
- Montée 3.17→3.33 : des propriétés ont été RENOMMÉES —
  `quarkus.http.cors=true` → `quarkus.http.cors.enabled=true` (l'ancienne était
  silencieusement IGNORÉE = CORS mort !) ; `quarkus.hibernate-orm.database.generation`
  → `quarkus.hibernate-orm.schema-management.strategy`. Toujours lire les WARN au boot.
- Le **wrapper Gradle** (`gradlew`, `gradlew.bat`, `gradle/`) SE VERSIONNE (standard) ;
  les caches `.gradle/` et `build/` s'ignorent.

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

## 2026-06-28 — Décisions figées + scaffolding backend + entité Emplacement

Décisions (cf. scoping §7) : **modèle le plus complet** (socle physique + couche
workflow `Demande`), **single-tenant** conçu pour évoluer, stack Quarkus 3.17.5 /
Java 21 / Panache / PG 16 / OpenAPI. *(Conventions précisées juste après : voir
l'entrée « ré-alignées sur gs ».)*

Branche `feature/backend-emplacement` :
- Scaffolding Quarkus : `pom.xml`, `Dockerfile`, `application.properties`,
  `docker-compose.yml` (db + backend).
- 1re entité **Emplacement** (cœur WMS) : code unique + adressage fin
  (zone/allée/travée/niveau) + `type` (enum TypeEmplacement) + `actif`. CRUD REST
  complet (`EmplacementResource`) avec validation et unicité du code. Seed de démo
  (`EmplacementDataInitializer`, données fictives).
- ➡️ Ensuite : `feature/article`, puis Stock, Mouvement, Reception, Demande.

## 2026-06-28 — Conventions ré-alignées sur gestion-stock (gs)

À la demande de l'utilisateur, on calque les conventions sur **gs** (le projet
portfolio) plutôt que sur **mp** (le labo) :
- Package **`com.example`** (+ sous-packages par feature à venir), et non l'ancien `com.example.wms`.
- **Pas de Bean Validation** : validation manuelle `valider()` renvoyant **422**
  (champ obligatoire), **409** (doublon code), **404** (introuvable), **422** si
  `id` fourni à la création. → comme `ArticleResource`.
- Initializer nommé par entité (**`EmplacementDataInitializer`**) avec `Logger` JBoss.
- **Un seul backend sur 8080** (couper gs/mp avant) ; base sur port hôte 5435.
- Notes de dev dans **`NOTES-DEV.md`** à la racine (comme gs), avec « Repère
  d'univers ». Le reverse/cadrage migration restent dans `docs/`.

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
