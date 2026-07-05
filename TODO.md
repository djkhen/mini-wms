# 📦 TODO / Idées — mini-wms

> Bac à idées du projet. Capturé en vrac, rangé ensuite. Projet **en pause**
> (focus actuel = certif Flutter + gestion-stock) → idées à reprendre plus tard.

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
