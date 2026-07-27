import 'package:get_it/get_it.dart';

import 'api_service.dart';

/// L'ANNUAIRE de l'appli (service locator GetIt). On y enregistre les objets
/// PARTAGÉS, et on les récupère partout avec `getIt<...>()`.
final GetIt getIt = GetIt.instance;

/// Enregistre les dépendances. Appelé UNE seule fois au démarrage (dans `main.dart`).
void setupLocator() {
  // registerLazySingleton = UNE seule instance d'ApiService, créée à la 1re demande,
  // puis réutilisée partout (au lieu d'un `ApiService()` par page).
  getIt.registerLazySingleton<ApiService>(() => ApiService());
}
