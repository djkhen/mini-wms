// Smoke test minimal : l'appli se construit et affiche le titre de l'écran Articles.
// (Le FutureBuilder passe par l'état "chargement" — pas d'appel réseau réel ici.)

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/main.dart';

void main() {
  testWidgets('L\'appli démarre sur l\'écran Articles', (WidgetTester tester) async {
    await tester.pumpWidget(const FluxoApp());

    // L'AppBar de l'écran d'accueil affiche "Articles".
    expect(find.text('Articles'), findsOneWidget);
    // Pendant le chargement, un indicateur de progression est visible.
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
