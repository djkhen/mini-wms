# Analyse du legacy — synthèse anonymisée (FLUX + TRACK)

> Reverse-engineering des sources legacy, **entièrement anonymisé** (aucun nom réel
> d'entreprise/site/client/produit). Sert de base à la reconstruction du modèle
> générique. Sources locales gitignorées (cf. `_legacy/SOURCES.md`).
>
> ⚠️ **Sécurité signalée** : la source FLUX versionnait des secrets de production en
> clair (DB/FTP/SMTP) + requêtes SQL concaténées (injection). Hors de notre repo
> (gitignoré), mais à traiter côté propriétaire d'origine.

## Les deux sources = deux couches complémentaires

| | **TRACK** | **FLUX** |
|---|---|---|
| Nature | **WMS physique complet** | **Couche « demandes / workflow »** |
| Cœur | Stock réel : entrepôts, emplacements, allocations, journal de mouvements | Demandes logistiques typées avec machine à états |
| Stock | Géré nativement (modèle propre) | Délégué à un **ERP externe** (synchro manuelle) |
| Forces | Emplacements riches, colisage/caisses, expédition/transport, multi-tenant, facturation UO | Modélisation workflow claire, jalons datés, notifications, mobile |
| Faiblesses | Très large, champs spécifiques client en dur, ~0 FK | Pas de vrai modèle de stock, 13 tables clonées, statuts en texte libre |

→ **Synthèse cible** : `Demande` (intention/workflow, façon FLUX) qui génère des
`Mouvement` physiques (source→destination, façon TRACK) sur un socle
Article / Emplacement / Stock.

---

## TRACK — WMS physique (modèle de référence)

### Socle stock (base référentiel)
- **Entrepôt** : code, nom, rattaché à un site.
- **Emplacement** ⭐ : code, zone, **coordonnées X/Y/Z**, contraintes physiques
  (température/sécurité…), **seuils min/max/réappro**, dims/poids max.
- **Référence/Article** : code, désignation, unité, dims/poids, prix.
- **Allocation = Stock** : quantité par (emplacement × référence) + `quantité_audit`
  (inventaire).
- **Transaction = Mouvement** : type `IN / OUT / RESET / AUDIT_IN / AUDIT_OUT / SCRAP`,
  emplacement, référence, quantité, utilisateur → **journal de mouvements**.
- **Fournisseur**, **Transporteur**.

### Opérationnel (flux entrant → sortant)
- **Réception** : machine à états (`transmitted → takeOver → (pause) → completed /
  deleted`), fournisseur/transporteur, n° de suivi/traçabilité, urgence, dates +
  utilisateur par transition.
- **Item** : pièce/ligne tracée, rattachée à une référence + réception, emplacement
  from/to, auto-référence (kit/sous-item), quantités reçue/expédiée/rebut.
- **Mouvement d'item** : **pivot du flux** — trace le passage colis → caisse →
  expédition → transport (dates-jalons).
- **Colisage** (préparation/picking) → **Caisses** (⭐ **imbriquées**, bloc douane/
  export) → **Expédition** → **Transport** (véhicule + remorques, conducteur).
- **Nomenclature / kit** (BOM), **Non-conformité** (polymorphe, bloquante),
  **Facturation à l'UO** (unités d'œuvre → factures + grilles tarifaires à paliers).
