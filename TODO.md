# 📦 TODO / Idées — mini-wms

> Bac à idées du projet. Capturé en vrac, rangé ensuite. Projet **en pause**
> (focus actuel = certif Flutter + gestion-stock) → idées à reprendre plus tard.
>
> 🔻 **Deux étages** : d'abord le **reste-à-faire actionnable** (tâches concrètes, court terme) ;
> ensuite le **bac à idées / vision** (plus loin, à mûrir).

## 🔧 Reste-à-faire actionnable (à jour 2026-07-27)

### ✅ FAIT — session 2026-07-27 (front Flutter ; branches mergées dans `main`)
- [x] **Écran Emplacements** (liste mobile, `GET /emplacements`).
- [x] **Menu** de navigation (`NavigationRail`) — Accueil / Articles / Emplacements, construit depuis une **liste de données** `_sections`.
- [x] **Écran détail Article** (`Navigator.push`/`pop` — fiche complète + flèche retour).
- [x] **Menu ADAPTATIF** (`NavigationRail` desktop / `Drawer` mobile via `LayoutBuilder`, seuil 600 px) ; pages passées en « contenu pur » (coquille = AppBar+menu).
- [x] **Support Android** (URL `10.0.2.2` selon plateforme + cleartext debug) → Fluxo tourne sur **web + Android**.
- [x] 🕵️ Plugin **FlutterAssetsGenerator DÉSACTIVÉ** (il corrompait `pubspec` + créait `assets.dart`) ; `lib/generated/` gitignoré + exclu de l'analyse.

### ▶️ PROCHAINE SESSION — au choix (le front couvre déjà menu adaptatif + listes + détail)
> Point de reprise. Backend complet + appli Flutter (web+Android). Options :
1. [ ] **Détail Emplacement** — même patron que le détail Article → à **coder soi-même** (bon exercice certif).
2. [ ] **GetIt** (refactor) — UN seul `Dio`/`ApiService` partagé + splitter en `ArticleService`/`EmplacementService`
       (l'`ApiService()` recréé dans chaque page commence à se répéter). Cf. roadmap §GetIt ci-dessous.
3. [ ] **Backend — pivot flux** : `stock_move` (stock dérivé) OU migration 003 `family` sur Article (§6ter conception).
4. [ ] 🧹 **Ménage branches** : supprimer les vieilles branches déjà mergées (mobile-init-article-list,
       -emplacement-list, -menu, detail-article, menu-adaptatif, referentiel-article, finition-emplacement, article-seed…).

### 🏗️ Architecture Flutter à venir — dans l'ordre (chaque notion QUAND le besoin arrive, pas avant)
> Feuille de route mobile. Principe **YAGNI** : on introduit chaque brique quand la **douleur** apparaît.
1. [x] ~~**Routes / Navigation**~~ ✅ FAIT (2026-07-27) : menu `NavigationRail`/`Drawer` **adaptatif** + `Navigator.push`/`pop`
       (écran détail). `go_router` plus tard, si le routing se complexifie (deep links, URL web propres…).
   - 📐 **Structure du menu (idée user 2026-07-27) — à appliquer quand le menu grossit** : le menu = une
     **liste de GROUPES**, chacun avec ses sous-écrans → 1er niveau petit et scalable. **Grouper par domaine,
     en miroir des packages backend** : **Paramétrage** (Article, Emplacement, Unités, Fournisseurs — cf. les
     packages `*.parametrage`) · **Flux** (Réception, Expédition, Inventaire) · **Production/GPAO** (OF, Gammes)
     · **Dashboard** · **IA**. Le **même modèle de données** alimente `NavigationRail` (desktop) **ET** `Drawer`
     (mobile) → **adaptatif** selon la largeur d'écran, sans réécriture. ⚠️ Le mobile garde **TOUT** (juste
     présenté autrement). Aujourd'hui : menu **plat** (2 écrans) mais **structuré en données** pour évoluer vers ça.
2. [ ] **GetIt** (service locator / injection de dépendances) — avoir **UN seul** `ApiService` partagé au
       lieu d'en recréer un dans chaque page. **Déclencheur** : la répétition `ApiService()` devient agaçante (2-3 écrans).
