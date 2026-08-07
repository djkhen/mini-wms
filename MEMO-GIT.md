# 🌿 Aide-mémoire Git — mini-wms

> Les commandes git qu'on utilise vraiment sur le projet, notées au fur et à mesure.
> À compléter à chaque fois qu'une commande revient.

---

## Renommer une branche — `git branch -m` (`-m` comme *move*)

⚠️ Il n'existe **PAS** de `git rename`. Le renommage de branche passe par `git branch -m`.

**Si tu es SUR la branche** (un seul argument = le nouveau nom) :
```
git branch -m nouveau-nom
```

**Si tu es AILLEURS** (deux arguments : ancien puis nouveau) :
```
git branch -m ancien-nom nouveau-nom
```

**⚠️ Si la branche a DÉJÀ été poussée sur GitHub** — renommer en local ne suffit pas :
il faut pousser le nouveau nom ET supprimer l'ancien côté distant :
```
git push origin -u nouveau-nom
git push origin --delete ancien-nom
```

🧠 Mnémotechnique : `-m` comme *move* — la même idée que `mv` pour renommer un fichier.

---

## `git pull` prend un dépôt DISTANT (remote), pas une branche

⚠️ `git pull main` → **erreur** : `'main' does not appear to be a git repository`.
`git pull` attend le nom d'un **remote** (le dépôt distant, souvent `origin`), **PAS** un nom de branche.

