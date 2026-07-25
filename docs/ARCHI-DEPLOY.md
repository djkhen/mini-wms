# ARCHI-DEPLOY — Fluxo / mini-wms (handoff ops)

> Consigne les décisions de **déploiement, multi-tenant et provisioning**. Complément
> de [`conception-plateforme.md`](conception-plateforme.md) (modèle métier) et de
> [`../CLAUDE.md`](../CLAUDE.md) (handoff global).

> 🔧 **RÉCONCILIATION (2026-07-25)** — ce doc vient d'une session antérieure. Aligné sur les décisions actées :
> - **Migrations : LIQUIBASE**, pas Flyway (décidé 2026-07-05). Toutes les mentions « Flyway » ci-dessous ont
>   été remplacées par Liquibase. Changelogs : `backend/src/main/resources/db/changeLog.xml` (master) +
>   `db/changes/NNN-*.sql` (changesets SQL formaté). Recette détaillée : [`../NOTES-DEV.md`](../NOTES-DEV.md).
> - **Build : GRADLE 9.3.1** (`./gradlew`), Quarkus **3.33.2.1 LTS**.
> - Le modèle métier de référence = [`conception-plateforme.md`](conception-plateforme.md) (ex-« CONTEXTE-mini_wms »).
> - ⚠️ **Point d'implémentation ouvert** : migrer un schéma tenant peut se faire soit **par l'app** (Quarkus
>   joue Liquibase au boot / à la création du tenant), soit par un **CLI Liquibase** externe (comme les scripts
>   ci-dessous). Les scripts sont des **squelettes** — le mécanisme concret est à trancher au moment du 2ᵉ tenant.

---

## 0. Résumé en une page

- **Hébergement actuel** : Railway (build Dockerfile depuis GitHub, HTTPS + CD gratuits).
- **Multi-tenant retenu** : app partagée + **1 schéma Postgres par client** (Hibernate mode `SCHEMA`). Tenant lu depuis le **JWT Keycloak**. Résolveur unique.
- **Ajouter un client (cas normal)** = `CREATE SCHEMA` + Liquibase + realm Keycloak + backup. **Pas** de nouveau conteneur → script `add-tenant.sh`.
- **Silo (échappatoire)** = conteneur dédié pour un client à forte exigence d'isolation/conformité → script `provision-client.sh`.
- **Packaging** : JVM en dev et petit volume ; **natif** quand la densité l'exige (silo 8-10 clients).
- **CI** : GitHub Actions (build JVM + natif, tests natifs inclus).
- **Règle d'or provisioning** : industrialiser **dès le client 1**. Le script EST la doc d'infra.

---

## 1. Pourquoi Railway maintenant

Le stack a déjà ses Dockerfiles → on force le **build par Dockerfile** (Nixpacks gère mal Quarkus).

Config Railway :
- New Project → Deploy from GitHub repo → sélection du repo.
- Railway détecte le `Dockerfile`. Si ailleurs qu'à la racine : Settings → Build.
- Ajouter un service **PostgreSQL** (provisionné en 1 clic, expose `PGHOST/PGPORT/PGUSER/PGPASSWORD/PGDATABASE`).
- Variables du service Quarkus :
  ```
  QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
  QUARKUS_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
  QUARKUS_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
  QUARKUS_HTTP_HOST=0.0.0.0
  QUARKUS_HTTP_PORT=${{PORT}}
  ```
- Networking → Generate Domain (`.up.railway.app`, HTTPS inclus).
- **CD** : chaque push rebuild + redéploie. Aucun pipeline à écrire pour ça.

**Filestore** : tout fichier hors base (PJ, PDF générés) → volume dédié, sinon perte au redémarrage du conteneur.

