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
├── docs/
│   ├── migration-wms-scoping.md   Cadrage : domaine, schéma ER, décisions
│   └── dev-journal.md             Journal de dev (chronologique)
├── _legacy/                       PHP source (LOCAL, gitignoré)
├── backend/                       Quarkus (à venir)
└── mobile/                        Flutter (à venir)
```

## Méthode de migration

On ne traduit pas le PHP ligne à ligne. On repart du **métier** et du **modèle** :

1. Comprendre le domaine + reverse de la BDD legacy
2. Reconstruire le schéma en entités Quarkus / Panache
3. Réexposer en API REST
4. Refaire l'UI (Flutter / Angular)
