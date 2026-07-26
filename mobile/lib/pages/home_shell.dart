import 'package:flutter/material.dart';

import '../navigation/section.dart';
import 'accueil_page.dart';
import 'articles_page.dart';
import 'emplacements_page.dart';

/// La COQUILLE de l'appli (= `home`). Elle possède le `Scaffold` + l'AppBar + le menu,
/// et affiche la section choisie. Les pages, elles, ne sont que du CONTENU (pas de Scaffold).
///
/// ⭐ ADAPTATIF : `LayoutBuilder` mesure la largeur dispo →
///   - large (desktop/web/tablette) → `NavigationRail` (barre latérale)
///   - étroit (mobile) → `Drawer` (menu tiroir ☰)
/// Les deux menus se construisent à partir de la MÊME liste `_sections`.
class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  // Le menu = une LISTE de données. Ajouter un écran = ajouter une Section ici.
  static const List<Section> _sections = [
    Section(libelle: 'Accueil', icone: Icons.home_outlined, page: AccueilPage()),
    Section(libelle: 'Articles', icone: Icons.inventory_2_outlined, page: ArticlesPage()),
    Section(libelle: 'Emplacements', icone: Icons.place_outlined, page: EmplacementsPage()),
  ];

  // Seuil de bascule : sous 600 px de large = mobile (Drawer) ; au-dessus = rail.
  static const double _seuilLarge = 600;

  void _choisir(int i) => setState(() => _index = i);

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final estLarge = constraints.maxWidth >= _seuilLarge;
        final section = _sections[_index];

        return Scaffold(
          appBar: AppBar(title: Text(section.libelle)),
          // Drawer UNIQUEMENT en étroit → l'AppBar affiche alors le ☰ automatiquement.
          // En large, le menu est le NavigationRail (pas de drawer).
          drawer: estLarge ? null : _drawer(context),
          body: estLarge
              ? Row(
                  children: [
                    _rail(),
                    const VerticalDivider(width: 1),
                    Expanded(child: section.page),
                  ],
                )
              : section.page,
        );
      },
    );
  }

  /// Menu LARGE — `NavigationRail` (barre latérale), construit depuis `_sections`.
  Widget _rail() {
    return NavigationRail(
      selectedIndex: _index,
      onDestinationSelected: _choisir,
      labelType: NavigationRailLabelType.all,
      destinations: [
        for (final s in _sections)
          NavigationRailDestination(icon: Icon(s.icone), label: Text(s.libelle)),
      ],
    );
  }

  /// Menu ÉTROIT — `Drawer` (tiroir), construit depuis la MÊME `_sections`.
  Widget _drawer(BuildContext context) {
    return Drawer(
      child: ListView(
        padding: EdgeInsets.zero,
        children: [
          const DrawerHeader(
            child: Center(
              child: Text('Fluxo',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
            ),
          ),
          for (int i = 0; i < _sections.length; i++)
            ListTile(
              leading: Icon(_sections[i].icone),
              title: Text(_sections[i].libelle),
              selected: i == _index,
              onTap: () {
                _choisir(i);
                Navigator.pop(context); // ferme le tiroir après la sélection
              },
            ),
        ],
      ),
    );
  }
}
