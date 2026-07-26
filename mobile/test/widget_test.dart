// Smoke test minimal : l'appli se construit et affiche le titre de l'écran Articles.
// (Le FutureBuilder passe par l'état "chargement" — pas d'appel réseau réel ici.)

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/main.dart';

void main() {
  testWidgets('L\'appli démarre sur la coquille avec le menu', (WidgetTester tester) async {
    await tester.pumpWidget(const FluxoApp());

    // Le menu latéral (NavigationRail) est présent.
    expect(find.byType(NavigationRail), findsOneWidget);
    // La section par défaut est l'accueil.
    expect(find.text('Bienvenue dans Fluxo'), findsOneWidget);
  });
}
