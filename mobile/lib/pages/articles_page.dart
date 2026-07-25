import 'package:flutter/material.dart';

import '../models/article.dart';
import '../services/api_service.dart';

/// Écran : la LISTE des articles (`GET /articles`).
///
/// `StatefulWidget` car l'écran a un ÉTAT qui change : le `Future` de chargement,
/// qu'on relance quand on appuie sur "rafraîchir".
class ArticlesPage extends StatefulWidget {
  const ArticlesPage({super.key});

  @override
  State<ArticlesPage> createState() => _ArticlesPageState();
}

class _ArticlesPageState extends State<ArticlesPage> {
  final ApiService _api = ApiService();

  // On garde le Future dans l'état : le FutureBuilder s'y abonne. Le recréer
  // (dans setState) relance l'appel réseau et reconstruit l'écran.
  late Future<List<Article>> _futureArticles;

  @override
  void initState() {
    super.initState();
    _futureArticles = _api.getArticles(); // 1er chargement au démarrage de l'écran
  }

  void _rafraichir() {
    setState(() {
      _futureArticles = _api.getArticles();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Articles'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Rafraîchir',
            onPressed: _rafraichir,
          ),
        ],
      ),
      // FutureBuilder = LE widget pour afficher un résultat asynchrone : il
      // reconstruit l'UI selon l'état du Future (en cours / erreur / données).
      body: FutureBuilder<List<Article>>(
        future: _futureArticles,
        builder: (context, snapshot) {
          // 1) Chargement en cours
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          // 2) Erreur (backend éteint, CORS, réseau...)
          if (snapshot.hasError) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  'Erreur de chargement :\n${snapshot.error}',
                  textAlign: TextAlign.center,
                ),
              ),
            );
          }
          // 3) Données arrivées
          final articles = snapshot.data ?? const [];
          if (articles.isEmpty) {
            return const Center(child: Text('Aucun article.'));
          }
          return ListView.separated(
            itemCount: articles.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (context, index) => _tuileArticle(articles[index]),
          );
        },
      ),
    );
  }

  /// Une ligne de la liste (un article).
  Widget _tuileArticle(Article a) {
    return ListTile(
      // Pastille avec la 1re lettre de la traçabilité (A/L/S) — repère visuel rapide.
      leading: CircleAvatar(child: Text(a.tracabilite.substring(0, 1))),
      title: Text('${a.reference} — ${a.designation}'),
      subtitle: Text('Unité : ${a.unite}  ·  Traçabilité : ${a.tracabilite}'),
      // Coche verte si actif, croix grise si inactif.
      trailing: a.actif
          ? const Icon(Icons.check_circle, color: Colors.green)
          : const Icon(Icons.cancel, color: Colors.grey),
    );
  }
}
