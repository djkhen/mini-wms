# mini-WMS

Mini **WMS** (Warehouse Management System) **standard et générique** — agnostique du
type de marchandise géré. Projet de **migration / modernisation** : reconstruction
d'une appli « gestion de flux » legacy (PHP 5.5) vers une stack moderne.

- **Backend** : Quarkus (Hibernate Panache + REST) + PostgreSQL
- **Mobile / Desktop** : Flutter
- **Web** (option) : Angular

Pièce de portfolio **brownfield** (moderniser de l'existant), complémentaire du
projet `gestion-stock` (greenfield). Différenciation : ce projet est centré sur le
**flux d'entrepôt + les emplacements** (le OÙ / le QUAND / quel mouvement), pas sur
le catalogue d'articles.

## ⚠️ Confidentialité

L'appli source vient d'un employeur (secteur sensible). On reconstruit **le concept
et le modèle métier**, **jamais** le code ni les données réelles.

- Données **fictives**, noms d'applis/lieux **fictifs**.
- Le code legacy reste dans `_legacy/` — **gitignoré, jamais commité** (référence de
  lecture seule pour le reverse-engineering).
- Aucune référence réelle (client, pièce, site, nom d'appli) dans ce repo.

## Structure

```
mini-wms/
├── NOTES-DEV.md                   Journal de dev (chronologique) — à lire en 1er
├── docs/
│   ├── migration-wms-scoping.md   Cadrage : domaine, schéma ER, décisions
│   └── legacy-analysis.md         Reverse anonymisé des sources legacy
├── _legacy/                       PHP source (LOCAL, gitignoré)
├── backend/                       API Quarkus (Panache + PostgreSQL)
└── mobile/                        Flutter (à venir)
```

## Démarrer le backend

```bash
# Tout via Docker (db + api) :
docker compose up --build          # API sur http://localhost:8080

# Ou en dev (Postgres local sur 5432, base wmsdb/wms/wms) :
cd backend && ./gradlew quarkusDev # API sur http://localhost:8080
```

Swagger UI : `/q/swagger-ui`. Endpoints actuels : `/emplacements` (GET/POST/PUT/DELETE)
et `/articles` (GET/POST/PUT/PATCH/DELETE).

## Méthode de migration

On ne traduit pas le PHP ligne à ligne. On repart du **métier** et du **modèle** :

1. Comprendre le domaine + reverse de la BDD legacy
2. Reconstruire le schéma en entités Quarkus / Panache
3. Réexposer en API REST
4. Refaire l'UI (Flutter / Angular)
