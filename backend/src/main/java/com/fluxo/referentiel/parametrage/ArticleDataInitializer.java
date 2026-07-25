package com.fluxo.referentiel.parametrage;

import com.fluxo.referentiel.domain.Article;
import com.fluxo.referentiel.domain.ModeTracabilite;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Insère quelques articles de démonstration au démarrage, UNIQUEMENT si la table
 * est vide (DONNÉES FICTIVES). Même pattern qu'EmplacementDataInitializer.
 *
 * Le jeu couvre les 3 modes de traçabilité (AUCUN / LOT / SERIE) + 1 article
 * INACTIF, pour exercer les filtres (?tracabilite= et ?actif=) dès la 1re démo.
 * Thème bois (métier cible) : négoce/quincaillerie (sans traçabilité), panneaux et
 * sections (suivi par lot), caisse fabriquée (suivi par n° de série).
 *
 * ⚠️ Démo/dev uniquement : à désactiver en prod via un profil Quarkus (un client
 * arrive avec SES propres articles, cf. conception §8bis « reprise de données »).
 */
@ApplicationScoped
public class ArticleDataInitializer {

    private static final Logger LOG = Logger.getLogger(ArticleDataInitializer.class);

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (Article.count() == 0) {
            LOG.info("Table articles vide : insertion des articles de démonstration.");
            // -- Traçabilité AUCUN : négoce / quincaillerie, aucun suivi lot ou série --
            creer("VIS-6x80", "Vis bois 6x80", "Vis à tête fraisée, boîte de 200", "piece", ModeTracabilite.AUCUN, true);
            creer("COLLE-D3", "Colle vinylique D3", "Colle bois hydrofuge, bidon 5 kg", "kg", ModeTracabilite.AUCUN, true);
            // -- Traçabilité LOT : panneaux et sections bois (suivi par lot fournisseur) --
            creer("CTP-15", "Contreplaqué okoumé 15 mm", "Panneau CTP 15 mm, 2500x1220", "m2", ModeTracabilite.LOT, true);
            creer("SAPIN-45x95", "Sapin raboté 45x95", "Section sapin séché, longueur 3 m", "m", ModeTracabilite.LOT, true);
            // -- Traçabilité SERIE : caisse fabriquée (chaque exemplaire est unique) --
            creer("CAISSE-T16", "Caisse bois modèle T16", "Caisse fabriquée, n° de série par exemplaire", "piece", ModeTracabilite.SERIE, true);
            // -- Article INACTIF : référence retirée du catalogue (désactivée, pas supprimée) --
            creer("OSB-9-OLD", "OSB 9 mm (ancien)", "Référence remplacée par OSB-12", "m2", ModeTracabilite.LOT, false);
            LOG.infof("%d articles de démonstration insérés.", Article.count());
        } else {
            LOG.infof("Table articles déjà peuplée (%d articles), pas d'insertion.", Article.count());
        }
    }

    private void creer(String reference, String designation, String description,
                       String unite, ModeTracabilite tracabilite, boolean actif) {
        Article a = new Article();
        a.reference = reference;
        a.designation = designation;
        a.description = description;
        a.unite = unite;
        a.tracabilite = tracabilite;
        a.actif = actif;
        a.persist();
    }
}
