# 🏭 Conception — Plateforme (nom de travail : **Fluxo**)

> **Doc VIVANT** — synthèse de la vision, du périmètre et du modèle. Grossit au fil des idées.
> Pas un CDC formel : un **support de conception** (mémoire externe + spec + atout portfolio).
> ⚠️ **Projet EN PAUSE** (focus actuel = certif Flutter + backend Quarkus). Ici on **capture/conçoit**, on ne code pas encore.

---

## 0. En une phrase
Une **plateforme unique** qui gère le **flux industriel de bout en bout** — du **négoce** simple à la
**fabrication + ordonnancement**, avec un **peu d'IA** — pour **industriels ET distributeurs**.

## 1. Vision & positionnement
- Réunir **GPAO** (production) + **Flux/WMS** (stock, emplacements, expédition) + **IA** (prévisions, ordonnancement).
- **Cohérent** : produire et expédier = le **même process**. L'appli GPAO Uniface reliait déjà « caisse → WMS ».
- **Différenciateur** : ~20 ans d'expertise métier (GPAO caisserie, WMS, tracking) → une **crédibilité que peu de devs ont**.
- **Marché élargi** : fabricants **+** négociants/distributeurs (même produit, plus de prospects).
- **Origine** : modernisation de l'appli GPAO **Uniface** (legacy) de l'auteur → **récit de migration** fort.

## 2. Concept central : l'**Ordre (flux)**
```
Ordre (flux)                       ← abstraction parente
 ├── OF Fabrication   (gamme + opérations + ordonnancement) → produit des Caisses (physiques)
 └── Ordre Négoce     (appro → vente, sans opérations)      → Articles / Prestations
```
- **Négoce = OF dégénéré** (sans gamme).
- **Colis** = **unité expédiable de 1er plan**, **découplée de la Caisse** : contient soit des **caisses**
  (fabrication) soit des **articles/prestations** (négoce).
