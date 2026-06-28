# Cadrage — mini-WMS (migration legacy → Quarkus + Flutter)

> Document de référence à relire. Synthèse de la discussion de cadrage.

## 1. Le projet

Reconstruction générique d'une appli « gestion de flux » legacy (PHP 5.5, 10+ ans)
en un **mini-WMS standard** : il n'a pas besoin de savoir quel type de marchandise
il gère → réutilisable tous secteurs, plus vendeur qu'un truc spécialisé.

**Confidentialité (réglée)** : données fictives, noms fictifs, aucune citation des
noms/lieux réels. On reconstruit le concept/modèle, jamais le code/les données de
l'employeur. Le PHP source reste dans `_legacy/` (gitignoré).

## 2. Différenciation vs `gestion-stock`

| | gestion-stock (greenfield) | mini-WMS (brownfield) |
|---|---|---|
| Focus | Inventaire / catalogue | **Opérations / flux d'entrepôt** |
| Cœur | Articles + niveaux de stock | **Emplacements + mouvements** |
| Question | « Combien d'articles ? » | « **Où ? Quand ? Quel mouvement ?** » |

## 3. Schéma de données (périmètre A : réception + emplacements, extensible)

```
┌─ FOURNISSEUR ──────────────┐
│ id, nom                    │
└──────────────┬─────────────┘
               │ 1..N  génère
               ▼
┌─ RECEPTION (en-tête entrant) ──────────────┐
│ id, @ManyToOne Fournisseur, reference (BL),│
│ dateReception, statut (ATTENDUE/EN_COURS/  │
│ TERMINEE)                                  │
└──────────────┬─────────────────────────────┘
               │ 1..N  contient
               ▼
┌─ LIGNE_RECEPTION ──────────────────────────┐
│ id, @ManyToOne Reception, @ManyToOne       │
│ Article, quantiteAttendue, quantiteRecue   │
└────────────────────────┬───────────────────┘
                         N│..1
                          ▼
┌─ ARTICLE (le QUOI — générique) ─┐     ┌─ EMPLACEMENT (le OÙ — ⭐ cœur WMS) ─┐
│ id, reference (unique),         │     │ id, code unique (ex "A-01-03-2"),  │
│ designation, unite              │     │ libelle, zone,                     │
└───────────▲─────────────▲───────┘     │ type (RECEPTION/STOCKAGE/EXPED.),  │
            │N..1         │N..1         │ actif                              │
            │             │             └──────▲──────────────▲──────────────┘
┌─ STOCK (le COMBIEN, /empl.) ─┐               │N..1          │N..1 (src + dest)
│ id, @ManyToOne Article,      ├───────────────┘              │
│ @ManyToOne Emplacement,      │                              │
│ quantite                     │     ┌─ MOUVEMENT (⭐ traçabilité / journal) ─┐
│ UNIQUE (article, emplacement)│     │ id, @ManyToOne Article,               │
└──────────────────────────────┘     │ @ManyToOne emplacementSource (null),  │
                                      │ @ManyToOne emplacementDestination     │
                                      │ (null), type (RECEPTION/RANGEMENT/    │
                                      │ TRANSFERT/PREPARATION/EXPEDITION/      │
                                      │ AJUSTEMENT), quantite, date           │
                                      └───────────────────────────────────────┘
```

## 4. Les choix « modernes » (vs schéma PHP de 10+ ans)

| Choix | Pourquoi c'est mieux |
|---|---|
| **STOCK découplé** (article × emplacement) | Multi-emplacement natif : un article à plusieurs endroits, un emplacement plusieurs articles. Contrainte `UNIQUE(article, emplacement)`. |
| **MOUVEMENT source + destination** | Gère le **transfert** nativement + journal d'audit complet. |
| **Pattern en-tête / lignes** (Reception / LigneReception) | Normalisé, propre (le legacy met souvent tout à plat). |
| **Enums** pour types / statuts | Robuste vs « magic strings/ints » du legacy. |
| **EMPLACEMENT = entité de 1er plan** | C'est l'ADN du WMS, pas un champ texte perdu. |

## 5. Périmètres

| | Couverture | Effort |
|---|---|---|
| **A** | Réception → mise en stock (emplacements + put-away) | léger, ciblé |
| **B** | A + préparation/picking → expédition | chaîne complète |
| **C** | B + traçabilité fine + inventaire | WMS « complet » |

**Démarrage : périmètre A.** Il pose déjà l'ADN WMS (les emplacements), puis on
étend vers B : `Commande` (client) → `Préparation` (picking depuis emplacements) →
`Expédition` (chaque picking = un Mouvement type PREPARATION).

## 6. Méthode de migration

1. Comprendre le domaine + reverse de la BDD legacy
2. Reconstruire le schéma en entités Quarkus / Panache
3. Réexposer en API REST
4. Refaire l'UI (Flutter / Angular)

> Pistes à généraliser si besoin (sans rien révéler) : n° de lot / n° de série,
> client-propriétaire de la pièce, date de péremption.

## 7. Prochaine étape

Comparer ce schéma avec celui de l'ancienne appli (`_legacy/`), garder le meilleur
des deux, puis coder la **1re entité : `Emplacement`** (le cœur du WMS).
