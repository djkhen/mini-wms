/// Modèle Article — le miroir Dart du DTO JSON renvoyé par l'API `GET /articles`.
///
/// On ne mappe QUE ce que l'API expose (le contrat `ArticleDto` côté backend) :
/// pas de logique, juste des champs + une fabrique `fromJson`. Les champs sont
/// `final` (immuable) — bon réflexe Flutter/Dart.
class Article {
  final int id;
  final String reference;
  final String designation;
  final String? description; // nullable : la description est optionnelle côté backend
  final String unite;
  final String tracabilite; // "AUCUN" | "LOT" | "SERIE" (l'enum backend, en texte)
  final bool actif;

  const Article({
    required this.id,
    required this.reference,
    required this.designation,
    required this.description,
    required this.unite,
    required this.tracabilite,
    required this.actif,
  });

  /// Construit un Article depuis le JSON décodé (une Map fournie par Dio).
  factory Article.fromJson(Map<String, dynamic> json) {
    return Article(
      id: json['id'] as int,
      reference: json['reference'] as String,
      designation: json['designation'] as String,
      description: json['description'] as String?,
      unite: json['unite'] as String,
      tracabilite: json['tracabilite'] as String,
      actif: json['actif'] as bool,
    );
  }
}
