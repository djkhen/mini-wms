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