- ✅ `git pull` — met à jour la branche courante depuis son remote suivi.
- ✅ `git pull origin main` — tire la branche `main` depuis le remote `origin`.
- ❌ `git pull main` — git croit que `main` est un dépôt distant → échoue (mais **rien n'est cassé**).

---

## Fusionner une branche dans la branche courante — `git merge`

Pour amener les commits d'une **autre branche** dans celle où tu es :
```
git merge nom-de-la-branche
```
Exemple : sur `feature/xxx`, faire `git merge main` récupère tout ce qui est sur `main`.
Si la branche courante n'a pas divergé → « **Fast-forward** » (elle avance, sans commit de merge).

🧠 Résumé : **`pull` = un REMOTE** (réseau, ex. `origin`) · **`merge` = une BRANCHE** (local).

---

## Quand ai-je BESOIN de `git merge` ? (règle simple)

**`git merge <branche>` = amener les commits d'une AUTRE branche dans celle où JE SUIS.**

- ✅ Le commit est **déjà sur ma branche courante** (je l'y ai fait) → **PAS de merge**, il est là, je `push`.
- 🔀 Le commit est sur une **AUTRE branche** et je le veux ici → `git merge <cette-branche>`.

Les **2 sens** (toujours : je suis **SUR la cible**, je nomme **la source**) :
- **Mettre à jour ma feature avec `main`** : sur ma feature → `git merge main`.
- **Intégrer ma feature finie dans `main`** : sur `main` → `git merge feature-xxx`.

🧠 Résumé : **merge = « je veux ICI les commits de LÀ-BAS ».** Si « là-bas » = « ici », pas besoin.

⚠️ **Piège « branche courante »** : cette règle dépend de **où tu es MAINTENANT**. Le commit ne bouge pas —
c'est TOI qui changes de point de vue avec `git checkout`. Ex. : après avoir committé sur `feature/x`, le commit
y est (pas de merge). Mais si tu passes sur `main`, `main` ne l'a pas → **merge nécessaire**. Test mental :
*« le commit est-il sur la branche où je suis À CET INSTANT ? »*

---

## `git push` ne pousse QUE la branche courante

`git push` envoie **la branche où tu es** vers son homologue sur le remote. Il ne touche **AUCUNE autre branche**.

- Sur `feature/xxx` → `git push` met à jour `origin/feature/xxx`. **`main` (local ET remote) ne bouge PAS.**
- Pour mettre le travail dans `main` : aller **sur** `main` → `git merge feature/xxx` → **`git push`** (de `main`, cette fois).

🧠 Chaque branche est **indépendante** : `commit` sauve sur la branche courante · `push` l'envoie sur son remote ·
`merge` relie les branches entre elles. On met `main` à jour en allant **dessus**.

⚡ Détail malin : après un merge, le `push` de `main` est souvent **minuscule** (« 1 object ») — les fichiers étaient
déjà sur le remote (poussés via la feature branch). Git ne re-téléverse jamais ce qu'il a déjà ; seul le **commit de
merge** (un petit pointeur) est nouveau.

---

## 🔄 LE CYCLE COMPLET — workflow feature-branch (à garder sous les yeux)

Le cycle par défaut, du début jusqu'à GitHub :

```
main local
   │   git checkout -b feature/x        ← créer LA branche + aller dessus
   ▼
feature/x
   │   git add .  /  git commit -m "…"   ← travailler (1 ou plusieurs commits)
   │   git push -u origin feature/x      ← (optionnel) sauver la branche sur le remote
   │
   │   git checkout main                 ← ⚠️ REVENIR sur main D'ABORD
   │   git merge --no-ff feature/x       ← amener la branche DANS main local
   ▼
main local (à jour)
   │   git push                          ← envoyer main sur le remote
   ▼
main remote (à jour) 🎉
```

🧠 Les **2 `checkout`** sont la clé :
- `checkout -b feature/x` au début → **crée** la branche ET s'y **déplace**.
- `checkout main` avant de merger → on merge **TOUJOURS depuis la cible** (*je suis SUR main, je nomme la source*).

💡 Rappels transverses : `commit` = sauve sur la branche courante · `push` = envoie **seulement** la branche
courante sur son remote · `merge` = relie les branches. On met `main` à jour en **allant dessus**.

---

## ⚠️ Merges SANS éditeur + réparer un message (vécu le 2026-08)

**Le piège** : `git merge --no-ff <branche>` **SANS `-m`** ouvre un **ÉDITEUR** (Vim sur Git Bash Windows) pour le
message de merge → galère si on ne sait pas en sortir. **Réflexe : TOUJOURS mettre `-m`** :
```
git merge --no-ff feature/xxx -m "merge: ..."
```

### Sortir d'un éditeur coincé
- ⚠️ D'abord : **clique DANS la fenêtre du terminal** (si elle n'est pas active, aucune touche ne répond !).
- **Vim** (des `~` en début de lignes vides) : `Échap` → tape `:wq` → `Entrée` (sauve + quitte).
- **Nano** (barre en bas `^X Exit`) : `Ctrl+X` → `Y` → `Entrée`.
- **Vraiment bloqué** : ouvre un AUTRE terminal → `git commit --no-edit` (conclut le merge en cours avec le message
  par défaut, **sans** éditeur). Ou `git merge --abort` pour tout annuler et refaire **avec `-m`**.

### Réparer le message du DERNIER commit (pas encore poussé)
```
git commit --amend -m "nouveau message propre"
```
Remplace le message du dernier commit (marche aussi sur un commit de **merge** : garde les 2 parents, change juste le
message). ⚠️ **Seulement si PAS encore poussé** (sinon ça réécrit un historique partagé).

🧠 `--no-edit` = « garde le message par défaut, pas d'éditeur » · `--amend -m` = « refais le message du dernier commit ».

---

## ❓ « Est-ce que j'ai oublié un push ? » — `git status -sb`

Ne jamais se fier à sa mémoire : **la réponse est affichée en permanence**, sur la 1ʳᵉ ligne.
```
git status -sb
```
- `## main...origin/main **[ahead 4]**` → **4 commits sur mon disque uniquement** = un `git push` m'attend.
- `## main...origin/main` (rien entre crochets) → **tout est envoyé**, rien en attente. ✅
- `[behind 2]` → le remote a 2 commits que je n'ai pas → `git pull`.
- `[ahead 1, behind 2]` → les deux ont avancé chacun de leur côté → `git pull` **puis** `git push`.

⚠️ Un `push` n'envoie **que des COMMITS**. Les fichiers modifiés mais **non commités** (les lignes ` M fichier`)
ne partent jamais : ils restent tranquillement dans le répertoire de travail. Pousser n'est donc **jamais**
dangereux pour du travail en cours.

🧠 Résumé : **`[ahead N]` = N commits à pousser.** Quand la mention disparaît, c'est fait.

---

## 🫧 Lire l'historique : la « BULLE » du merge — et le lien avec Azure DevOps

Pour **voir** la forme de l'historique (et pas seulement la liste) :
```
git log --graph --oneline --decorate -10
```

Exemple réel du projet :
```
* e2608b0 docs(test): explique text block, %s et .formatted   ← LIGNE DROITE (commit direct sur main)
*   0d1d16a merge: regles de typage champs_custom
|\                                                             ← BULLE = un merge
| * 7999f28 docs: regles de typage du champs_custom JSONB
|/
*   5a96684 merge: champs custom JSONB sur tiers
|\                                                             ← BULLE
| * 5f7a1f8 feat(tiers): champs personnalisables par client
|/
```

**Un merge fusionne DEUX lignes d'histoire** → il dessine une bulle. Donc :
> **Pas de branche = rien à fusionner = PAS de merge.**

Si on commite directement sur `main`, le merge n'a pas été « oublié » : il était **sans objet**. Ce n'est
pas cassé, mais l'historique perd sa lisibilité — une bulle raconte *« voici un sujet traité de bout en bout »*,
un commit isolé ne raconte rien.

**Le rôle exact de `--no-ff`** : c'est lui qui **FORCE la bulle**. Sans lui, quand `main` n'a pas bougé pendant
le travail, Git « aplatit » la branche en ligne droite (*fast-forward*) et le regroupement disparaît de l'historique.

### 🔗 Équivalence Azure DevOps (vécu au travail, sans connaître la syntaxe)
| Interface Azure DevOps / GitHub | Ligne de commande |
|---|---|
| Créer une branche dans l'UI | `git checkout -b feature/x` |
| Pousser / publier la branche | `git push -u origin feature/x` |
| **Créer une Pull Request** puis **Complete** | `git checkout main` + `git merge --no-ff feature/x -m "…"` + `git push` |

👉 **La bulle du graphe, c'est la PR.** Le bouton *Complete* exécute un `merge --no-ff` côté serveur : c'est
exactement le même geste, fait par la plateforme au lieu de l'être à la main.

⚠️ **On ne « répare » pas un commit déjà poussé** pour lui ajouter une bulle : il faudrait réécrire l'historique
(`push --force`), infiniment plus risqué qu'une ligne droite dans le graphe. On assume et on continue.

🧠 Règle tenable en solo : **un sujet = une branche = un merge**, même pour 3 lignes de commentaire.
Les exceptions arrivent bien assez tôt toutes seules.
