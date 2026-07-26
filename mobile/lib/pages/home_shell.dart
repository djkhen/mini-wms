import 'package:flutter/material.dart';

import '../navigation/section.dart';
import 'accueil_page.dart';
import 'articles_page.dart';
import 'emplacements_page.dart';

/// La COQUILLE de l'appli (= la page d'accueil / `home`) : le menu `NavigationRail`
/// à gauche + la section choisie à droite.
///
/// `StatefulWidget` car l'écran a un ÉTAT : l'index de la section sélectionnée.
/// Cliquer une icône → `setState` → le corps affiche l'écran correspondant.
class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _indexCourant = 0;

  // ⭐ LE MENU = UNE LISTE DE DONNÉES. Ajouter un écran = ajouter une Section ici.
  // (Plus tard : grouper par domaine + Drawer sur mobile — cf. TODO archi Flutter.)
  static const List<Section> _sections = [
    Section(libelle: 'Accueil', icone: Icons.home_outlined, page: AccueilPage()),
    Section(libelle: 'Articles', icone: Icons.inventory_2_outlined, page: ArticlesPage()),
    Section(libelle: 'Emplacements', icone: Icons.place_outlined, page: EmplacementsPage()),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: [
          // --- Le menu à gauche, construit À PARTIR de la liste _sections ---
          NavigationRail(
            selectedIndex: _indexCourant,
            onDestinationSelected: (i) => setState(() => _indexCourant = i),
            labelType: NavigationRailLabelType.all, // libellés toujours visibles
            leading: const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Text('Fluxo',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            ),
            destinations: [
              for (final s in _sections)
                NavigationRailDestination(
                  icon: Icon(s.icone),
                  label: Text(s.libelle),
                ),
            ],
          ),
          const VerticalDivider(width: 1),
          // --- Le contenu à droite : l'écran de la section sélectionnée ---
          Expanded(child: _sections[_indexCourant].page),
        ],
      ),
    );
  }
}
