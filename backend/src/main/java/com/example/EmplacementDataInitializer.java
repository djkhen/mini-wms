package com.example;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Insère quelques emplacements de démonstration au démarrage, uniquement si la
 * table est vide (DONNÉES FICTIVES). Même principe que ArticleDataInitializer
 * de gestion-stock : on évite de dupliquer les données à chaque redémarrage et
 * on illustre la persistance.
 *
 * Les emplacements couvrent les différents types (quai, réception, stockage,
 * tri, expédition) pour pouvoir tester filtres et flux dès la première démo.
 */
@ApplicationScoped
public class EmplacementDataInitializer {

    private static final Logger LOG = Logger.getLogger(EmplacementDataInitializer.class);

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (Emplacement.count() == 0) {
            LOG.info("Table emplacements vide : insertion des emplacements de démonstration.");
            creer("QUAI-01", "Quai de réception 1", TypeEmplacement.QUAI, "QUAI", null, null, null);
            creer("REC-A-01", "Zone réception A", TypeEmplacement.RECEPTION, "A", "01", null, null);
            creer("A-01-01-1", "Rack A allée 1 travée 1 niveau 1", TypeEmplacement.STOCKAGE, "A", "01", "01", "1");
            creer("A-01-01-2", "Rack A allée 1 travée 1 niveau 2", TypeEmplacement.STOCKAGE, "A", "01", "01", "2");
            creer("A-01-02-1", "Rack A allée 1 travée 2 niveau 1", TypeEmplacement.STOCKAGE, "A", "01", "02", "1");
            creer("TRI-01", "Zone de tri qualité", TypeEmplacement.TRI, "TRI", null, null, null);
            creer("EXP-01", "Zone d'expédition", TypeEmplacement.EXPEDITION, "EXP", null, null, null);
        } else {
            LOG.infof("Table emplacements déjà peuplée (%d emplacements), pas d'insertion.", Emplacement.count());
        }
    }

    private void creer(String code, String libelle, TypeEmplacement type,
                       String zone, String allee, String travee, String niveau) {
        Emplacement e = new Emplacement();
        e.code = code;
        e.libelle = libelle;
        e.type = type;
        e.zone = zone;
        e.allee = allee;
        e.travee = travee;
        e.niveau = niveau;
        e.actif = true;
        e.persist();
    }
}
