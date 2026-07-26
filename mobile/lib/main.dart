import 'package:flutter/material.dart';

import 'pages/home_shell.dart';

void main() {
  runApp(const FluxoApp());
}

/// Racine de l'application Fluxo (mobile/web/desktop — même code).
class FluxoApp extends StatelessWidget {
  const FluxoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Fluxo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        useMaterial3: true,
      ),
      // Accueil = la COQUILLE (menu NavigationRail + contenu) : donne accès à toutes les sections.
      home: const HomeShell(),
    );
  }
}
