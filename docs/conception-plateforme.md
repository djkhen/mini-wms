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

## 2. Concept central : la **Commande et ses LIGNES** *(révisé le 2026-08-07)*
```
CommandeClient   (l'affaire / le dossier)              ← objet COMMERCIAL
 │   client, dates, conditions, tarif → BL, facture
 ├── Ligne · article AVEC Nomenclature   → génère un OrdreFabrication (GPAO)
 ├── Ligne · article SANS Nomenclature   → NÉGOCE : allocation → expédition, AUCUN OF
 └── Ligne · prestation                  → ni stock ni OF (service facturé)
```
- ❌ **ABANDONNÉ : l'abstraction parente `Ordre`** avec 2 sous-types (`OF Fabrication` / `Ordre Négoce`).
  Une commande réelle **MÉLANGE les deux** (3 caisses fabriquées **+** 40 sangles revendues telles quelles) →
  un objet ne peut pas choisir son sous-type. **Le « type » appartient à la LIGNE, jamais à la commande** —
  et il ne se saisit même pas : il se **DÉDUIT** de l'article (avec ou sans `Nomenclature`).
- ❌ **« Négoce = OF dégénéré » est FAUX** (formule héritée du legacy). Un flux de négoce n'est pas une
  fabrication vide : c'est **une suite de mouvements SANS étape de production** — les étapes ⑤/⑥ du
  [parcours produit](parcours-produit-mini_wms.html) sont simplement **absentes**. Aucun objet fantôme.
- **L'OF DESCEND d'une ligne** (0 ou 1), il ne la commande pas. Chez un client **FLUX seul**, ce niveau reste
  vide — conforme à la discipline « le flux tourne complet SANS aucun OF » (§6quinquies A).
- ⭐ **L'unification se fait par le BAS** : quelle que soit sa nature, une ligne finit en `Mouvement` vers
  `CLIENT`. C'est le **journal** qui réconcilie tout, **pas un objet parent**.
- **Colis** = **unité expédiable de 1er plan**, **découplée de la Caisse** : contient soit des **caisses**
  (fabrication) soit des **articles/prestations** (négoce).
- ⏳ **`Affaire`/`Dossier`** (un cran AU-DESSUS de la commande : regrouper plusieurs devis/commandes) →
  **PAS maintenant** (YAGNI). À ajouter le jour où un client réel le demande ; sinon c'est un niveau vide que
  tout le monde doit traverser — le même piège que la caisse fantôme, dans l'autre sens.

**🔧 Récit legacy → moderne (⭐ argument portfolio)** : en Uniface, **tout passait par une caisse** → le négoce
était géré par une **caisse virtuelle invisible + ligne de prestation**. Ce n'était **pas absurde** : réutiliser
la mécanique existante (tarif, éditions, statuts) coûtait des jours là où dupliquer aurait coûté des mois — une
dette technique au sens propre, un emprunt. Mais les **intérêts** ont été payés : objets vides de sens en base,
formulaires pleins de champs sans objet, **vocabulaire faux** (« OF » pour une simple revente), et surtout
**impossibilité de vendre le flux sans la production**. Le diagnostic n'est pas « une aberration de langage »
mais une **ABSTRACTION MANQUANTE** — le vocabulaire n'a fait que suivre le modèle. ⭐ Fluxo supprime le problème
à la racine : **il n'y a plus d'objet racine du tout**, seulement des mouvements. Vendre sans fabriquer n'est
plus « un OF dégénéré », c'est **un mouvement de moins**.

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
- **WMS / flux** : voir [`migration-wms-scoping.md`](migration-wms-scoping.md) (Emplacement, Mouvement, Réception…).
  ⚠️ Ce doc parle encore d'une table `Stock` : **périmé** — le stock est **DÉRIVÉ** des mouvements (§6ter).
- **Tarification** : voir [`tarification-model.java`](tarification-model.java) — domaine
  `com.fluxo.commercial` (4ᵉ module de core-metier) : `RegleTarification` (portée client/modèle/article ×
  base M²/poids/volume/unité + **prix exceptionnel/lien** prioritaire), résolution par priorité dans
  `TarificationService`. Prix catalogue d'un article = une règle (article, client=null) ; prix **figé**
  dans les lignes (devis/commande) au chiffrage. ⚠️ Argent = **BigDecimal**, jamais double.
