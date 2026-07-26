/// Modèle Emplacement — miroir Dart du DTO JSON de `GET /emplacements`.
/// Même principe qu'`Article` : champs `final` + fabrique `fromJson`.
/// (Champs nullables = optionnels côté backend : libelle, zone, allée, travée, niveau.)
class Emplacement {
  final int id;
  final String code;
  final String? libelle;
  final String type; // QUAI | RECEPTION | STOCKAGE | EXPEDITION | TRI (l'enum backend, en texte)
  final String? zone;
  final String? allee;
  final String? travee;
  final String? niveau;
  final bool actif;

  const Emplacement({
    required this.id,
    required this.code,
    required this.libelle,
    required this.type,
    required this.zone,
    required this.allee,
    required this.travee,
    required this.niveau,
    required this.actif,
  });

  factory Emplacement.fromJson(Map<String, dynamic> json) {
    return Emplacement(
      id: json['id'] as int,
      code: json['code'] as String,
      libelle: json['libelle'] as String?,
      type: json['type'] as String,
      zone: json['zone'] as String?,
      allee: json['allee'] as String?,
      travee: json['travee'] as String?,
      niveau: json['niveau'] as String?,
      actif: json['actif'] as bool,
    );
  }
}