3. [ ] **State management — Provider OU Riverpod** (à trancher le moment venu) — partager un état entre
       écrans, cache, chargement, token Keycloak. **Déclencheur** : un `FutureBuilder` par écran ne suffit plus.
       Reco pressentie : **Riverpod** (standard moderne, testable) ; Provider = plus simple à apprendre d'abord.

### ✅ Finition Emplacement (backend) — FAIT le 2026-07-25 (branche `feature/finition-emplacement`)
- [x] **CRUD en DTO** (`EmplacementDto` + mapper `de()`) — l'API n'expose plus l'entité brute.
- [x] **Filtre `?code=`** sur la liste (lookup par identifiant métier, cohérent avec Article).
- [x] **Javadoc fantôme** nettoyé dans `Emplacement.findByCode`.
- [ ] **PATCH Emplacement** — ⏸️ **REPORTÉ (YAGNI)** : le PUT suffit. À n'ajouter **que si** un bouton
      « désactiver un emplacement » apparaît (alors : rename `modifier`→`modifierComplet` + `modifierPartiel`
      via `readerForUpdating`, recette [NOTES-DEV.md](NOTES-DEV.md) 2026-07-25).

### ⏭️ Prochaines (référentiel Article terminé)
- [x] ~~**Merger** `feature/referentiel-article` → `main`~~ ✅ FAIT le 2026-07-25 (merge `--no-ff`, poussé).
      Article (CRUD + PUT/PATCH) est désormais officiel sur `main`.
- [x] ~~**Seed `ArticleDataInitializer`**~~ ✅ FAIT le 2026-07-26 (branche `feature/article-seed`) — 6 articles
      démo (3 traçabilités AUCUN/LOT/SERIE + 1 inactif), thème bois. Filtres `?tracabilite=`/`?actif=` testés.
      ⚠️ à désactiver en prod via profil Quarkus.
- [ ] **Migration 003 — champ `family`** sur `Article` (SIMPLE|BOIS|PANNEAU|CAISSE), cf. §6ter conception.

### ⭐ Grosse brique — NOYAU FLUX (stock dérivé)
- [ ] **`stock_move`** (journal immuable, `quantity`>0, sens par emplacements virtuels) — **PAS** de table
      Stock-compteur ; le stock devient une **vue d'agrégation**. Design figé : [conception §6ter](docs/conception-plateforme.md).
