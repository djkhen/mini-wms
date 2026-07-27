import 'package:flutter/material.dart';

import '../models/article.dart';
import '../services/api_service.dart';
import '../services/locator.dart';
import 'article_detail_page.dart';

/// Contenu de la section « Articles » : la LISTE (`GET /articles`).
/// CONTENU PUR — pas de Scaffold ni d'AppBar (fournis par la coquille `HomeShell`).
class ArticlesPage extends StatefulWidget {
  const ArticlesPage({super.key});

  @override
  State<ArticlesPage> createState() => _ArticlesPageState();
}

class _ArticlesPageState extends State<ArticlesPage> {
  final ApiService _api = getIt<ApiService>(); // instance PARTAGÉE (annuaire), plus de `ApiService()`
  late Future<List<Article>> _futureArticles;

  @override
  void initState() {
    super.initState();
    _futureArticles = _api.getArticles(); // chargé à l'affichage de la section
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<Article>>(
      future: _futureArticles,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text('Erreur de chargement :\n${snapshot.error}',
                  textAlign: TextAlign.center),
            ),
          );
        }
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
    );
  }

  /// Une ligne cliquable → ouvre la fiche détail (Navigator.push).
  Widget _tuileArticle(Article a) {
    return ListTile(
      leading: CircleAvatar(child: Text(a.tracabilite.substring(0, 1))),
      title: Text('${a.reference} — ${a.designation}'),
      subtitle: Text('Unité : ${a.unite}  ·  Traçabilité : ${a.tracabilite}'),
      trailing: a.actif
          ? const Icon(Icons.check_circle, color: Colors.green)
          : const Icon(Icons.cancel, color: Colors.grey),
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => ArticleDetailPage(article: a)),
        );
      },
    );
  }
}