- **Module « points d'appel »** : demande de transport interne déclenchée depuis une
  borne par un opérateur, **planification de tournées**, **double-scan** validation
  départ/arrivée. (Atypique d'un WMS standard.)

### Architecture notable
- **Multi-tenant** : *toutes* les tables portent un identifiant de configuration
  (`cnfg_id`) → une instance sert plusieurs clients/contrats, isolés.
- **Quasi-aucune clé étrangère** (intégrité applicative), beaucoup de **listes EAV**
  paramétrables (les `*_lst_id`), statuts hétérogènes (ENUM ici, `int` ailleurs).
- Champs **spécifiques client en dur** dans la réception (caractéristiques produit,
  n° série, n° caisse, OF outillage…) → **à externaliser** en attributs typés.
- **Pas de code Java/Quarkus** : la « migration » est un refactor PHP (couches
  Service/Manager + embryon d'API REST pour le mobile) préparant un strangler. Le
  **contrat REST** et le découpage en services restent de bonnes specs.

---

## FLUX — couche demandes / workflow

### Tronc commun (≈13 tables de « demande de flux » quasi identiques)
`id` · `demandeur` · `référence` · `quantité` · `commentaire` (demandeur) ·
`commentaire_traitant` · **`état`** (texte libre, défaut « Envoyé ») · `division` ·
**jalons datés** : `date_demande`, `date_préparation`, `date_livraison`,
`date_blocage`, `date_réception`.

### Types de flux (chacun = une table clonée, à unifier)
1. **Aléa** de production · 2. **Bon de sortie matière** · 3. **Flux direct**
(cross-dock, piloté par import de fichiers) · 4. **Réappro urgent** ·
5. **Transfert inter-sites** (origine→destination) · 6. **Servi hors kanban** ·
7. **Tri qualité** · 8. **Retour tri** · 9. **Remboursement qualité** ·
10. **Réservation/restitution article** · 11. **Inventaire** (réconciliation
quantité ERP vs comptée, recomptages) · 12. **Anomalie réception** (litige BL/POD,
fournisseur, pièces jointes) · 13. **Mise en conformité** (tickets NC, délais).

### Machine à états commune
`Envoyé → Reçu / Préparé → Livré`, avec branches `Bloqué`, `Annulé`, `Clôturé`.
Chaque transition horodate le jalon `date_*` correspondant.

### Support
- **Utilisateurs** + **Profils** (rôles : logistique, service, production, qualité,
  appro, coordinateur, magasin, mobile…), **menu dynamique** filtré par profil.
- **Zones de stockage** (`ref_zone`, zones logiques — pas d'adressage fin allée/
  niveau), destinataires d'e-mails **par module/flux**.
- Couches transverses : **exports Excel/PDF**, **KPI/graphiques**, **notifications
  e-mail**, **client mobile Flutter** lisant les mêmes flux.

### À moderniser
- **13 tables clonées → une entité `Demande` générique** (type de flux en enum +
  attributs spécifiques en JSON/colonnes) ou héritage Panache.
- **Statut en texte libre → enum + machine à états** explicite.
- **13 colonnes `*_pref` dans users → table de liaison** `préférence(user, type_flux)`.
- **Aucune FK** → vraies relations (`demande→zone`, `demande→user`, `email→type_flux`).
- **latin1 / MyISAM → UTF-8 / transactionnel** (cible Postgres).
- Désactivation de fonctionnalité par **hack de données** → flag `actif`.
- Double-saisie ERP (champs de synchro manuels) → **intégration/API** propre.

---

## Anti-patterns legacy communs (ce qu'on modernise)

| Legacy | Cible Quarkus/Panache |
|---|---|
| Aucune clé étrangère | Relations `@ManyToOne`/`@OneToMany` explicites |
| Statuts en `varchar`/`int` magiques | **Enums Java** + machine à états |
| Tables clonées par variante | 1 entité + discriminateur/type |
| Listes EAV `*_lst_id` non typées | Enums métier ou entité `ListItem` générique |
| Colonnes audit répétées partout | `@MappedSuperclass` Auditable + listeners |
| Champs spécifiques client en dur | Attributs dynamiques / JSON / table d'attributs |
| latin1 + MyISAM | UTF-8 + InnoDB/Postgres transactionnel |
| Secrets en clair, SQL concaténé | Config externalisée, requêtes paramétrées (Panache) |

---

## Vers le modèle générique (à figer ensemble)

Le mini-WMS retient **le socle physique de TRACK** (Article / Emplacement / Stock /
Mouvement / Réception) **enrichi de la couche workflow de FLUX** (`Demande` typée à
machine à états). Décisions de périmètre et d'architecture : voir
[migration-wms-scoping.md](migration-wms-scoping.md) (section décisions).
