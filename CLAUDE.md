# mini_wms — contexte projet (handoff Claude Code)

> Fichier de contexte à placer à la racine du repo (`CLAUDE.md`). Résume les décisions d'architecture et le modèle de domaine actés en discussion. À faire évoluer au fur et à mesure.

> ⚠️ **RÉCONCILIATIONS (2026-07-25) — ce doc a été rédigé dans une autre session ; corrections :**
> 1. **Migrations : lire LIQUIBASE partout où c'est écrit « Flyway »** — outil aligné sur le boulot de
>    l'auteur (règle d'alignement) ; le principe (schéma versionné en migrations) est identique. Déjà en
>    place : `db/changeLog.xml` + `db/changes/00N-*.sql`.
> 2. **Périmètre** : `mini_wms` = **LA plateforme unifiée « Fluxo »** (WMS/flux + **GPAO** + IA). Ce doc
>    couvre le **socle technique + flux/production** ; les **couches métier** (tarification/UO, devis,
>    ordonnancement, moteur de formules GPAO/débit + CODE SEI, éditions, intégrations Sage X3/WMS) sont
>    dans **`docs/conception-plateforme.md`** (référence complémentaire). `gestion-stock` = appli sœur
>    SIMPLE et **finie** (pas fusionnée ici).
> 3. Build = **Gradle** (aligné boulot), Quarkus **3.33 LTS**.

---

## 1. Ce qu'est le projet

