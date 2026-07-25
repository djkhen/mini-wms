import 'package:flutter/material.dart';

import 'pages/articles_page.dart';

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
      // Premier écran affiché au lancement : la liste des articles.
      home: const ArticlesPage(),
    );
  }
}
