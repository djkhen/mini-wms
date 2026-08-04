package com.fluxo.init;

import com.fluxo.referentiel.domain.Tiers;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Insère quelques tiers de démonstration au démarrage, UNIQUEMENT si la table est
 * vide (DONNÉES FICTIVES). Regroupé dans `com.fluxo.init` avec les autres seeds.
 *
 * Le jeu couvre les 3 rôles (fournisseur / transporteur / client), un tiers qui
 * CUMULE deux rôles (fournisseur ET transporteur → l'intérêt de la table unique),
 * et un tiers INACTIF — pour exercer les filtres (?estFournisseur=, ?actif=…).
 *
 * ⚠️ Démo/dev uniquement : à désactiver en prod via un profil Quarkus (un client
 * arrive avec SES propres partenaires).
 */
@ApplicationScoped
public class TiersDataInitializer {

    private static final Logger LOG = Logger.getLogger(TiersDataInitializer.class);

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (Tiers.count() == 0) {
            LOG.info("Table tiers vide : insertion des tiers de démonstration.");
            //     code                raisonSociale            fourn.  transp. client  actif
            creer("SCIERIE-DUPONT",    "Scierie Dupont SARL",   true,   false,  false,  true);
            creer("PANNEAUX-OUEST",    "Panneaux de l'Ouest",   true,   false,  false,  true);
            creer("TRANS-RAPIDE",      "Transports Rapide",     false,  true,   false,  true);
            creer("BOIS-EXPRESS",      "Bois Express",          true,   true,   false,  true);  // fournisseur ET transporteur
            creer("MENUISERIE-MARTIN", "Menuiserie Martin",     false,  false,  true,   true);  // client
            creer("VIEUX-FOUR",        "Ancien Fournisseur",    true,   false,  false,  false); // inactif
            LOG.infof("%d tiers de démonstration insérés.", Tiers.count());
        } else {
            LOG.infof("Table tiers déjà peuplée (%d tiers), pas d'insertion.", Tiers.count());
        }
    }

    private void creer(String code, String raisonSociale,
                       boolean estFournisseur, boolean estTransporteur, boolean estClient, boolean actif) {
        Tiers t = new Tiers();
        t.code = code;
        t.raisonSociale = raisonSociale;
        t.estFournisseur = estFournisseur;
        t.estTransporteur = estTransporteur;
        t.estClient = estClient;
        t.actif = actif;
        t.persist();
    }
}
