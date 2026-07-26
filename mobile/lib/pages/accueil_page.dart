import 'package:flutter/material.dart';

/// Page d'accueil (l'« index » de l'appli) — la 1re section du menu.
/// Volontairement minimale pour l'instant : un mot de bienvenue.
/// Deviendra le **dashboard** plus tard (cf. vision : dashboard à widgets).
class AccueilPage extends StatelessWidget {
  const AccueilPage({super.key});

  @override
  Widget build(BuildContext context) {
    final couleur = Theme.of(context).colorScheme.primary;
    return Scaffold(
      appBar: AppBar(title: const Text('Accueil')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.warehouse_outlined, size: 72, color: couleur),
            const SizedBox(height: 16),
            Text('Bienvenue dans Fluxo',
                style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            const Text('Choisis une section dans le menu à gauche.'),
          ],
        ),
      ),
    );
  }
}
