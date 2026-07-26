import 'package:flutter/material.dart';

import '../models/emplacement.dart';
import '../services/api_service.dart';

/// Écran : la LISTE des emplacements (`GET /emplacements`).
/// Copie conforme d'`ArticlesPage`, adaptée aux champs d'un emplacement.
class EmplacementsPage extends StatefulWidget {
  const EmplacementsPage({super.key});

  @override
  State<EmplacementsPage> createState() => _EmplacementsPageState();
}

class _EmplacementsPageState extends State<EmplacementsPage> {
  final ApiService _api = ApiService();
  late Future<List<Emplacement>> _futureEmplacements;

  @override
  void initState() {
    super.initState();
    _futureEmplacements = _api.getEmplacements();
  }

  void _rafraichir() {
    setState(() {
      _futureEmplacements = _api.getEmplacements();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Emplacements'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Rafraîchir',
            onPressed: _rafraichir,
          ),
        ],
      ),
      body: FutureBuilder<List<Emplacement>>(
        future: _futureEmplacements,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
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
          final emplacements = snapshot.data ?? const [];
          if (emplacements.isEmpty) {
            return const Center(child: Text('Aucun emplacement.'));
          }
          return ListView.separated(
            itemCount: emplacements.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (context, index) => _tuileEmplacement(emplacements[index]),
          );
        },
      ),
    );
  }

  Widget _tuileEmplacement(Emplacement e) {
    // Sous-titre : le type + la zone (si présente).
    final zone = e.zone != null ? '  ·  Zone : ${e.zone}' : '';
    return ListTile(
      leading: const Icon(Icons.place_outlined),
      title: Text('${e.code}${e.libelle != null ? ' — ${e.libelle}' : ''}'),
      subtitle: Text('Type : ${e.type}$zone'),
      trailing: e.actif
          ? const Icon(Icons.check_circle, color: Colors.green)
          : const Icon(Icons.cancel, color: Colors.grey),
    );
  }
}
