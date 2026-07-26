import 'package:flutter/material.dart';

/// Une section de navigation = une entrée du menu : un libellé, une icône, un écran.
///
/// Le menu se construit à partir d'une **LISTE de `Section`** → ajouter un écran au
/// menu = ajouter une ligne dans cette liste (pas de code en dur). C'est ce qui
/// permettra, plus tard, de grouper par domaine et d'alimenter aussi un Drawer
/// (mobile) à partir des mêmes données (cf. TODO « Architecture Flutter à venir »).
class Section {
  final String libelle;
  final IconData icone;
  final Widget page;

  const Section({
    required this.libelle,
    required this.icone,
    required this.page,
  });
}