- **Traçabilité (lot & n° série)** : voir [`tracabilite-model.java`](tracabilite-model.java) — mode de
  suivi porté par l'`Article` (`AUCUN`/`LOT`/`SERIE`), `Lot` / `UniteSerie`, `Mouvement` (journal),
  `LienGenealogie` (rappel ciblé).
- À étendre : `CommandeClient` + `LigneCommande` (lignes **hétérogènes**, cf. §2) ; `Colis` (découplé de `Caisse`).

**🧵 Fil directeur — chaîne de traçabilité (le n° lot en colonne vertébrale) :**
```
Réception (lot fournisseur) → Stock (emplacement, lot) → OF (conso lot) → Colis → Client
```
Le **`Mouvement` estampille le lot** à chaque étape ; la **`LienGenealogie`** relie lot produit ← lots
consommés → permet le **rappel ciblé** (⬇️ descendante : « qui est touché par ce lot ? ») et « de quoi
c'est fait » (⬆️ ascendante). Le **n° de lot** est l'identifiant qui traverse toute la chaîne.

## 6ter. ⭐ NOYAU FLUX — stock DÉRIVÉ des mouvements (adopté 2026-07, schéma Odoo-like)

Source : [`schema-bd-mini_wms-v2.html`](schema-bd-mini_wms-v2.html) — ⭐ **v2 (nommage FRANÇAIS + tables `tenant`/`tenant_feature`)**,
supersède la v1 anglaise ; issu d'une session Odoo de l'utilisateur,
retenu comme **socle** car il réalise SA philosophie « le stock se **CALCULE**, pas un compteur »).

> 🔤 **Terminologie figée : Produit = Article = product** — MÊME concept (une fiche référentiel). Fluxo dit
> « **Article** » ; c'est le `product` du schéma. Plus jamais d'hésitation.

