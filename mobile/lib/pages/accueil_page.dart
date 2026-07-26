import 'package:flutter/material.dart';

/// Contenu de la section « Accueil » (l'index). CONTENU PUR : pas de Scaffold ni
/// d'AppBar — c'est la coquille (`HomeShell`) qui les fournit. Deviendra le dashboard.
class AccueilPage extends StatelessWidget {
  const AccueilPage({super.key});

  @override
  Widget build(BuildContext context) {
    final couleur = Theme.of(context).colorScheme.primary;
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.warehouse_outlined, size: 72, color: couleur),
          const SizedBox(height: 16),
          Text('Bienvenue dans Fluxo',
              style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 8),
          const Text('Choisis une section dans le menu.'),
        ],
      ),
    );
  }
}
