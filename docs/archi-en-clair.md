# 🧭 L'architecture EN CLAIR — fiche de relecture

> Résumé **simple** des décisions d'architecture (sessions des 3-4 juil. 2026), organisé selon
> **tes propres questions**. À relire tranquillement. La version détaillée est dans
> [`conception-plateforme.md`](conception-plateforme.md).

---

## 1. « Combien de microservices ? » → **3, pas plus**

```
   Web (Angular) ─┐
   Mobile (Flutter)┴─► GATEWAY ──► Keycloak (login, rôles — INSTALLÉ, pas codé)
                         │
                         ├──► CORE-METIER (Quarkus) ⭐ 90 % du code + PostgreSQL
                         └──► SERVICE-IA  (Python)  prévisions + ordonnancement, sans base
```

| Service | Rôle |
|---|---|
| **gateway** | porte d'entrée unique, sécurité (Keycloak) |
| **core-metier** | TOUT le métier : flux/WMS + GPAO + référentiel |
| **service-ia** | calculs (Prophet/OR-Tools), ne stocke rien |

---

## 2. « Un microservice par fonctionnalité (réception, expédition, NC) ? » → **NON !**

C'était LE malentendu (et tu l'as compris) :
- **Fonctionnalité** (réception, expédition, NC…) = **qui UTILISE les données** → simple **répertoire/package**.
- **Domaine** (flux, gpao, referentiel) = **qui POSSÈDE les données** → frontière de service possible.

> 🔑 **Le test** : si deux « services » ont besoin des **mêmes tables** → c'est **UN SEUL** service.
> Réception, expédition et NC touchent tous `Mouvement`/`Lot`/`Emplacement` → même domaine (`flux`), répertoires différents.

```
📦 core-metier                      ← 1 projet Quarkus, 1 déploiement
 ├── com.fluxo.flux                ← domaine WMS
 │    ├── domain/        Stock, Mouvement, Lot, Emplacement (entités du domaine)
 │    ├── reception/     ← fonctionnalité = répertoire
 │    ├── expedition/    ← fonctionnalité = répertoire
 │    ├── nonconformite/ ← fonctionnalité = répertoire
 │    └── parametrage/   (config DES emplacements, zones…)
 ├── com.fluxo.gpao                ← domaine GPAO
 │    ├── domain/  modeles/  of/  debit/  parametrage/ (formules BET…)
 └── com.fluxo.referentiel        ← le DICTIONNAIRE COMMUN (transverse)
      ├── article/  client/  site/
      └── dashboard/   ← gestion des widgets (catalogue + affectation par l'admin)
```

---

## 3. « Où mettre admin, user, configuration ? » → **ce ne sont pas des services**

| Candidat | Verdict | Où ça vit |
|---|---|---|
| **Admin** | c'est un **RÔLE**, pas un service | `@RolesAllowed("admin")` sur les API + sections du front |
| **User** (comptes, login, mots de passe) | déjà codé par d'autres ! | **Keycloak** |
| **User** (préférences : widgets, langue) | données transverses | `referentiel/dashboard/` |
| **Configuration d'un domaine** (formules BET, zones) | appartient à son domaine | package `parametrage/` DU domaine |
| **Configuration transverse** (sites, articles) | partagée par tous | `referentiel` |

> 🔑 **Le réflexe** : « X possède-t-il des données à lui qui vivent leur vie ? » Non → pas un service.

---

## 4. « Les emplacements dans referentiel ? » → **NON : dans `flux`**

> 🔑 **La règle** : utilisé par **UN** domaine → il vit chez ce domaine. Utilisé par **PLUSIEURS** → `referentiel`.

| Entité | Utilisée par | Vit dans |
|---|---|---|
| **Emplacement** | flux seulement (c'est LE cœur du WMS) | `com.fluxo.flux` |
| **Article** | flux + gpao (négoce/prestations !) + commercial | `com.fluxo.referentiel` |
| **Client** | commercial + expédition | `com.fluxo.referentiel` |
| **Formules BET** | gpao seulement | `com.fluxo.gpao` |
| **Poste de travail** | gpao seulement (ordonnancement) | `com.fluxo.gpao` |

Nuance importante : **« utiliser » ≠ « posséder »**. La GPAO a le droit de **référencer** l'Article
(`@ManyToOne Article` dans LignePrestation) — le référentiel est fait pour ça. Ce qui est interdit :
modifier en douce les données d'un AUTRE domaine (la GPAO ne touche pas au Stock directement,
elle **demande** au module flux).

---

