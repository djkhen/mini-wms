import 'package:flutter/material.dart';

import '../models/article.dart';

/// Écran DÉTAIL d'un article, ouvert via `Navigator.push` (EMPILÉ par-dessus la liste).
///
/// 🧭 Concept : contrairement au menu (qui échange le corps), ici on **empile** un
/// nouvel écran → l'AppBar affiche AUTOMATIQUEMENT une flèche « retour » (← = `Navigator.pop`).
///
/// On reçoit l'`Article` par le constructeur : on a déjà l'objet (depuis la liste),
/// donc pas besoin de re-appeler l'API.
class ArticleDetailPage extends StatelessWidget {
  final Article article;

  const ArticleDetailPage({super.key, required this.article});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(article.reference), // la flèche retour ← est ajoutée toute seule
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _champ(context, 'Référence', article.reference),
          _champ(context, 'Désignation', article.designation),
          _champ(context, 'Description', article.description ?? '—'),
          _champ(context, 'Unité', article.unite),
          _champ(context, 'Traçabilité', article.tracabilite),
          _champ(context, 'Actif', article.actif ? 'Oui' : 'Non'),
        ],
      ),
    );
  }

  /// Affiche un couple libellé (gris, petit) / valeur (grande).
  Widget _champ(BuildContext context, String libelle, String valeur) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(libelle,
              style: theme.textTheme.labelMedium?.copyWith(color: Colors.grey)),
          const SizedBox(height: 2),
          Text(valeur, style: theme.textTheme.bodyLarge),
        ],
      ),
    );
  }
}
