import 'package:flutter/material.dart';

import 'pages/emplacements_page.dart';

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
      // ⚠️ TEMPORAIRE : on affiche l'écran Emplacements pour le tester.
      // Étape suivante (le menu) : l'accueil donnera accès aux DEUX écrans (Articles + Emplacements).
      home: const EmplacementsPage(),
    );
  }
}