**Principe** : le stock n'est **jamais stocké** → il est **DÉRIVÉ** de `stock_move` (Σ entrées − sorties DONE).
⚠️ Notre ancienne table `Stock(quantité)` = un **COMPTEUR** = l'anti-pattern → **ABANDONNÉE**, remplacée par
une **vue/requête**. (Idem le CONTENU d'une caisse : dérivé de la Σ des mouvements du package.)

**Le cœur — `stock_move`** (journal **immuable**) : article, lot, `quantity` (**>0 toujours**), uom,
`source_location→dest_location`, `source_package→dest_package`, `state` DRAFT|DONE|CANCELLED, origin_type/id.
Le **SENS vient des emplacements** (dont **VIRTUELS** : `FOURNISSEUR`, `CLIENT`, `PRODUCTION`, `PERTE`),
**jamais** d'un signe ni d'un type ENTRÉE/SORTIE en dur → **UNE seule mécanique** pour
achat / vente / production / casse / mise en caisse.

**Entités du noyau** : `material` (densité) · `uom` · `location` (type + parent, dont virtuels) ·
**`article`** (⭐ champ **`family`** SIMPLE|BOIS|PANNEAU|CAISSE, dimensions, `tracking` NONE|LOT|SERIAL,
`is_container` + payload_max/tare/inner) · `lot` · `bom`+`bom_line` · `work_order` (OF) ·
`package` (la caisse-**contenant** : article+lot série, `parent_package` imbriqué) ·
`reception`+`reception_line` · `stock_move`.

**7 règles d'or** : (1) une seule mécanique (mouvements) ; (2) stock dérivé ; (3) contenu caisse dérivé ;
(4) **poids en cascade** (pesé lot → densité×humidité mesurée → densité nominale `material`) ; (5) charge :
Σ contenu ≤ `payload_max_kg` (→ bon de transport) ; (6) **immuable** (jamais éditer un move validé →
mouvement inverse, journal auditable) ; (7) **multi-tenant = 1 schéma Postgres/client** (pas de `tenant_id`).

**Négoce vs fabrication, unifié ici** : un article de négoce = un `article` **sans `bom`** (reçu → vendu :
`FOURNISSEUR→INTERNE→CLIENT`) ; une caisse = un `article` **avec `bom`** (fabriqué via `work_order`). Même table.

**FUSION avec le reste du CDC** : ce noyau = le **socle FLUX/PRODUCTION**. On **empile** par-dessus nos
couches (commercial/tarif, devis, ordonnancement, moteur de formules GPAO/débit + CODE SEI, intégrations
Sage/WMS) — compatibles (ex. « dispo matière » de l'ordonnancement = une **requête sur les mouvements**).

**Impact CODE** : prochaine grosse brique = **`stock_move`** (pas une table Stock-compteur) ; `Article`
gagne un champ **`family`** (migration 003) ; le stock devient une **vue d'agrégation** (+ index, snapshot
plus tard, cf. pied de page du schéma). Le modèle `tracabilite-model.java` est **révisé** en conséquence
(son `Stock`-compteur → dérivé).

## 6quater. 📐 Modèle UoM — pivot + référentiel (nailé 2026-07-25, session concept)

Prolonge le §6ter : le `stock_move` porte une `uom` — voici **quelle** unité, et **d'où** vient la conversion.

**Le pivot (`stock_uom`)** — chaque `article` a **UNE** unité de stock, choisie une fois. **Tous** ses
`stock_move` sont comptés dans cette unité → la dérivation `Σ mouvements` reste une **addition bête**, jamais
une conversion à la lecture. Règle de choix : *« dans quelle unité je compterais ce stock à la main dans
l'entrepôt ? »*. Conversion = **une étoile** (chaque unité déclare 1 facteur vers le pivot), pas une toile N².
> ⚠️ Deux pivots à ne pas confondre : **pivot de catégorie** (réf. mathématique d'une famille : le mètre
> pour les longueurs) vs **pivot de l'article** (`stock_uom`, choix métier par article). Le 1er rend les
> conversions possibles ; le 2ᵉ rend la somme des mouvements cohérente.

**quantité × unité = mesure** — l'unité qualifie **le nombre saisi**, pas « un objet ». `100 + unité` = 100
pièces ; `3 + colis` = 3 pas de « colis ». À la saisie (réception…), l'opérateur choisit **quelle unité +
combien** ; il **ne tape JAMAIS le taux** (déjà en base). On convertit **à l'écriture** → le mouvement est
stocké dans le `stock_uom`.

**Où vit le facteur de conversion** (le « 50 » de 1 colis = 50 pièces) — **3 cas** :
| Type | Le facteur est… | Où |
|---|---|---|
| Ratio **universel** (douzaine=12, m=100 cm) | défini une fois, **global** | table `uom` (ratio vers pivot de catégorie) |
| **Conditionnement propre à l'article** (colis=50 pour CET article) | défini une fois, **par article** | `packaging` lié au produit (≠ `uom` : un facteur d'`uom` est global, ne peut pas être « 50 ici, 24 là ») |
| **Calculé** (unité→m² bois) | **calculé** à la volée | dérivé des **dimensions** article/lot (`larg×long`) |

