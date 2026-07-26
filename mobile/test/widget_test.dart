// Smoke test minimal : l'appli se construit et affiche le titre de l'écran Articles.
// (Le FutureBuilder passe par l'état "chargement" — pas d'appel réseau réel ici.)

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/main.dart';

void main() {
  testWidgets('L\'appli démarre et charge un écran de liste', (WidgetTester tester) async {
    await tester.pumpWidget(const FluxoApp());

    // Au démarrage, l'écran de liste affiche un indicateur de chargement
    // (indépendant de QUEL écran est en accueil).
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
