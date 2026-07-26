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