- [ ] **Référentiel UoM** — tables `uom` + `uom_category` (+ `packaging` lié à l'article) et le `stock_uom`
      pivot sur `Article`. Modèle nailé : [conception §6quater](docs/conception-plateforme.md).
- [ ] **Réception** (`reception` + `reception_line`) — 1er vrai flux : saisie en unité d'achat →
      **conversion** vers `stock_uom` → génère un `stock_move`. (Suite logique après `stock_move`.)
  - 📋 **En-tête réaliste (œil métier user, 2026-07)** : **Fournisseur** · **N° de commande** (le bon de commande
    que la réception SOLDE → contrôle « reçu vs commandé », relie au futur module **Achats**) · **Transporteur**
    (qui a livré) · **N° BL** fournisseur · date · statut (attendue / en cours / terminée). ⚠️ v1 = champs **SIMPLES**
    (texte / petit référentiel) ; « N° commande » → deviendra un **LIEN** vers l'entité `Commande` quand on fera le module Achats.
  - 🏭 **Prérequis : entité `Fournisseur`** (référentiel simple, CRUD comme Article/Emplacement — bon exercice solo).
- [ ] **Emplacement → modèle `location` du schéma** (à faire AVEC `stock_move`) — enrichir l'entité :
      ajouter le `type` **VIRTUEL** (INTERNE | FOURNISSEUR | CLIENT | PRODUCTION | PERTE, **indispensable**
      au stock dérivé : achat = `FOURNISSEUR→INTERNE`, vente = `INTERNE→CLIENT`, casse = `INTERNE→PERTE`)
      **+ `parent_id`** (arborescence Entrepôt→Zone→case). ⚠️ **Garder les DEUX axes sans les confondre** :
      le `type` actuel = **rôle fonctionnel** (RECEPTION/STOCKAGE/…) ; le nouveau = **réel vs virtuel**.
      ⏸️ **Pas avant `stock_move`** (les emplacements virtuels ne servent à rien sans mouvements = travail
      dans le vide). Comparaison détaillée : entité actuelle vs table `location` de [`schema-bd-wms-gpao.html`](docs/schema-bd-wms-gpao.html).

### 🧹 Nettoyages / dette (petits, à caser)
- [ ] **Scaffold `mobile/`** à committer proprement (`chore: scaffold mobile`) ; retirer les stubs vides
      (`mobile/lib/generated/assets.dart`).

### 🚀 Ops / infra (quand le socle tourne)
- [ ] **CI GitHub Actions** `.github/workflows/build.yml` — `./gradlew build` + tests ; plus tard build+tests
      **natifs**. Cf. [ARCHI-DEPLOY §6](docs/ARCHI-DEPLOY.md).
- [ ] **Multi-tenant** — `TenantResolver` unique (tenant depuis JWT, 401 si absent) + trancher le
      **mécanisme de migration par schéma** (app Quarkus vs CLI Liquibase). Point ouvert : [ARCHI-DEPLOY §3](docs/ARCHI-DEPLOY.md).
- [ ] 💼 **LICENSING — table `public.tenant_features`** (⭐ le mécanisme « produit commercialisable ») : droits
      **FLUX / GPAO** par tenant, `status` TRIAL/ACTIVE + `expires_at` (démo J+30 / licence à durée / permanent) →
      **expiration auto, données conservées**. + page **superadmin** (le VENDEUR écrit les droits, le client JAMAIS)
      + **gate UI** (GPAO non souscrite → section masquée = hook de vente). ⚠️ **Droit** (schéma `public`, vendeur)
      **≠ paramétrage** (schéma client, admin). Détail : [conception §6quinquies](docs/conception-plateforme.md).
- [ ] 💼 **Offres FLUX / FLUX+GPAO** — FLUX = socle vendable (marché large PME) ; GPAO = étage premium **optionnel**
      activé par le droit. « GPAO seule » impossible. ⚠️ Discipline : le flux tourne **complet SANS aucun OF** (jamais
      de champ obligatoire lié à un OF sur un `Mouvement` ; `Nomenclature`/`OrdreFabrication` vides pour un client flux-seul).
- [ ] 🇫🇷 **Nommage code métier en FRANÇAIS** (déjà le cas : Article/Emplacement) — traduire les noms EN du schéma :
      `stock_move`→Mouvement, `product`→Article, `package`→Colis, `uom`→Unite, `work_order`→OrdreFabrication, etc. Jamais FR/EN mélangés.

### 📄 Docs (décision à froid)
- [ ] **Réaligner le README sur Fluxo** — il décrit encore un « mini-WMS générique » (pré-pivot plateforme).
      Décision de com' : jusqu'où afficher « plateforme » vs profil bas (repo issu du legacy employeur).

---

## ⚖️ Décisions & arbitrages (ce qui est retenu, ce qui est écarté, et son coût)

> Registre honnête : pour chaque choix, l'**alternative écartée** + l'**avantage** ET l'**inconvénient**
> assumé de ce qu'on a fait. Un choix sans inconvénient connu = un choix pas encore compris.

### 1. Migrations — **Liquibase** _(écarté : Flyway)_
- ✅ Standard entreprise ; rollback déclaré ; schéma = code versionné (master XML + changesets) ; comble un manque non pratiqué au boulot.
- ⚠️ Plus verbeux que Flyway (XML + SQL formaté) ; checksum strict (un changeset joué est immuable → toute correction = nouveau fichier).

### 2. Build backend — **Gradle 9.3.1** _(écarté : Maven)_
- ✅ Aligné sur l'outillage du boulot (GCA) ; build incrémental rapide ; flexible.
- ⚠️ Syntaxe moins universelle que le POM ; gs/mp restent en Maven → deux outils à maintenir dans l'écosystème perso.

### 3. Client HTTP Flutter — **Dio** _(écarté à la maison : Chopper, déjà pratiqué au boulot)_
- ✅ Standard communautaire, code vitrine lisible ; zéro `build_runner` ; intercepteurs Keycloak simples ; couvre l'outil que le boulot ne me fait pas pratiquer.
- ⚠️ Pas de codegen typé (contrats écrits main) ; ne réutilise pas le réflexe Chopper quotidien.

### 4. Multi-tenant — **1 schéma Postgres/client** _(écarté : `tenant_id` ; en réserve : base/client, silo)_
- ✅ Isolation logique forte sans le poids d'une base par client ; SCHEMA↔DATABASE réversible sans refactor ; pas de `WHERE tenant` à oublier.
- ⚠️ Les migrations doivent passer sur **tous** les schémas ; pas l'isolation **physique** d'un silo ; un bug du `TenantResolver` = fuite potentielle (→ garde-fous obligatoires).

### 5. Stock — **dérivé des mouvements** _(écarté : table `Stock(quantité)` compteur)_
- ✅ Journal auditable ; stock à n'importe quelle date passée gratuit ; physique vs prévisionnel ; jamais de désync compteur.
- ⚠️ Coût de calcul (Σ) qui grossit → index puis snapshot à grande échelle ; plus abstrait qu'un compteur (courbe de compréhension).

### 6. API — **DTO systématiques** _(écarté : exposer l'entité brute)_
- ✅ Contrat JSON explicite ; supprime les pièges lazy-loading (500) ; découple modèle interne/externe.
- ⚠️ Boilerplate (record + mapper `de()`) par entité ; risque de désync DTO↔entité (ex. bug designation/description inversé rencontré).

### 7. PATCH — **`readerForUpdating`** _(écarté : `@JsonSetter(nulls = Nulls.SKIP)`)_
- ✅ Décision **locale** au PATCH (le PUT garde « remplace tout ») ; distingue *absent* de *null* → peut vider un champ volontairement.
- ⚠️ Exige de charger l'entité d'abord + `ObjectMapper` injecté ; un `null` explicite **écrase** → valider **après** la fusion.

### 8. UoM — **1 pivot `stock_uom`/article + conversions au référentiel** _(écarté : stock en unités multiples)_
- ✅ La somme des mouvements reste une addition bête (zéro conversion à la lecture) ; conversions définies une fois, réutilisées.
- ⚠️ Une conversion manquante bloque une saisie ; le pivot mal choisi se paie cher (re-choisir = re-convertir l'historique).

### 9. Article bois — **une fiche par dimension** _(alternative ouverte : dimensions portées par le `lot`)_ ❓
- ✅ Simple, standard ; chaque référence = un article net et traçable.
- ⚠️ Prolifération d'articles si beaucoup de dimensions → **à réévaluer** pour le sur-mesure (question non tranchée, cf. CLAUDE.md).

### 10. Hébergement — **Railway** _(écarté pour l'instant : VPS + Docker + Caddy)_
- ✅ CD + HTTPS gratuits, zéro ops, build Dockerfile direct depuis GitHub.
- ⚠️ Moins de maîtrise et coût moins optimisé à l'échelle ; dépendance à la plateforme.

### 11. Archi — **monolithe modulaire (3 services)** _(écarté : nanoservices / full microservices d'emblée)_
- ✅ Simplicité de départ ; un module peut sortir en vrai microservice plus tard sans réécriture ; évite le monolithe distribué.
- ⚠️ Frontières de modules à discipliner (interfaces only) sinon couplage rampant ; pas la scalabilité indépendante dès le jour 1.

---

## 💡 Idée d'architecture (2026-07-03) — app unique + microservices + IA + dashboard

- [ ] **Une seule application, microservices séparés** (WMS-core + services dédiés).
      ⚠️ Garder **2-3 services max** au début — microservices = complexité (réseau, données, déploiement).
- [ ] **IA — prévisions** (réappro / besoins stock) → **microservice dédié**, ML séries temporelles
      (Prophet / scikit-learn en Python, ou lib Java). Données = historique des mouvements. Faisable.
- [ ] **IA — ordonnancement** (séquencer tâches / picking / slotting) → **optimisation** (OR-Tools /
      heuristiques), microservice séparé. NB : c'est de l'optimisation, pas un LLM.
- [ ] **(option) LLM (Claude)** pour le **langage naturel** : poser une question au WMS en français,
      expliquer une prévision. Distinct de la prévision numérique.
- [ ] **Page d'accueil = dashboard à widgets**, un **widget par fonctionnalité / microservice**
      (stock, réceptions, prévisions, ordonnancement…). Bonus : montre visuellement l'archi microservices.

**Scoping conseillé** : démarrer par **WMS-core + 1 service prévision + dashboard**, puis étendre.
Argument vitrine fort (IA + microservices + dashboard) pour la prospection industrie/GPAO.

## 🌟 VISION (2026-07-03) — une SEULE plateforme : GPAO + Flux/WMS + IA

Idée directrice de l'utilisateur : **réunir dans une seule application** son ancienne **GPAO**
(caisserie : conception/débit/devis) **+** la **gestion de flux / WMS** (emplacements/mouvements) **+** un
**peu d'IA** (prévisions / ordonnancement). Cohérent : produire et expédier = même process de bout en bout,
et l'appli GPAO Uniface reliait déjà « caisse → WMS ». **Récit vitrine très fort** = 20 ans de GPAO + job
actuel tracking/flux (GCA) + IA réunis en une plateforme moderne (cf. [[gpao-uniface-app]]).

- [ ] Archi : **plateforme unique, modules/microservices** — `Module GPAO` + `Module Flux/WMS` +
      `Service IA` + gateway + **dashboard à widgets** (1 widget = 1 module/fonctionnalité).
- [ ] ⚠️ **Ambitieux** → construire en **tranches**, pas tout d'un coup. Statut : **capture uniquement**
      (projet en pause, focus = certif Flutter + backend Quarkus).
- [ ] 💥 **GÉNÉRALISATION (idée user 2026-07-03) — négoce + fabrication + stock, un seul moteur** :
      un **négoce** (achat-revente sans fabriquer) = un **OF dégénéré** (article sans gamme/opérations).
      ⇒ **gestion-stock n'est pas un projet à part** : c'est la base « article + stock » partagée par les
      2 modes, qui **se dissout dans la plateforme**. **Marché élargi** : fabricants **ET** négociants/
      distributeurs (mêmes prospects potentiels, même produit). ⚠️ Design : prévoir une **abstraction
      commune** `Ordre (flux)` → variantes `OF Fabrication` (gamme+ordo) / `Ordre Négoce` (appro→vente),
      plutôt que forcer le négoce dans l'OF de prod.
- [ ] Nom : si l'appli couvre **production + flux**, le nom devra peut-être embrasser les deux (Fluxo =
      candidat parapluie, ou nom plus large à trouver).

## 📝 Idées reportées depuis gs (2026-07-04, scope discipline)

- [ ] **Référentiel Fournisseurs / Tiers** (module `referentiel`, déjà prévu par `Lot.fournisseur`) :
      dans gs le tiers est du texte libre (3 orthographes = 3 tiers) → ici : entité + CRUD + **dropdown**
      dans les commandes/réceptions. Idem probablement **Clients**.
- [ ] **Coquille de dialogue commune (`AppDialog`)** dès le jour 1 côté Flutter : titre + contenu +
      Retour factorisés (leçon des 4 dialogues quasi identiques de gs). NB : aussi bon exercice certif
      à faire dans gs un soir calme (composition de widgets, ~1h, risque zéro).
- [ ] **Vue « commandes par article » native** (déjà notée) — gs l'a maintenant en v1.1 ; ici le
      `Mouvement.reference` la rendra encore plus riche (OF, colis…).

## 🏷️ Nom du produit (idées, décision remise à plus tard — « on verra »)

- **Fluxo** ⭐ — évoque le *flux* (cœur métier), court, brandable
- **Optiflux** ⭐ — flux + optimisation → met en avant l'IA (prévisions/ordonnancement)
- **Locus** — latin « le lieu » = l'emplacement, cœur du WMS ; sobre/pro
- **Traxo / Traxen** — traçabilité + mouvement
- **Célérix** — de *celeris* (rapide), efficacité logistique
- **StockFlow** — descriptif, clair (un peu générique)
- **Novaflux** — « nouveau flux », côté modernisation

`mini-wms` reste le **nom de code du repo**. À faire avant de choisir : vérifier **domaine .fr/.com** + **INPI** (marques).