**Référentiel vs mouvement** — le facteur est une **donnée du référentiel** : définie **AVANT** l'exploitation,
par un **admin du client** (l'éditeur pré-charge un socle : unités SI, catégories), **en table**, stable.
L'opérateur/la réception ne fait que **la lire**. Chaîne de dépendance : `uom_category → uom → article
(+stock_uom) → packaging → PUIS reception/stock_move`. Format **récurrent** → référentiel (code propre :
`COLIS-50`, `COLIS-45`) ; **cas ponctuel** (un colis dépareillé de 45 exceptionnel) → **saisie sur la
ligne/le lot**, JAMAIS une entrée référentiel (sinon poubelle : `COLIS-37`, `COLIS-48`…). Principe général :
un **taux** est du référentiel stable (→ table), un **stock** est un résultat calculé (→ jamais de compteur).

**Impact CODE** : `reception_line.uom_id` = unité de saisie (≠ `stock_uom` OK, mais **même catégorie**,
convertible) → conversion → `stock_move` en `stock_uom`. `stock_move.uom_id` = **toujours** le `stock_uom`
(le champ existe pour l'explicite/robustesse ; vaut `article.stock_uom` ~99 % des cas). Angle bois = le
différenciateur : contrôle réception (poids théorique dims×densité vs pesé) tombe de la cascade de poids §6ter.

## 6quinquies. 💼 MODÈLE PRODUIT — offres, licensing, séquence de flux, nommage (adopté 2026-07)

Source : sessions Odoo de l'utilisateur (`CONTEXTE-mini_wms_2.md` + `CLAUDE.md`), **réconcilié** (Liquibase, pas Flyway).

### A. Deux offres empilées, un seul socle — « flux d'abord, production en option »
- **FLUX** = le **socle vendable** (négoce/distributeur, PME : réception→stockage→prépa→expédition ; **ne fabrique rien**).
- **FLUX + GPAO** = **étage premium optionnel** (fabricant : reçoit matière → **produit** → expédie).
- ❌ **« GPAO seule » n'existe PAS** : produire = consommer/créer du stock = des **mouvements** → la GPAO **présuppose le flux**.
- ⚠️ **Discipline NON négociable** : le flux tourne **complet et cohérent SANS aucun OF**. Jamais de champ obligatoire
  lié à un OF sur un `Mouvement`. `Nomenclature`/`OrdreFabrication` **existent mais restent vides** pour un client flux-seul (GPAO invisible).

### B. ⭐ Licensing — DROIT (entitlement) ≠ PARAMÉTRAGE (le cœur « produit commercialisable »)
Deux notions à **ne JAMAIS confondre** :
- **DROIT** = ce que le client a *souscrit* (FLUX, GPAO…). **Commercial**, écrit par le **VENDEUR seul** (page superadmin),
  source de vérité = schéma partagé **`public`**. Le client ne peut **JAMAIS** s'auto-attribuer un droit.
- **PARAMÉTRAGE** = *comment* le client utilise ce à quoi il a droit (emplacements, workflows…). Édité par l'**admin du
  client**, dans **son** schéma. N'affiche **que** ce que le droit autorise.
> Le droit **gate** le paramétrage : GPAO non souscrite → section masquée (« non souscrit — contactez votre intégrateur » = **hook de vente**).

**Table `public.tenant_features`** (1 ligne par droit, PAS un `TEXT[]`) :
```
tenant (FK public.tenants) · feature ('FLUX'|'GPAO'|futurs 'TRACA_LOT','EDI'…) · status ('ACTIVE'|'TRIAL')
starts_at (nullable) · expires_at (nullable = permanent) · PK (tenant, feature)
```
Couvre 3 cas d'un seul modèle : **démo** (`TRIAL` + `expires_at` J+30) · **licence à durée** (`expires_at` = fin contrat) · **permanent** (`expires_at NULL`).
- **Test de droit** (porte unique) : ligne existe, `status IN (ACTIVE,TRIAL)`, `now()` ∈ [`starts_at`, `expires_at`].
- **Expiration = désactivation AUTO** (la date passée coupe seule, aucune tâche de nettoyage).
- **Ne jamais couper les DONNÉES** : fin de démo GPAO → fonctions masquées, **données conservées** (si conversion plus tard, il les retrouve).
- **« Expiré » ≠ « jamais eu »** → message « essai terminé, contactez-nous » vs « découvrez la GPAO ».
- **Minimal** : pas de prix/remise/n° contrat ici (→ facturation, autre sujet). Répond juste à « **a-t-il le droit, MAINTENANT ?** ».

### C. Séquence de flux — le STOCK n'est PAS une étape
```
Réception → Contrôle → Rangement → [STOCK] → Commande → Allocation → Préparation → Colisage/Caisse → Expédition → Suivi
```
- **`[STOCK]` = réservoir DÉRIVÉ** (état lu à tout moment), **jamais** une case qu'on traverse.
- Chaque étape = **un mouvement** : Réception=`FOURNISSEUR→Quai` · Rangement=`Quai→Zone` · **Allocation = mouvement `DRAFT`**
  `Zone→Prépa` (réserve sans sortir) · Préparation = ce `DRAFT` passe `DONE` · Colisage = vers un `Colis` · Expédition=`Zone→CLIENT`.
- ⭐ **Allocation = étape critique invisible** : stock **disponible = physique − réservé (`DRAFT`)** → évite de promettre 2× la même palette.
- **Contrôle réception** = le **différenciateur bois** (pesée vs poids théorique, humidité). Sans lui = WMS générique ; avec = on parle au métier.

### D. Convention de nommage — code métier en FRANÇAIS
Le code existant l'est déjà (`Article`, `Emplacement`). **Ne JAMAIS mélanger FR/EN.** Traduire les noms anglais du schéma :
`stock_move`→**Mouvement** · `product`→**Article** · `location`→**Emplacement** · `uom`→**Unite** · `package`→**Colis** ·
`bom`→**Nomenclature** (+`LigneNomenclature`) · `work_order`→**OrdreFabrication** · `reception`→**Reception** (+`LigneReception`) ·
`material`→**Materiau** · `lot`→**Lot** · `tenant_features`→**TenantFeature**. Exception : termes d'infra en anglais (tenant, feature, schema, JWT).

## 6sexies. 🚚 Cluster APPROVISIONNEMENT — tiers → commande → réception (schéma dédié 2026-07)

Source : [`schema-approvisionnement-mini_wms.html`](schema-approvisionnement-mini_wms.html). Répond à l'œil métier de
l'utilisateur (fournisseur, commande, transporteur, pesée). Flux : `commande_achat → reception → mouvement (FOURNISSEUR→Quai)`.

### `tiers` — UNE table, des RÔLES (l'idée clé) ⭐
Fournisseur, transporteur, (futur) client = des **booléens** sur un même `tiers` : `est_fournisseur`, `est_transporteur`,
`est_client`. Un transporteur qui est **aussi** fournisseur = **une seule fiche**. **Réutilisable tel quel côté vente.**
Champs : `code`, `raison_sociale`, `siret`, `email`, `telephone`. + table **`adresse`** liée (SIEGE/LIVRAISON/FACTURATION, plusieurs par tiers).
> 👉 Ça **remplace** l'idée « entité Fournisseur » : un seul `Tiers` polyvalent au lieu de N entités séparées.

### `commande_achat` (+ `ligne_commande_achat`) — PLANIFIE, ne bouge rien
La commande décrit **ce qui est attendu** ; elle **n'impacte pas le stock**. Champs : `reference`, FK `fournisseur_id→tiers`,
`date_commande`/`date_prevue`, `etat` (BROUILLON|CONFIRMEE|RECUE_PARTIEL|RECUE|ANNULEE), `montant_total` (dérivé).
Ligne : article, unite, `quantite_commandee`, `prix_unitaire`.

### `reception` (+ `ligne_reception`) — EXÉCUTE + génère les mouvements
FK `fournisseur_id→tiers`, FK **`transporteur_id→tiers`**, FK **`commande_achat_id` (NULLABLE)**, `num_bl`, date, etat,
**`poids_pese_kg`**. Ligne : FK `ligne_commande_id` (nullable = **reliquat**), article, lot, `quantite_recue`, unite.
→ À la **validation**, chaque ligne génère un **`Mouvement`** `FOURNISSEUR→Quai` (le stock apparaît, dérivé).

### Les 4 règles d'or (notes du schéma)
- **Reliquat DÉRIVÉ** : `ligne_reception.ligne_commande_id` relie reçu↔commandé ; l'état de la commande (RECUE_PARTIEL/RECUE)
  **en découle**, jamais saisi à la main. Reste à livrer = quantité commandée − Σ reçu.
- **Réception SANS commande possible** (`commande_achat_id`/`ligne_commande_id` **nullables**) : retour, dépannage, hors
  commande. Le flux ne suppose **JAMAIS** qu'une commande existe. ✅ cohérent « flux d'abord ».
- **Pesée = différenciateur bois** : `transporteur_id` + `poids_pese_kg` (bon de pesée) → comparé au poids théorique
  (dim × densité `materiau`) → **alerte écart**. Tracé jusqu'au transporteur.
- **Facturation VOLONTAIREMENT absente** : `prix_unitaire` sur la ligne de commande = oui (reliquat/valorisation) ; mais
  TVA/comptabilité fournisseur = **autre domaine**, à ne pas mélanger ici.

## 6septies. 📸 Référentiels au présent, documents figés — le SNAPSHOT (règle transverse, 2026-07)

**Principe** : un **référentiel évolue** (le tiers change de nom/adresse) ; un **document validé fige la réalité de son
instant** (comme le `Mouvement` immuable). Un document = un **instantané**, pas une vue temps réel.

**Piège évité** : si un document ne garde qu'une `FK tiers_id`, renommer le tiers **réécrit rétroactivement** tous les
vieux documents (et la recherche sur l'ancien nom ne trouve plus rien).

**Solution (snapshot sur documents)** — sur chaque document transactionnel (`Reception`, `CommandeAchat`, futures
`Expedition`/`Vente`…), garder **les DEUX** :
- `tiers_id` → **FK** vers le tiers (lien vivant → naviguer vers la fiche actuelle),
- une **copie FIGÉE** au moment T : `fournisseur_libelle` (+ selon la valeur légale : `fournisseur_siret`, `fournisseur_adresse`).
  ⚠️ Figer le **jeu d'infos pertinent**, pas juste le nom (sinon le bug revient sur l'adresse).

Compléments :
- `actif` (bool) sur `Tiers` → sortir les obsolètes des listes de saisie **sans supprimer** (ne JAMAIS renommer-écraser
  un tiers pour « corriger » → ça altère l'historique lu via les FK).
- Historique de noms daté (SCD Type 2) = **seulement si** retrouver un tiers par un ancien nom **dans le référentiel**
  devient un besoin réel. Rare → **ne pas anticiper** (si activé : table append-only, le `Tiers` garde toujours la valeur actuelle).

Même logique pour l'article si son libellé/prix doit être figé (ex. `prix_unitaire` sur `LigneCommandeAchat`).
**Règle générale : ne jamais laisser un référentiel réécrire l'histoire d'un document validé.**

## 6octies. 🎛️ Formulaires paramétrables PAR CLIENT — le pilier « personnalisation »

Besoin : les champs saisis/affichés d'un document (ex. réception) **varient selon le client**. ⚠️ **Ne JAMAIS rendre le
SCHÉMA paramétrable** (pas de colonnes/tables par client → divergence interdite). On paramètre la **présentation**, pas les données.

**3 couches à séparer** :
1. **Modèle (FIXE)** — la table contient *tous* les champs métier possibles ; un champ inutilisé reste `NULL`. Schéma unique pour tous.
2. **Config d'affichage (PARAMÉTRABLE, par client)** — métadonnée d'**UI**, dans le **schéma du client** (paramétrage, pas un droit).
3. **Rendu (GÉNÉRIQUE)** — le front lit la config et **construit le formulaire dynamiquement** (un seul écran, piloté par config).

Table **`<client>.config_champ`** : `document` (RECEPTION|LIGNE_RECEPTION|…) · `champ` · `visible` · `obligatoire` ·
`ordre` · `libelle_custom` · `type` (pour champs custom). Validation `obligatoire` **des 2 côtés** (front = ergonomie + back = sécurité).

**Niveau A retenu (liste blanche)** : montrer/masquer/ordonner/rendre obligatoire des champs **qui existent en colonnes**. Couvre ~95 %.

**Champs vraiment libres → colonne `champs_custom JSONB`** (PAS d'EAV) :
- **Une seule** colonne `champs_custom JSONB` sur le document. PostgreSQL → JSONB **typé + indexable** (index GIN) : la souplesse de l'EAV sans ses défauts.
- Définition du champ = `config_champ` ; **valeur** = `champs_custom JSONB` du document.
- ⭐ **Règle de promotion** : le JSONB est une **salle d'attente**, pas une décharge → dès qu'un champ custom devient un
  besoin **récurrent** (plusieurs clients) → **le promouvoir en vraie colonne**. Anti-EAV : tout ce qui est connu/fini → colonnes.
- **Livrer un jeu par DÉFAUT** (config standard sensée, jamais un formulaire vide à configurer de zéro).

**Types stockables dans `champs_custom`** — JSON n'en connaît que **6** : `string`, `number`, `boolean`, `null`, `array`, `object`
(vérifié par test : Java ↔ JSONB fait l'aller-retour fidèlement, objets imbriqués compris) :
- ⚠️ **AUCUN type DATE en JSON** → une date se stocke **en texte ISO 8601** : `"2026-08-07"` / `"2026-08-07T14:30:00"`.
  Ce format a deux vertus : le **tri alphabétique = tri chronologique**, et le **cast SQL direct** — `(champs_custom->>'dateIso')::date + 30` fonctionne.
  ⛔ **Interdit** : `"07/08/2026"` (ambigu jj/mm ou mm/jj, ni triable ni castable). **Le format d'affichage est l'affaire du FRONT, jamais du stockage.**
- ⛔ **Jamais d'argent, de quantité ni de poids** dans le JSONB : Jackson désérialise un décimal en `Double` (arithmétique binaire
  approximative), et `12.50` revient `12.5`. Tout ce qui entre dans un **calcul, un total ou une contrainte** → **vraie colonne typée**
  (`NUMERIC`). Ex. la **pesée** en réception = colonne, jamais une clé JSON. *Le JSONB accompagne le métier, il ne le porte pas.*
- **Le JSONB n'impose AUCUN type** : la colonne accepte `"entier": "quinze"` sans broncher. C'est un **réservoir, pas un contrat** →
  le type est déclaré par `config_champ.type` et **validé par l'application AVANT écriture** (back = sécurité, front = ergonomie).
- **Indexation** (le jour où le volume l'exige, pas avant) : un index **GIN** accélère `@>` et `?`, **PAS `->>`**.
  Pour un champ précis très filtré chez un client → **index B-tree d'expression** `((champs_custom->>'x'))`, plus petit et ciblé.
  Bénéfice de l'archi 1 schéma/client : **on indexe client par client, selon SES champs**.

**Portée du custom = CIBLÉE, pas généralisée** (« un privilège qu'une table mérite, jamais un défaut »). Tables autorisées
**pour l'instant** : `Tiers` (client ET fournisseur), `Reception` (en-tête), `LigneReception`. **Interdits** : `Mouvement`
(cœur immuable/auditable — JAMAIS de custom), tables techniques/jonction. On étend **table par table, sur besoin réel**.
- Java : interface `Personnalisable` (champ `champs_custom` + validation) dont **héritent SEULEMENT** les entités concernées → hériter = choix conscient.

Frontière : `config_champ` = **paramétrage** (admin client, son schéma) ≠ **droit** (`public.tenant_features`, vendeur).

## 7. Récit de migration (Uniface → moderne) — **argument portfolio**
- **Legacy** : monolithe **Uniface** (GPAO **+** WMS même base), rustines (caisse virtuelle pour négoce),
  éditions Crystal Report, intégration Sage X3.
- **Moderne** : **Quarkus** (microservices) + **Angular/Flutter** (dashboard) + **PostgreSQL** + **IA**.
- **Gains** : plus d'objet racine **du tout** (le négoce n'est plus une caisse fantôme, cf. §2), `Colis` découplé,
  stock **dérivé** au lieu d'un compteur, moteur de formules paramétrable, IA, WMS découplé.
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
 │    ├── domain/  (Mouvement, Article, Lot, Emplacement — PAS de table Stock : elle est DÉRIVÉE)
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
- **Reprise de données à l'installation client** (noté 2026-07-05) : chaque client a SES emplacements/articles
  → module d'**import CSV/Excel** (plan d'entrepôt, catalogue). Les seeds de démo = dev uniquement
  (désactivés en prod — profil Quarkus).
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
2. **`Réception`** — le 1er vrai flux : réceptionner un article (n° lot si activé) à un emplacement → **génère un `Mouvement` `FOURNISSEUR → Emplacement`** ; le stock **APPARAÎT** par dérivation (⚠️ aucune table `Stock` à créer/incrémenter — §6ter). Démontrable tout de suite, exerce le cœur (mouvement/lot/traçabilité).
3. **Dashboard** = simple menu de liens d'abord ; « widgets configurables par l'admin » = **v2**.

**Alignement** : Réception (backend) = **Quarkus** (focus) ; l'écran = un peu de **Flutter** (certif). La **plateforme devient le projet backend principal** ; gestion-stock s'y fond. Point de reprise probable : branche `feature/backend-emplacement` (entité `Emplacement` déjà amorcée).