- 🔧 *Legacy* : le négoce était géré par une **caisse virtuelle invisible + ligne de prestation** (hack, car
  dans l'Uniface tout passait par une caisse). *Moderne* : abstraction propre `Ordre` + `Colis` découplé.

## 3. Modules
| Module | Rôle |
|---|---|
| ⚙️ **Catalogue / BET** | config paramétrable (0 code) : modèles de caisses, **formules**, nomenclatures, variables |
| 💶 **Commercial** | tarification, **devis versionnés**, valorisation |
| 🏭 **Production / GPAO** | OF, gammes, **ordonnancement**, statuts caisse (ENC→FAB→EMB→EXP) |
| 📦 **Flux / WMS** | articles, **emplacements**, mouvements, réception, colisage, expédition, BL |
| 🤖 **Service IA** | **prévisions** (besoins matière) + **ordonnancement** (optimisation) |
| 📊 **Dashboard** | page d'accueil à **widgets** (1 widget = 1 module/fonctionnalité) |
| 🔌 **Intégrations** | **Sage X3** (facturation, sortant) ; WMS externe si séparé |

## 4. Briques métier clés (héritées du legacy, à moderniser)
- **Catalogue BET data-driven** : modèles (T16, T15 Chlorid…) + **formules** (moteur d'expressions) +
  **nomenclature hiérarchique** (`PLT/COT/COUV → planches/traverses → attributs EPAIS/LONG_INT/LONG_EXT`) +
  **variables/options** (`COUV_CADRE`, `CORNIERE`…) utilisées en conditions.
- **Configurateur** : l'opérateur saisit **poids + dimensions** → **fiche de débit** (liste de coupe).
- **Prévisions bois (MRP)** : besoins matière déduits du débit → appro/achats.
- **Tarification** : portée (**client / type / code**) × base (**m² / poids / volume / unité / surface**),
  choisie au chiffrage, avec **prix exceptionnel par lien (client)** prioritaire.
- **Devis → versions → 1 « mise en production »** → fabrication (aussi possible **sans devis**, direct).
- **Éditions** : OF, fiche débit, **fiche marquage**, devis, valorisation, **colisage**, **BL**.

## 5. IA — la bonne techno pour le bon besoin
| Besoin | Techno | LLM ? |
|---|---|---|
| **Prévisions** (besoins matière/réappro) | ML séries temporelles (Prophet / scikit-learn) | ❌ |
| **Ordonnancement** (séquencer, capacité finie) | optimisation (OR-Tools) + dispatching EDD | ❌ |
| **Langage naturel** (questions au système, expliquer une prévision) | **LLM (Claude)** | ✅ (option) |

## 6. Modèle de données
- **Ordonnancement** : voir [`ordonnancement-gpao-model.java`](ordonnancement-gpao-model.java)
  (PosteDeTravail + calendrier, Article/gamme, OF, OperationOF, Indisponibilite, PlanOrdonnancement…).
- **WMS / flux** : voir [`migration-wms-scoping.md`](migration-wms-scoping.md) (Emplacement, Stock, Mouvement, Réception…).
- **Tarification** : voir [`tarification-model.java`](tarification-model.java) — domaine
  `com.fluxo.commercial` (4ᵉ module de core-metier) : `RegleTarification` (portée client/modèle/article ×
  base M²/poids/volume/unité + **prix exceptionnel/lien** prioritaire), résolution par priorité dans
  `TarificationService`. Prix catalogue d'un article = une règle (article, client=null) ; prix **figé**
  dans les lignes (devis/commande) au chiffrage. ⚠️ Argent = **BigDecimal**, jamais double.
- **Traçabilité (lot & n° série)** : voir [`tracabilite-model.java`](tracabilite-model.java) — mode de
  suivi porté par l'`Article` (`AUCUN`/`LOT`/`SERIE`), `Lot` / `UniteSerie`, `Mouvement` (journal),
  `LienGenealogie` (rappel ciblé).
- À étendre : `Ordre` (parent) → `OF Fabrication` / `Ordre Négoce` ; `Colis` (découplé de `Caisse`).

**🧵 Fil directeur — chaîne de traçabilité (le n° lot en colonne vertébrale) :**
```
Réception (lot fournisseur) → Stock (emplacement, lot) → OF (conso lot) → Colis → Client
```
Le **`Mouvement` estampille le lot** à chaque étape ; la **`LienGenealogie`** relie lot produit ← lots
consommés → permet le **rappel ciblé** (⬇️ descendante : « qui est touché par ce lot ? ») et « de quoi
c'est fait » (⬆️ ascendante). Le **n° de lot** est l'identifiant qui traverse toute la chaîne.

## 7. Récit de migration (Uniface → moderne) — **argument portfolio**
- **Legacy** : monolithe **Uniface** (GPAO **+** WMS même base), rustines (caisse virtuelle pour négoce),
  éditions Crystal Report, intégration Sage X3.
- **Moderne** : **Quarkus** (microservices) + **Angular/Flutter** (dashboard) + **PostgreSQL** + **IA**.
- **Gains** : abstraction propre (`Ordre`), `Colis` découplé, moteur de formules paramétrable, IA, WMS découplé.
- **Angle prospection** : « je **modernise** le legacy industriel » = compétence **rare et très demandée** (PME/ETI).

## 8. Architecture technique
- **Stack** : Quarkus 3.33 + Panache + PostgreSQL + Angular/Flutter + Docker + Keycloak (auth).
- **Build backend : GRADLE 9.3.1** (⚠️ décision CHANGÉE le 2026-07-05 — remplace Maven) : l'utilisateur
  veut s'aligner sur l'outillage de son travail (même argument « compter double » que Dio). Gradle 9.3.1 =
  version officiellement recommandée pour Quarkus 3.33 ; Quarkus épinglé en **3.33.2.1** (dernière
  maintenance LTS). Syntaxe Maven↔Gradle : MEMO-CODE §6. (gs et mp restent en Maven.)
- **📐 RÈGLE D'ALIGNEMENT (2026-07-05, affinée le soir)** : la plateforme prend l'outil du boulot (GCA)
  quand ça **comble un manque** (ce que le boulot ne me fait PAS pratiquer moi-même : Gradle, Liquibase).
  Quand je pratique déjà l'outil au boulot au quotidien → la maison prend **le standard communautaire**
  (couvrir les deux > doubler le même).
- **Client HTTP Flutter : DIO** (décision FINALE 2026-07-05, après aller-retour Dio→Chopper→Dio) :
  je pratique déjà Chopper tous les jours au boulot → Dio à la maison = je couvre les deux ; Dio = standard
  communautaire (code vitrine lisible par tout dev), zéro friction build_runner, intercepteurs Keycloak OK.
  Révisable au moment d'attaquer le mobile (loin). `http` reste OK pour gs (fini).
- **Migrations de schéma : LIQUIBASE** (décidé 2026-07-05, à confirmer vs le boulot — mot entendu au
  travail) : changelogs versionnés (master XML + changesets en SQL formaté), rollbacks déclarés,
  `quarkus-liquibase`. Hibernate passe en `validate` (ne touche plus au schéma). Le standard entreprise
  (vs Flyway plus minimaliste).
- **API : DTO systématiques** (records Java) — jamais d'entités brutes exposées (pattern validé dans gs,
  cf. refactor commandes du 2026-07-04 : contrat JSON explicite + supprime les pièges lazy-loading).

### Organisation des services (décidée le 2026-07-04)
**3 microservices seulement** au départ :
| Service | Techno | Rôle |
|---|---|---|
| **gateway** | Quarkus | point d'entrée unique (web+mobile), auth Keycloak, agrégation dashboard |
| **core-metier** | Quarkus | ⭐ 90 % du code — modules flux/WMS + GPAO + référentiel, **1 PostgreSQL** |
| **service-ia** | Python | prévisions (Prophet/scikit-learn) + ordonnancement (OR-Tools) — **sans base** (stateless) |

Communication : REST partout (front→gateway→services ; core-metier→IA). Événements (Kafka…) = plus tard si besoin.

### ⭐ La règle des 3 niveaux (domaine vs fonctionnalité)
> **Domaine = qui POSSÈDE les données** → frontière de microservice possible.
> **Fonctionnalité = qui UTILISE les données** → simple package/répertoire à l'intérieur.
> Si deux « services » auraient besoin des **mêmes tables** → c'est **UN** service. (anti-pattern évité : nanoservices / monolithe distribué)

```
NIVEAU 1 — microservices (frontière réseau)   : gateway │ core-metier │ service-ia
NIVEAU 2 — modules de domaine (core-metier)   : com.fluxo.flux │ com.fluxo.gpao │ com.fluxo.referentiel
NIVEAU 3 — fonctionnalités (packages/module)  : reception/ │ expedition/ │ nonconformite/ │ inventaire/ …
```

```
📦 core-metier                    ← 1 projet Quarkus, 1 déploiement
 ├── com.fluxo.flux              ← domaine WMS
 │    ├── domain/  (Stock, Mouvement, Lot, Emplacement — entités PARTAGÉES du domaine)
 │    ├── reception/  expedition/  nonconformite/  inventaire/   ← fonctionnalités
 ├── com.fluxo.gpao              ← domaine GPAO
 │    ├── domain/  modeles/  of/  debit/
 └── com.fluxo.referentiel       ← article, client, site
```
**Monolithe modulaire** : les modules ne se parlent que par **interfaces de service** (jamais l'entité de
l'autre en direct) → un module peut être **sorti en vrai microservice plus tard** sans réécriture.
1 schéma PostgreSQL par module (`flux`, `gpao`, `ref`) pour préparer une éventuelle séparation.

## 8bis. « Quoi d'autre ? » — modules & aspects à prévoir (vision complète)

> Voir grand ici, **démarrer petit** (cf. §10). Beaucoup existent déjà dans le legacy Uniface (l'auteur connaît le métier).

**🧩 Modules métier**
- **Achats / Appro** — pendant AMONT des prévisions : prévision → **commande fournisseur** → réception (boucle avec le flux entrant).
- **Clients (CRM léger)** — fiches, conditions tarifaires, contacts (implicite dans la tarif « par client / par lien »).
- **Traçabilité / lots / n° série** — ⭐ passé **aéro (Safran/LSO)** : « où est telle pièce », lot, péremption. Fort différenciateur (secteurs régulés).
- **Qualité / non-conformités** — contrôle, rebuts, NCR.
- **Inventaire** — comptages / inventaire tournant (WMS).
- **Saisie atelier temps réel** — déclarer l'avancement FAB → **réel vs planifié** → **recale l'ordonnancement**.

**⚙️ Aspects transverses (incontournables)**
- **Sécurité & rôles** — BET / opérateur / commercial / atelier / admin → **Keycloak**.
- **Multi-sites** — le legacy l'était (options par site) → prévoir `Site` dès le modèle.
- **Mobile atelier/logistique** — **scan** + saisie terrain → place naturelle pour les compétences **Flutter** (relie la certif au projet).
- **Migration des données legacy** — catalogue, tarifs, historique depuis Uniface/SQL Server (souvent le plus gros chantier réel).
- **Reporting / KPI** — OTD (respect délais), **taux de charge des postes**, retards, valorisation stock.

**🧩 Champs personnalisés par client** (besoin vu au boulot de l'utilisateur, 2026-07-05) : une plateforme
installée chez des clients finit toujours par devoir accepter des **champs optionnels définis par le
client** (ex. « n° de four » sur Article chez X, « code douane » chez Y). Réponse retenue : **colonne JSONB**
(`attributs_personnalises`) sur les entités concernées — flexible SANS sacrifier ACID/jointures (pas besoin
de Mongo). Côté mobile : Chopper + convertisseurs manuels gèrent bien ces champs dynamiques (c'est
précisément pour ça que le boulot a quitté le codegen OpenAPI : `additionalProperties` mal généré).

**🧩 Visibilité des champs configurable par client** (idée utilisateur, 2026-07-05 — depuis le marché 😄) :
écrans **pilotés par configuration** — une table `ConfigurationChamp` (entité, champ, **visible**,
obligatoire, libellé personnalisé) par installation/client ; le front construit ses formulaires en la
lisant. Se marie avec les champs JSONB (la même table déclare les champs custom) et avec la cascade
global→site→rôle (comme les tarifs du legacy). Dosage : v1 = drapeaux visible/obligatoire sur les champs
standards ; v2 = dictionnaire complet de champs dynamiques.

**🚫 Délégué (NE pas construire)** — **Facturation** → reste dans **Sage X3** (juste l'interface).

**📥 Sources d'inspiration à fournir par l'auteur** : 2 applis WMS legacy (réception/flux) + le **code Uniface GPAO**.

## 9. Nom (à trancher plus tard)
Candidats : **Fluxo** ⭐, Optiflux, Locus, Traxo, Célérix, StockFlow, Novaflux.
À vérifier avant de choisir : **domaine .fr/.com** + **INPI** (marques).

## 10. ⚠️ Discipline de périmètre (LE plus important)
- **Ne PAS tout construire.** Démarrer par **UNE tranche démo qui TOURNE** (ex. **ordonnancement + Gantt**,
  ou le **moteur de formules GPAO**) → 5 min de démo qui font « waouh ».
- **Démo qui marche > CDC parfait.** Ne pas tomber dans le piège du doc infini.
- Ce doc reste **léger et vivant** ; il grossit **au fil des idées réelles**, pas d'un gros effort initial.

### 🏁 Première tranche — ordre de build (validé 2026-07-03)
> ⚠️ **NE PAS commencer par le dashboard configurable** (= construire le cadre avant le tableau = coquille vide).
> Commencer par un **flux qui marche de bout en bout**.

1. **Paramétrage : `Article`** (avec `ModeTracabilite` AUCUN/LOT/SERIE → le n° lot est **optionnel**) **+ `Emplacement`** — CRUD simple, les fondations.
2. **`Réception`** — le 1er vrai flux : réceptionner un article (n° lot si activé) à un emplacement → **crée/incrémente `Stock` + un `Mouvement`**. Démontrable tout de suite, exerce le cœur (stock/lot/traçabilité).
3. **Dashboard** = simple menu de liens d'abord ; « widgets configurables par l'admin » = **v2**.

**Alignement** : Réception (backend) = **Quarkus** (focus) ; l'écran = un peu de **Flutter** (certif). La **plateforme devient le projet backend principal** ; gestion-stock s'y fond. Point de reprise probable : branche `feature/backend-emplacement` (entité `Emplacement` déjà amorcée).
