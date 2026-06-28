# Journal de dev — mini-WMS

Notes chronologiques (décisions, blocages, idées). Le plus récent en haut.

---

## 2026-06-28 — Initialisation du repo

- Création du repo `mini-wms` (dossier voisin de `mini-projet`).
- Cadrage posé dans [migration-wms-scoping.md](migration-wms-scoping.md) :
  domaine, schéma ER (périmètre A), décisions « modernes vs legacy ».
- Confidentialité : `_legacy/` gitignoré, on reconstruit le concept pas le code.
- **À faire ensuite** : déposer le PHP source dans `_legacy/`, reverse de la BDD,
  comparer avec le schéma proposé, puis coder l'entité `Emplacement`.
