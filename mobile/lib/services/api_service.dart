import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart'; // kIsWeb, defaultTargetPlatform (marchent PARTOUT, contrairement à dart:io)

import '../models/article.dart';
import '../models/emplacement.dart';

/// URL de base du backend, choisie SELON LA PLATEFORME.
/// ⚠️ Sur l'émulateur Android, `localhost` = l'émulateur, PAS le PC → il faut `10.0.2.2`
/// pour joindre le backend qui tourne sur l'hôte. (Un vrai téléphone = l'IP LAN du PC : plus tard.)
/// Web / desktop : `localhost` convient. NB : pas de CORS sur mobile (ce n'est pas un navigateur).
String _urlDeBase() {
  if (kIsWeb) return 'http://localhost:8080'; // web (Chrome)
  if (defaultTargetPlatform == TargetPlatform.android) {
    return 'http://10.0.2.2:8080'; // émulateur Android
  }
  return 'http://localhost:8080'; // desktop / iOS
}

/// Point d'accès UNIQUE à l'API backend (Quarkus).
///
/// On centralise Dio ici : l'URL de base et (plus tard) les intercepteurs
/// (token Keycloak, gestion d'erreurs, logs) ne se configurent qu'à UN endroit.
/// C'est l'équivalent Flutter du "un seul endroit qui sait parler au serveur".
class ApiService {
  ApiService()
      : _dio = Dio(
          BaseOptions(
            baseUrl: _urlDeBase(), // localhost (web/desktop) ou 10.0.2.2 (émulateur Android)
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