`mini_wms` = WMS (gestion d'entrepôt + gestion de flux) qui sert de **socle**, étendu vers une **GPAO** (gestion de production) sur un métier **bois** (planches, CTP, OSB, sections massives).

**Objectif double** : pièce de portfolio différenciante (positionnement consulting) + éventuel produit vertical pour petits industriels du bois, mal servis par le MRP générique d'Odoo. On ne clone PAS Odoo : on en reprend les **bons modèles métier** (mouvement de stock, nomenclature, OF) réimplémentés sur un stack maîtrisé.

**Différenciateur assumé** : la **traçabilité lot/série** amont→aval (remonter d'un défaut aux lots et clients impactés), crédibilisée par un vécu safety-critical.

---

## 2. Stack

- **Backend** : Quarkus + Hibernate ORM avec **Panache** + PostgreSQL
- **Frontend** : Flutter (mobile) + Angular/Nginx (web)
- **Auth** : Keycloak (OIDC / bearer-only côté Quarkus, PKCE côté Flutter)
- **Migrations** : **Liquibase** (⚠️ le doc dit parfois « Flyway » — lire Liquibase, cf. réconciliations)
- **Conteneurisation** : Docker / Docker Compose
- **Déploiement** : Railway (build depuis le repo GitHub, HTTPS auto, CD à chaque push)
- **CI** : GitHub Actions (à mettre en place)

---

## 3. Principe d'architecture central (NON négociable)

**Le stock n'est jamais stocké, il est dérivé des mouvements.**

Toute la conception découle de cet invariant. Ne jamais introduire de champ `quantity` mutable sur un article/emplacement. Le stock courant = somme des mouvements.

Conséquences directes :
- Stock à **n'importe quelle date passée** = gratuit (filtrer `effective_date <= X`).
- Distinguo **physique** (mouvements `DONE`) vs **prévisionnel** (`DONE` + `DRAFT`).
- Historique **auditable** façon journal comptable.

---

## 4. Le mouvement (`stock_move`) — le cœur

Règles impératives :

1. **`quantity` toujours positive.** Le sens est porté par `source_location` → `dest_location`, jamais par un signe.
2. **La sémantique vient des emplacements**, pas d'un type en dur. Emplacements physiques (`INTERNE`) + **virtuels** : `FOURNISSEUR`, `CLIENT`, `PRODUCTION`, `PERTE`, `INVENTAIRE`.
   - Achat = `FOURNISSEUR → Entrepôt`
   - Vente = `Entrepôt → CLIENT`
   - Consommation prod = `Entrepôt → PRODUCTION`
   - Entrée prod = `PRODUCTION → Entrepôt`
   - Casse = `Entrepôt → PERTE`
3. Champ `kind` autorisé **uniquement comme étiquette dérivée** (filtrage/affichage), jamais comme source de vérité.
4. **Deux dates** : `effective_date` (mouvement réel) + `created_at` (saisie en base).
5. **État** : `DRAFT | DONE | CANCELLED`. Un `DRAFT` n'impacte pas le stock physique.
6. **Immuable une fois `DONE`** : on n'édite/supprime jamais. Erreur → mouvement inverse.
7. **Lien à l'origine** : `origin_type` + `origin_id` (remonter à l'OF / réception / commande source).

Champs : `product_id`, `lot_id?`, `quantity`, `uom_id`, `source_location_id`, `dest_location_id`, `source_package_id?`, `dest_package_id?`, `effective_date`, `created_at`, `state`, `origin_type`, `origin_id`.

---

## 5. Article (`product`) — toute la variabilité bois vit ici

Le flux reste générique ; la spécificité métier est portée par l'article, **pas** par héritage Java (surtout pas de `Bois extends Article`) ni par EAV.

- `family` : `SIMPLE | BOIS | PANNEAU | CAISSE`
- Dimensions **en colonnes**, nullables selon famille : `thickness_mm`, `width_mm`, `length_mm`
- `material_id` → `material` (la densité vit là, pas sur l'article)
- `stock_uom_id` → **une seule unité de stockage décidée une fois** ; le reste est conversion à la saisie
- `tracking` : `NONE | LOT | SERIAL`
- Champs contenant (si `is_container`) : `payload_max_kg`, `tare_kg`, `inner_l/w/h`

Dérivés calculés : `volumeM3()` (ép×larg×long), `surfaceM2()` (larg×long).

**Création d'article à la volée depuis la réception** requise : dans le bois, une section non référencée arrive souvent. Code généré type `BM-SAPIN-45x95x3000`.

Question ouverte : deux dimensions ≠ deux articles distincts (standard, simple) **ou** dimensions variables sur un même article (→ porter les dimensions sur le `lot`) ? Défaut retenu : **un article par dimension**.

---

## 6. Matériau, densité, humidité

- `material` (référentiel) : `code`, `label`, `density_kg_m3` (nominale), `ref_moisture_pct`. Remplace le champ `essence` en String.
- Densité **variable avec l'humidité** (bois vert ≈ ×2 vs séché). Donc :
  - densité nominale → sur `material`
  - mesure réelle → sur `lot` : `actual_moisture_pct`, `actual_weight_kg`
- **Cascade de poids** : poids pesé (lot) → sinon densité corrigée par humidité mesurée → sinon densité nominale.
- **Contrôle de réception** : comparer poids théorique (dimensions × densité) au poids pesé. Écart > seuil → alerte (bois humide ou quantité fausse).

Ordres de grandeur : sapin ~450, chêne ~700, OSB ~600-650, CTP ~500-550 kg/m³.

---

## 7. La caisse — produit fabriqué PUIS contenant

Concept clé : la caisse a **trois vies**, sans jamais toucher au mouvement.

1. **Fabriquée** : c'est un `product` (`family=CAISSE`, `tracking=SERIAL`) avec sa `bom` ; un `work_order` génère les mouvements.
2. **Devient contenant** : on crée un `package` (exemplaire physique) → `product_id` (la caisse) + `lot_id` (n° série) + `parent_package_id?` (imbrication caisse/palette).
3. **Remplie** : mettre un article dedans = un `stock_move` **au même emplacement** avec `dest_package_id` renseigné.

**Le contenu d'une caisse est dérivé** exactement comme le stock (Σ mouvements entrants − sortants sur le package). Aucune table de contenu à maintenir.

Contrôle au remplissage : Σ poids contenu ≤ `payload_max_kg`. Poids brut expédié = `tare_kg` + contenu (→ bon de transport).

---

## 8. GPAO — extension par génération de mouvements

- `bom` (nomenclature) : `product_id` (produit fini), `quantity`, `version`, `active`
- `bom_line` : `bom_id`, `component_id` → product, `quantity`, `uom_id`
- `work_order` (OF) : `bom_id`, `product_id`, `quantity`, `state` (`DRAFT|CONFIRMED|IN_PROGRESS|DONE|CANCELLED`), `planned_date`, `done_date`, `produced_lot_id`

Un OF **ne modifie pas le stock** : à sa progression, il **génère des mouvements** — consommation composants (`Entrepôt → PRODUCTION`) selon la BOM, puis entrée produit fini (`PRODUCTION → Entrepôt`). Aucune mécanique nouvelle, juste un déclencheur de plus.

Question ouverte : caisses **standardisées** (BOM statique par modèle) ou **sur mesure** par commande (BOM générée par configuration) ?

---

## 9. Multi-tenant

- **Modèle retenu** : app partagée + **isolation par schéma Postgres** (Hibernate multi-tenant mode `SCHEMA`). 1 tenant = 1 client = 1 schéma = 1 code immuable.
- Démarrage avec **un seul tenant `default`**, mécanique en place ; client suivant = `CREATE SCHEMA` + Flyway + 1 ligne de config.
- **Le tenant vient du JWT Keycloak** (claim `tenant` ou realm par client), **jamais** d'un header/param/URL contrôlé par l'utilisateur.
- **Résolution du tenant = frontière unique** dans le code (un seul `TenantResolver`). Rien d'autre ne sait comment on identifie un client → choix réversible.
- Garde-fous : valider le tenant contre la liste connue (anti-injection sur nom de schéma) ; rejeter (401) si pas de tenant, jamais de fallback silencieux ; test d'intégration « token tenant A ne voit rien de tenant B ».
- Pas de colonne `tenant_id` (c'était le modèle rejeté, trop risqué). Un client à forte exigence conformité → silo dédié possible sans changer le code métier.
- Flyway : migrer **tous** les schémas (Hibernate a un jeu d'entités unique → la table doit exister partout). Custom pour UN client → **feature flag** (table présente partout, activée pour un tenant) ; custom lourd → **silo dédié**.

---

## 10. Packaging JVM vs natif

- **Dev** : JVM (dev mode, boucle rapide, débug).
- **Prod 1-3 clients** : JVM.
- **Densité (silo 8-10 clients)** : passer en **natif** (RAM ÷4 : ~50-100 Mo vs 250-400 Mo).
- Risques natif : closed-world GraalVM (réflexion/proxies dynamiques cassent hors extensions officielles) ; bugs qui n'apparaissent qu'en natif → **tests `@QuarkusIntegrationTest` sur le binaire natif en CI obligatoires**. Build lent/gourmand → build en CI, pas en local.
- Règle de sécurité : rester sur les **extensions officielles Quarkus** (Panache, RESTEasy Reactive, OIDC, Flyway). Vérifier toute lib tierce hors écosystème.

---

## 11. Déploiement & ops

- **Railway** : projet depuis repo GitHub, build via **Dockerfile** (forcer, Nixpacks gère mal Quarkus). PostgreSQL en service Railway. Variables clés : `QUARKUS_DATASOURCE_JDBC_URL` (jdbc:… reconstruit depuis les vars PG), `QUARKUS_HTTP_HOST=0.0.0.0`, `QUARKUS_HTTP_PORT=${{PORT}}`. Domaine public généré, HTTPS inclus. Push = redeploy (CD gratuit).
- **Filestore** : les pièces jointes/fichiers ne vont pas en base → volume Docker dédié, sinon perte au redémarrage.
- **Silo (si modèle 1 un jour)** : script `provision-client.sh` à écrire **dès le client 1** (conteneur `-p` + `.env`, base/schéma + Flyway, reverse proxy wildcard `*.app.com`, DNS wildcard, realm Keycloak, backup). Le script = la doc d'infra, toujours à jour. Bash suffit jusqu'à ~15-20 clients ; au-delà, Ansible/K8s.

---

## 12. Index & perf (anticipé)

- `stock_move` est la seule table qui grossit vraiment. Index : `(product_id, state, effective_date)`, `(dest_location_id, state)`, `(source_location_id, state)`, `(dest_package_id)`, `(source_package_id)`.
- Quand le recalcul ralentit (centaines de milliers de mouvements) : table de **snapshot** (stock figé à une date de clôture) + recalcul du delta depuis. **Pas avant** que la lenteur se voie.

---

## 13. État connu du projet (à confirmer/mettre à jour)

- `gestion-stock` (projet frère) : infra + backend + CRUD Flutter faits ; Keycloak auth/RBAC et déploiement à finaliser.
- `mini_wms` : porte la gestion de flux. **Modèle du mouvement à revérifier dans le code** contre les règles §4 (généricité, quantité positive, emplacements virtuels, immutabilité, dates). Point de départ probable : faire évoluer un éventuel modèle « compteur » vers le modèle « mouvement ».

---

## Décisions actées — récap une ligne

- Stock dérivé des mouvements, jamais mutable.
- Quantité positive + emplacements (incl. virtuels) portent le sens.
- Variabilité bois dans `product`/`material`/`lot`, pas dans le flux.
- Densité sur `material`, mesure réelle sur `lot`, cascade de poids.
- Caisse = product + package ; contenu dérivé des mouvements.
- OF/réception génèrent des mouvements, ne touchent pas au stock.
- Multi-tenant mode SCHEMA, tenant depuis JWT, résolveur unique.
- JVM en dev/petit, natif pour la densité, extensions officielles only.
- Railway + Dockerfile ; CI GitHub Actions (tests natifs inclus).
- **Deux offres empilées : FLUX (socle) / FLUX+GPAO (premium option)** ; « GPAO seule » impossible ; le flux tourne **complet SANS aucun OF**.
- **Droit ≠ paramétrage** : droit (vendeur/superadmin, `public.tenant_features`, TRIAL/ACTIVE + `expires_at` → expiration auto, données conservées) = le **licensing** du produit ; paramétrage (admin client, son schéma) n'affiche que ce que le droit autorise.
- **Stock ≠ étape du flux** : réservoir **dérivé** ; **allocation = mouvement `DRAFT`** (dispo = physique − réservé) ; contrôle réception = différenciateur bois.
- **Nommage : code métier en FRANÇAIS** (Mouvement/Article/Emplacement/Reception/Colis/OrdreFabrication/Unite/Materiau/Nomenclature) — jamais FR/EN mélangés.
- **Snapshot documents** : un document validé (Reception, CommandeAchat…) **fige** les infos du tiers (libellé + légal) au moment T, EN PLUS de la FK → renommer un tiers ne réécrit pas l'historique. Référentiels au présent, documents figés (§6septies).
- **Formulaires paramétrables par client** : modèle **FIXE** + `config_champ` (présentation, schéma client) + rendu **générique** ; champs libres en **`champs_custom JSONB`** (jamais de colonnes/tables par client) ; règle de promotion JSONB→colonne (§6octies).
- (Détail complet : `docs/conception-plateforme.md` §6quinquies → §6octies.)
