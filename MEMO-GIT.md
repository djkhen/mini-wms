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
