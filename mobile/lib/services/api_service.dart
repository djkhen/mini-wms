import 'package:dio/dio.dart';

import '../models/article.dart';
import '../models/emplacement.dart';

/// Point d'accès UNIQUE à l'API backend (Quarkus, http://localhost:8080).
///
/// On centralise Dio ici : l'URL de base et (plus tard) les intercepteurs
/// (token Keycloak, gestion d'erreurs, logs) ne se configurent qu'à UN endroit.
/// C'est l'équivalent Flutter du "un seul endroit qui sait parler au serveur".
class ApiService {
  ApiService()
      : _dio = Dio(
          BaseOptions(
            // ⚠️ En web (Chrome) sur ta machine, le backend est sur localhost:8080.
            // Lancer l'appli sur --web-port=5000 (déjà autorisé par le CORS backend).
            baseUrl: 'http://localhost:8080',
            connectTimeout: const Duration(seconds: 5),
            receiveTimeout: const Duration(seconds: 5),
          ),
        );

  final Dio _dio;

  /// `GET /articles` -> la liste des articles.
  /// Dio décode déjà le JSON : `reponse.data` est une List de Map.
  Future<List<Article>> getArticles() async {
    final reponse = await _dio.get('/articles');
    final data = reponse.data as List<dynamic>;
    return data
        .map((json) => Article.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  /// `GET /emplacements` -> la liste des emplacements. Même patron que getArticles().
  Future<List<Emplacement>> getEmplacements() async {
    final reponse = await _dio.get('/emplacements');
    final data = reponse.data as List<dynamic>;
    return data
        .map((json) => Emplacement.fromJson(json as Map<String, dynamic>))
        .toList();
  }
}
