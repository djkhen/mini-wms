# 📦 TODO / Idées — mini-wms

> Bac à idées du projet. Capturé en vrac, rangé ensuite. Projet **en pause**
> (focus actuel = certif Flutter + gestion-stock) → idées à reprendre plus tard.
>
> 🔻 **Deux étages** : d'abord le **reste-à-faire actionnable** (tâches concrètes, court terme) ;
> ensuite le **bac à idées / vision** (plus loin, à mûrir).

## 🔧 Reste-à-faire actionnable (à jour 2026-07-25)

### ▶️ En cours
- [ ] **PATCH Emplacement** — sur `EmplacementResource` : renommer le PUT `modifier` → `modifierComplet`
      **+** ajouter `modifierPartiel` (PATCH, `readerForUpdating`), sur le modèle d'`ArticleResource`.
      Recette : [NOTES-DEV.md](NOTES-DEV.md) (entrée 2026-07-25).

### ⏭️ Prochaines (référentiel Article terminé)
- [ ] **Merger** `feature/referentiel-article` → `main` (Article CRUD + PUT/PATCH fini et testé).
- [ ] **Seed `ArticleDataInitializer`** — démos couvrant les 3 traçabilités (AUCUN/LOT/SERIE) + 1 inactif
      (dev uniquement, désactivé en prod via profil Quarkus).
- [ ] **Migration 003 — champ `family`** sur `Article` (SIMPLE|BOIS|PANNEAU|CAISSE), cf. §6ter conception.

### ⭐ Grosse brique — NOYAU FLUX (stock dérivé)
- [ ] **`stock_move`** (journal immuable, `quantity`>0, sens par emplacements virtuels) — **PAS** de table
      Stock-compteur ; le stock devient une **vue d'agrégation**. Design figé : [conception §6ter](docs/conception-plateforme.md).
- [ ] **Référentiel UoM** — tables `uom` + `uom_category` (+ `packaging` lié à l'article) et le `stock_uom`
      pivot sur `Article`. Modèle nailé : [conception §6quater](docs/conception-plateforme.md).
- [ ] **Réception** (`reception` + `reception_line`) — 1er vrai flux : saisie en unité d'achat →
      **conversion** vers `stock_uom` → génère un `stock_move`. (Suite logique après `stock_move`.)

### 🧹 Nettoyages / dette (petits, à caser)
- [ ] **`EmplacementResource` expose l'entité brute** (pas de DTO) → aligner sur le pattern `ArticleDto`
      (dette `EmplacementDto` WIP, évite les pièges lazy-loading).
- [ ] **Scaffold `mobile/`** à committer proprement (`chore: scaffold mobile`) ; retirer les stubs vides
      (`mobile/lib/generated/assets.dart`).
- [ ] **Javadoc fantôme** dans `Emplacement.findByCode` à nettoyer.

### 🚀 Ops / infra (quand le socle tourne)
- [ ] **CI GitHub Actions** `.github/workflows/build.yml` — `./gradlew build` + tests ; plus tard build+tests
      **natifs**. Cf. [ARCHI-DEPLOY §6](docs/ARCHI-DEPLOY.md).
- [ ] **Multi-tenant** — `TenantResolver` unique (tenant depuis JWT, 401 si absent) + trancher le
      **mécanisme de migration par schéma** (app Quarkus vs CLI Liquibase). Point ouvert : [ARCHI-DEPLOY §3](docs/ARCHI-DEPLOY.md).

### 📄 Docs (décision à froid)
- [ ] **Réaligner le README sur Fluxo** — il décrit encore un « mini-WMS générique » (pré-pivot plateforme).
      Décision de com' : jusqu'où afficher « plateforme » vs profil bas (repo issu du legacy employeur).

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