Alternative future = **VPS + Docker Compose + Caddy** (HTTPS auto Let's Encrypt) pour maîtrise/coût. Même logique, plus d'ops.

---

## 2. Multi-tenant — les 3 modèles et la décision

| Modèle | Principe | Isolation | Ops | Verdict |
|---|---|---|---|---|
| **1. Silo** | 1 conteneur + 1 base par client | Physique (maximale) | Lourde (N déploiements) | Échappatoire conformité |
| **2. Partagé + schéma** | 1 app, 1 **schéma** Postgres par client | Logique (forte) | Légère | **RETENU** |
| **2bis. Partagé + base** | 1 app, 1 **base** par client | Logique (forte) | Moyenne | Si un client l'exige |
| **3. `tenant_id`** | tout dans les mêmes tables, filtré | Applicative (fragile) | Minime | **Rejeté** (un `WHERE` oublié = fuite) |

**Décision : modèle 2 en `SCHEMA`.** Isolation logique sans le poids d'une base par client (`CREATE SCHEMA` vs provisionner/monitorer une base). Odoo, lui, est en 2bis (une base par client).

Point clé : **côté code, `SCHEMA` et `DATABASE` sont identiques** — même `TenantResolver`, même logique métier. C'est une propriété de config Hibernate. On peut basculer plus tard sans refactor.

Garde-fous **obligatoires** :
- Tenant depuis le **JWT Keycloak** (claim `tenant` ou realm par client), **jamais** header/param/URL.
- **Valider** le tenant contre la liste connue avant usage (anti-injection sur nom de schéma).
- **Rejeter (401)** si pas de tenant. Jamais de fallback silencieux vers `default`.
- **Résolution = frontière unique** : un seul `TenantResolver`, rien d'autre ne sait comment on identifie un client → choix réversible.
- **Test d'intégration** : token tenant A ne voit rien de tenant B.
- Démarrer avec un seul tenant `default`, mécanique en place.

### 2ter. ⭐ Cas d'usage : un client peut avoir PLUSIEURS tenants (plusieurs activités)

**« tenant » ≠ « client ».** Un même client peut exploiter **plusieurs plateformes isolées** — une par
**activité** ou **site** (ex. un distributeur avec un **flux vêtement** ET un **flux téléphone**).

Chaque activité = **un tenant = un schéma**. Concrètement :

> **Même Docker, même appli, même base de données — ce qui DIFFÈRE, c'est le schéma** (un par activité).

```
🏢 base "fluxo" (UNE seule)
   ├── 🏠 schéma acme_vetement    → articles/emplacements/mouvements du flux vêtement
   └── 🏠 schéma acme_telephone   → articles/emplacements/mouvements du flux téléphone
```
(base = l'immeuble · schéma = un appartement · tables = les pièces ; mêmes pièces, meubles propres, pas de vue chez le voisin.)

- **Provisioning** : relancer `add-tenant.sh` **une fois par activité** (`acme_vetement`, puis `acme_telephone`). **Zéro dev.**
- **Pourquoi ça marche sans code spécifique** : Fluxo est **générique** (agnostique de la marchandise). La
  spécificité métier vit dans les **données**, pas le code — téléphone → article en traçabilité **SERIE**
  (IMEI) ; vêtement → **LOT** + variantes taille/couleur + champs custom JSONB (conception §8bis).
- **Alternative** (si les deux activités **partagent** des masters — fournisseurs communs, reporting
  consolidé) : **un seul tenant** + une dimension **`Site`/`Activité`** interne. Défaut retenu : **tenants
  séparés** quand les activités sont indépendantes (plus étanche, déjà supporté).

---

## 3. Provisionner un client — CAS NORMAL (modèle 2, schéma)

Ce que ça exige réellement (toujours la même liste → scriptable) :

1. **Schéma** Postgres créé.
2. **Liquibase** applique tous les changesets sur ce schéma.
3. **Keycloak** : realm (ou client) + claim `tenant` configuré.
4. **Backup** : le schéma entre dans le dump.
5. *Conteneur / DNS / proxy* → **rien à faire** (app partagée, wildcard DNS déjà en place).

```bash
#!/usr/bin/env bash
# add-tenant.sh — ajoute un client en mode SCHEMA (modèle 2)
# Usage : ./add-tenant.sh <code> "<label commercial>"
set -euo pipefail

CODE="${1:?code tenant requis (ex: acme)}"     # minuscules, sans espace/accent, IMMUABLE
LABEL="${2:?label requis}"

# --- garde-fou : code technique valide (anti-injection sur nom de schéma) ---
[[ "$CODE" =~ ^[a-z][a-z0-9_]{1,30}$ ]] || { echo "code invalide"; exit 1; }

: "${PGHOST:?}" "${PGUSER:?}" "${PGPASSWORD:?}" "${PGDATABASE:?}"   # depuis l'env/secret manager
: "${KC_URL:?}" "${KC_ADMIN:?}" "${KC_ADMIN_PWD:?}"

echo "==> 1. CREATE SCHEMA $CODE"
psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" \
  -c "CREATE SCHEMA IF NOT EXISTS \"$CODE\";"

echo "==> 2. Liquibase update sur le schéma $CODE"
# Le schéma tenant reçoit et le schéma métier ET sa propre table DATABASECHANGELOG.
liquibase \
  --changelog-file=db/changeLog.xml \
  --url="jdbc:postgresql://$PGHOST/$PGDATABASE?currentSchema=$CODE" \
  --username="$PGUSER" --password="$PGPASSWORD" \
  --default-schema-name="$CODE" \
  --liquibase-schema-name="$CODE" \
  update
# (Variante : laisser l'app Quarkus jouer Liquibase au 1er boot pour ce tenant — cf. bandeau.)

echo "==> 3. Keycloak : realm + claim tenant"
# kcadm : se connecter puis créer le realm et un mapper de claim 'tenant'
kcadm.sh config credentials --server "$KC_URL" --realm master \
  --user "$KC_ADMIN" --password "$KC_ADMIN_PWD"
kcadm.sh create realms -s realm="$CODE" -s enabled=true
# mapper 'tenant' -> injecte le claim dans le token (à adapter au client/scope)
# kcadm.sh create clients/<id>/protocol-mappers/models -r "$CODE" -f mapper-tenant.json

echo "==> 4. Enregistrer le tenant (table admin + liste de backup)"
psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" \
  -c "INSERT INTO public.tenants(code,label,created_at) VALUES ('$CODE','$LABEL',now())
      ON CONFLICT (code) DO NOTHING;"

echo "OK — tenant '$CODE' prêt. Aucun redéploiement nécessaire."
```

> La table `public.tenants` (schéma partagé `public`) est aussi la **liste connue** que le `TenantResolver` consulte pour valider un tenant. ⚠️ C'est un **registre** de tenants, pas une colonne `tenant_id` sur les tables métier (l'anti-pattern rejeté au §2).

---

## 4. Provisionner un client — SILO (modèle 1, échappatoire)

À réserver aux clients à forte exigence (données sensibles, audit, conformité) ou custom lourd. Là, « ajouter un client » = **nouveau conteneur**. Les 6 opérations, toujours identiques :

1. **Conteneur** — même image, nom/port différents (`-p` + `.env` par client).
2. **Base** — `CREATE DATABASE` + Liquibase.
3. **Reverse proxy** — wildcard `*.app.com` (rien à faire) ou bloc Caddy dédié.
4. **DNS** — wildcard posé une fois.
5. **Keycloak** — realm/client pour le tenant.
6. **Backup** — la base entre dans le script de dump.

```bash
#!/usr/bin/env bash
# provision-client.sh — déploie un client en SILO (modèle 1)
# Usage : ./provision-client.sh <code> "<label>"
set -euo pipefail

CODE="${1:?code requis}"; LABEL="${2:?label requis}"
[[ "$CODE" =~ ^[a-z][a-z0-9_]{1,30}$ ]] || { echo "code invalide"; exit 1; }

ROOT="/opt/mini_wms"; ENVDIR="$ROOT/clients"; mkdir -p "$ENVDIR"
PORT=$(( 8000 + RANDOM % 900 ))     # à remplacer par une alloc déterministe

echo "==> 1. .env client"
cat > "$ENVDIR/$CODE.env" <<EOF
CLIENT_CODE=$CODE
CLIENT_LABEL=$LABEL
DB_NAME=wms_$CODE
DB_PASSWORD=$(openssl rand -hex 16)
APP_PORT=$PORT
SUBDOMAIN=$CODE.app.com
EOF

echo "==> 2. Base + migrations"
psql -h "$PGHOST" -U "$PGUSER" -c "CREATE DATABASE wms_$CODE;" || true
liquibase --changelog-file=db/changeLog.xml \
  --url="jdbc:postgresql://$PGHOST/wms_$CODE" \
  --username="$PGUSER" --password="$PGPASSWORD" update

echo "==> 3. Conteneur (image partagée, projet isolé)"
docker compose -p "client-$CODE" --env-file "$ENVDIR/$CODE.env" up -d

echo "==> 4. Reverse proxy"
# Wildcard *.app.com déjà routé → rien. Sinon, ajouter un bloc Caddy et reload.

echo "==> 5. Keycloak realm"
kcadm.sh config credentials --server "$KC_URL" --realm master --user "$KC_ADMIN" --password "$KC_ADMIN_PWD"
kcadm.sh create realms -s realm="$CODE" -s enabled=true

echo "==> 6. Backup"
echo "wms_$CODE" >> "$ROOT/backup/databases.list"

echo "OK — silo '$CODE' déployé sur port $PORT."
```

> Les deux scripts sont des **squelettes à adapter** (secrets via gestionnaire, allocation de port déterministe, mapper Keycloak réel, idempotence). Le principe et l'ordre des étapes, eux, sont fermes.

---

## 5. JVM vs natif — impact déploiement

- **Dev + prod 1-3 clients** : JVM (~250-400 Mo RAM/instance, build ~30 s, dev mode).
- **Densité silo (8-10 clients)** : **natif** (~50-100 Mo RAM/instance). C'est LE cas d'usage.
- Risque natif : closed-world GraalVM → réflexion/proxies dynamiques cassent hors extensions officielles ; bugs visibles **seulement** en natif.
- **Parade obligatoire** : `@QuarkusIntegrationTest` sur le binaire natif **en CI**. Build natif long/gourmand (4-8 Go RAM) → jamais en local en boucle, toujours en CI.
- Sécurité : **extensions officielles Quarkus uniquement** (Panache, REST/`quarkus-rest`, OIDC, Liquibase). Vérifier toute lib tierce.
- Densité conteneurs : Docker **partage les layers** → 10 silos ≠ 10× l'image. Ce qui se multiplie, c'est la **RAM**, pas le disque. Mutualiser Postgres (une instance, une base/schéma par client) réduit fortement le coût mémoire même en silo.

---

## 6. CI/CD — GitHub Actions

- **CI** (intégration continue) : à chaque push → clone, `./gradlew build`, tests. Statut ✅/❌ sur la PR.
- **CD** (déploiement continu) : **déjà assuré par Railway** (redeploy au push).
- Rôle spécifique de la CI ici : **builder le natif** (5-10 min, 4-8 Go) et faire tourner les **tests natifs** — c'est elle qui attrape les bugs « natif only » avant la prod.
- Fichier : `.github/workflows/build.yml`. GitHub Actions gratuit sur repo public, quota généreux en privé.

> Compétence facturable en consulting : beaucoup de TPE/PME n'ont aucune CI. Monter un pipeline propre est une presta à part entière.

---

## 7. Le coût récurrent du silo & quand changer d'échelle

- Le piège du silo n'est pas la **création** (scriptée) mais la **mise à jour** : déployer une version chez N clients = N conteneurs à rebuild/redémarrer. Ça se scripte (boucle sur `clients/*.env`), mais c'est le coût qui fera basculer vers le modèle 2 le jour venu.
- Custom pour UN client en modèle 2 : Liquibase migre **tous** les schémas (Hibernate a un jeu d'entités unique → la table doit exister partout). Custom léger → **feature flag** (table partout, activée pour un tenant). Custom lourd → **sortir ce client en silo**.
- **Bash suffit jusqu'à ~15-20 clients.** Au-delà → Ansible ou Kubernetes.

---

## 8. Garde-fous — récap une ligne

- Industrialiser le provisioning **dès le client 1** (connaissance fraîche, pas de divergence, script = doc).
- Le script est **idempotent** et lisible : il documente l'infra mieux qu'un wiki.
- Tenant **toujours** depuis le JWT ; validé contre la liste connue ; 401 si absent.
- Résolution du tenant en **un seul endroit** → SCHEMA↔DATABASE↔silo restent des choix réversibles.
- Secrets hors scripts (gestionnaire/env), jamais commités.
- Natif validé **en CI** avant toute bascule ; extensions officielles only.
