# 🌿 Aide-mémoire Git — mini-wms

> Les commandes git qu'on utilise vraiment sur le projet, notées au fur et à mesure.
> À compléter à chaque fois qu'une commande revient.

---

# ⭐ AU TRAVAIL — workflow par PULL REQUEST (à lire en premier)

> Le workflow **B** : c'est **la plateforme** (Azure DevOps) qui fait le merge, pas moi.
> Ici la branche d'intégration s'appelle **`develop`** (même rôle que `main` sur mini-wms).
> *(Le workflow **A**, tout en local, est décrit plus bas — voir « DEUX façons d'intégrer une feature ».)*

### 1. Partir d'un `develop` À JOUR
```
git checkout develop
git pull                              ← sinon je pars d'une base vieille de 3 jours
git checkout -b feature/x             ← je crée MA ligne de travail
```

### 2. Travailler
```
git add .
git commit -m "mon message"           ← ⚠️ le -m est OBLIGATOIRE (sinon éditeur qui s'ouvre)
```
👉 Le commit est sur **`feature/x` LOCAL** — `develop` n'en sait encore **rien**.

### 3. Publier la branche
```
git push -u origin feature/x          ← ⚠️ le NOM de la branche est requis
```
👉 Envoie `feature/x` → `origin/feature/x`. **Ni PR, ni merge** : juste une sauvegarde sur le serveur.
Le `-u` ne se met **qu'au premier push** de la branche ; ensuite `git push` tout court suffit.

### 4. Ouvrir la PR (dans l'interface)
👉 C'est la **demande** de fusionner `feature/x` dans `develop`. Rien ne bouge tant qu'elle n'est pas *Complete*.

### 5. Si des collègues ont livré entre-temps → se remettre à jour
```
git pull origin develop               ← DEPUIS feature/x : ramène develop DANS ma branche
   ... je résous les conflits ...
git push                              ← met la PR à JOUR automatiquement (pas de nouvelle PR)
```
⚠️ **Piège** : `git merge develop` fusionnerait mon `develop` **LOCAL**, peut-être périmé !
`git pull origin develop` (= `fetch` + `merge`) garantit le **vrai** `develop`, celui du serveur.

### 6. ⏸️ ON S'ARRÊTE LÀ — la PR est asynchrone
Elle attend une **relecture** et/ou la **CI**. On ne reste pas devant : **on passe à la tâche suivante.**

- **Tâche indépendante** (cas normal) → repartir de `develop` à jour :
  ```
  git checkout develop
  git pull
  git checkout -b feature/y
  ```
- **Tâche qui dépend de `feature/x`** (encore en PR) → *branches empilées*, à éviter si possible :
  ```
  git checkout -b feature/y feature/x
  ```
  (`feature/y` traînera les commits de `x` dans sa PR tant que celle de `x` n'est pas passée.)

⚠️ **Avant tout `checkout`, tout doit être COMMITÉ**, sinon git refuse ou emporte les modifs avec lui.
Au milieu de quelque chose ? → `git stash` … puis `git stash pop` en revenant.

### 7. Plus tard, quand la PR est *Complete* → rapatrier
```
git checkout develop
git pull                              ← récupère le merge fait par LE SERVEUR
git branch -d feature/x               ← ménage (la branche a fait son travail)
```
💡 Azure DevOps / GitHub proposent une case **« supprimer la branche après le merge »** : la cocher évite de le faire à la main.

🧠 **En une phrase** : *je pousse, j'ouvre la PR, je repars de `develop` à jour sur la tâche suivante — et je
reviens chercher le résultat quand la PR est passée.*

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

---

## 🔀 DEUX façons d'intégrer une feature — ne jamais les mélanger

**Le merge n'est JAMAIS optionnel : il est seulement DÉPLACÉ.** Soit je le fais en local, soit la plateforme
(GitHub / Azure DevOps) le fait via la **Pull Request**. `git pull` ne *crée* aucun merge de ma feature :
il **récupère** un merge que quelqu'un a déjà fait.

| | **A — tout en local** (nos sessions mini-wms) | **B — par PR** (workflow du travail, Azure DevOps) |
|---|---|---|
| 1 | `git checkout -b feature/x` | `git checkout -b feature/x` |
| 2 | `git commit -m "…"` | `git commit -m "…"` |
| 3 | `git push -u origin feature/x` *(optionnel)* | `git push -u origin feature/x` ← **obligatoire** |
| 4 | `git checkout main` puis `git merge --no-ff feature/x -m "…"` | **PR → Merge / Complete** (le serveur fait le merge) |
| 5 | `git push` | `git checkout main` puis `git pull` |

⚠️ **Sans PR, un `pull` sur `main` ne ramène RIEN de ma feature** — `git pull` = `git fetch` + `git merge origin/main`,
il ne va jamais chercher `origin/feature/x`. Ce n'est pas une régression pour autant : `main` reste simplement
inchangé, et **le travail n'est jamais perdu** — il attend sagement dans la branche, intégrable des semaines plus tard.

⚠️ **Ne pas cumuler A et B** (merge local **+** PR) → deux merges du même travail.

---

## ⬇️ Synchroniser SA branche avec `main` — le merge dans l'AUTRE sens

Pendant que je travaille sur `feature/x`, `main` avance (PR d'un collègue mergée). Avant de livrer, je ramène
`main` **dans ma branche** pour découvrir les conflits **chez moi** :
```
git pull origin main          ← depuis feature/x
```

**Pourquoi c'est une bonne pratique** :
1. Les conflits se résolvent **dans ma branche**, où ça ne bloque personne — au lieu de coincer une PR attendue.
2. Je vérifie que mon code marche **avec** le travail des autres, pas seulement avec le `main` d'il y a 3 jours.
3. Plus j'attends, plus le conflit grossit → synchroniser **souvent** coûte moins cher.

⚠️ **Le piège de syntaxe** (les deux ne font PAS la même chose) :
- `git pull` **seul** sur `feature/x` → ramène `origin/feature/x` (ma propre branche distante). Inutile si je suis seul dessus.
- `git pull origin main` → ramène **`main`** dans ma feature. ✅ **C'est celui-là.**

🧠 **Les 2 sens du merge** :
- **`main` → ma feature** (`git pull origin main`) = *je me mets à jour / je reste compatible.* **Autant de fois que je veux.**
- **ma feature → `main`** (PR, ou `git merge --no-ff`) = *je livre.* **Une seule fois, à la fin.**

---

## 🆘 « J'ai oublié `checkout -b` : j'ai commité sur `main` ! » (vécu le 2026-08-07)

**Symptômes** : le prompt Git Bash affiche `(main)` alors que je croyais être sur ma branche, et le push échoue :
```
error: src refspec docs/mon-sujet does not match any
```
→ traduction : **cette branche n'existe pas**. Mes commits sont sur `main` local.

**La clé** : une branche est une **ÉTIQUETTE posée sur un commit**, pas une copie. Je pose une 2ᵉ étiquette
ici, puis je **recule** celle de `main`. **Les commits ne bougent jamais.**
```
AVANT :   ●──●──● ← main            (origin/main est resté 2 commits en arrière)

APRÈS :   ●──●──● ← docs/mon-sujet
             ↑
          main = origin/main
```

**Les 4 commandes — l'ORDRE fait la sécurité** :
```
git checkout -b docs/mon-sujet        ← pose l'étiquette ICI (capture les commits) + s'y déplace
git push -u origin docs/mon-sujet     ← ⭐ met les commits en SÉCURITÉ sur GitHub AVANT tout reset
git checkout main
git reset --hard origin/main          ← recule l'étiquette main à l'état du serveur
```
Puis le cycle normal reprend, et le merge crée enfin la bulle :
```
git merge --no-ff docs/mon-sujet -m "merge: ..."
git push
```

⚠️ **`git reset --hard` est la commande la plus destructrice de git** : elle supprime définitivement les
modifications **non commitées**. Ne l'utiliser QUE si les **2 conditions** sont réunies :
1. `git status` **totalement propre** (aucune ligne de fichier) ;
2. les commits abandonnés sont **récupérables ailleurs** (branche créée **ET** poussée → d'où l'ordre ci-dessus).

**Jamais** sur une branche partagée déjà poussée.

💡 **Après le reset, mes fichiers semblent revenus en arrière → c'est NORMAL**, pas une perte : le répertoire
de travail **suit la branche**. `main` pointe sur un commit antérieur, donc git réécrit les fichiers en
conséquence. Le `merge` les ramène instantanément.

🧠 **Prévention** : le prompt Git Bash affiche la branche **entre parenthèses**. Voir `(main)` avant de
commiter = **STOP, je crée ma branche d'abord**. Et `git checkout -b` **emporte** les modifications en
cours avec lui : on peut donc créer la branche même après avoir commencé à travailler.

---

## 💥 « Mon commit a SUPPRIMÉ des lignes que je n'ai pas touchées ! » — l'IDE écrase le fichier

**Vécu le 2026-08-07** : un commit affiche `1 file changed, 115 deletions(-)` alors qu'on voulait **ajouter**
du texte. Le fichier commité était une **version périmée**.

**La cause** : le fichier était **ouvert dans l'IDE** (IntelliJ / VS Code) avec l'ancien contenu **en mémoire**.
Quand git modifie le fichier sur le disque (`reset`, `checkout`, `merge`) puis que l'IDE sauvegarde — auto-save
ou Ctrl+S — **l'IDE écrase le disque avec son buffer périmé**. Git commite alors sagement ce qu'il voit.

**Le réflexe qui évite tout** :
> ⭐ **FERMER (ou recharger) dans l'IDE les fichiers qu'une commande git va modifier.**

**Détecter** — toujours regarder le résumé après un commit :
```
1 file changed, 115 deletions(-)     ← ⚠️ QUE des suppressions alors que j'ajoutais ?!
```
Et **avant** de commiter, vérifier ce qui part vraiment :
```
git diff --stat            (non préparé)   ·   git diff --staged --stat   (préparé)
```

**Réparer** (sans réécrire l'historique, même si le commit est déjà poussé) :
```
git checkout main -- le-fichier.md        ← récupère la BONNE version depuis une autre branche
   ... on refait les modifs voulues ...
git commit -m "fix: restaure les lignes ecrasees + ..."
```
🧠 `git checkout <branche> -- <fichier>` = **« reprends CE fichier tel qu'il est là-bas »**. Rien n'est jamais
perdu : chaque version vit dans un commit.