## 5. « Mais l'article possède bien des emplacements ? » → **NON : c'est le `Mouvement` qui fait le lien**

L'image du supermarché :
- la **fiche produit** (Article) ne sait pas où elle est rangée ;
- le **rayon** (Emplacement) ne sait pas ce qu'il contient ;
- c'est le **journal des entrées/sorties** (Mouvement) qui permet de dire « 40 pots au rayon 12 ».

⚠️ **Nuance capitale (§6ter)** : ce « 40 » n'est **stocké NULLE PART** — il est **CALCULÉ** (Σ entrées − Σ sorties).
L'ancienne table `Stock(quantité)` était un **compteur** = l'anti-pattern → **abandonnée**.

```
Article "PLANCHE-22"          Emplacement "A-01-03"
        └───────────┐   ┌───────────┘
                    ▼   ▼
   Mouvement (article, lot, quantité>0, source → destination, date, état)   ⭐ LE lien
   "150 PLANCHE-22 : FOURNISSEUR → A-01-03, le 12/03, lot L-2026-0345"
                    │
                    ▼   Σ des mouvements DONE  (une REQUÊTE, jamais une table)
   Stock "150 PLANCHE-22 en A-01-03, lot L-2026-0345"
```

- Un article peut être à **10 emplacements** ; un emplacement peut contenir **10 articles**.
- `(article, emplacement, lot)` n'est plus une **contrainte d'unicité** sur une table `Stock` :
  c'est désormais la **clé de regroupement** (`GROUP BY`) de la requête qui dérive le stock.
- **L'article "se trouve à" des emplacements — il ne les "possède" pas.**

🧠 Le gain : le stock **à n'importe quelle date passée** devient gratuit (filtrer sur la date du mouvement),
et l'historique est **auditable** — impossible avec un compteur qu'on écrase à chaque opération.

---

## 6. « Quelle base : Mongo, Postgres, MySQL ? » → **PostgreSQL**

| Base | Verdict |
|---|---|
| **PostgreSQL** ✅ | relationnel + **ACID** + **JSONB** + tu le connais déjà (mp, gs) + standard |
| MySQL 🤷 | ferait le job, mais inférieur (JSONB, fenêtrage…) et rien à y gagner |
| MongoDB ❌ | pensé pour documents indépendants ; ton métier = jointures + transactions partout |

**1 PostgreSQL** pour core-metier, **1 schéma par module** (`flux`, `gpao`, `ref`) — prépare une
éventuelle séparation future. Le service-ia n'a **pas de base**. `JSONB` couvre les besoins « flexibles »
(config widgets, variables de modèles BET).

### ACID (le sigle expliqué avec TON exemple : transférer 50 planches de A vers B = 3 écritures)
| Lettre | Garantie | Sur le transfert |
|---|---|---|
| **A**tomicité | tout ou rien | plante au milieu → tout annulé, pas de planches « évaporées » |
| **C**ohérence | règles toujours respectées | pas de stock négatif, pas de mouvement orphelin |
| **I**solation | utilisateurs simultanés OK | 2 caristes en même temps → pas de quantité fantôme |
| **D**urabilité | commit = gravé | coupure de courant après commit → rien de perdu |

En Quarkus : `@Transactional` sur la méthode → ACID automatique. (C'est le `BEGIN TRAN…COMMIT`
que tu utilises depuis 20 ans en SQL Server/Oracle — juste le nom savant.)

---

## 7. Rappels des décisions déjà actées (contexte)

- **2 applis distinctes** : **gs** = stock SIMPLE (TPE, Flutter, à FINIR d'abord) ; **mini-wms** = LA
  plateforme (industrie : flux + GPAO + IA). gs ne doit JAMAIS recevoir emplacements/lots (sinon doublon).
- **Ordre de build de la plateforme** : 1) `Article` (+ mode lot AUCUN/LOT/SERIE) + `Emplacement` →
  2) **Réception** (1er flux complet : stock + mouvement) → 3) dashboard simple (widgets configurables = v2).
- **Traçabilité** : n° lot **optionnel par article** (`ModeTracabilite`) ; chaîne
  `Réception → Stock → OF → Colis → Client` ; modèles dans
  [`tracabilite-model.java`](tracabilite-model.java) et [`ordonnancement-gpao-model.java`](ordonnancement-gpao-model.java).
- **Focus perso inchangé** : certif Flutter (échéance sept.) + backend Quarkus. La plateforme se
  construit à rythme soutenable, par petites tranches.

---

*Fiche vivante — on la complétera à chaque décision. Si un point reste flou à la relecture → question à Claude, il réexplique et met à jour cette fiche.*
