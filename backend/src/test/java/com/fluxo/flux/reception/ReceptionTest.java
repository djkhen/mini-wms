package com.fluxo.flux.reception;

import com.fluxo.flux.domain.Emplacement;
import com.fluxo.flux.domain.Mouvement;
import com.fluxo.referentiel.domain.Article;
import com.fluxo.referentiel.domain.ModeTracabilite;
import com.fluxo.referentiel.domain.Tiers;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Le 1er flux COMPLET : un camion arrive → un document → des mouvements → du stock.
 *
 * Ce que ces tests verrouillent : la réception ne « met pas à jour » un compteur, elle
 * GÉNÈRE des mouvements. Le stock qu'on vérifie ensuite n'est lu nulle part — il est
 * calculé (Mouvement.stockPhysique).
 */
@QuarkusTest
class ReceptionTest {

    private static final String FOURN = "TEST-REC-FOURNISSEUR";
    private static final String ART_SIMPLE = "TEST-REC-PLANCHE";
    private static final String ART_TRACE = "TEST-REC-POUTRE";

    @AfterEach
    void nettoyer() {
        QuarkusTransaction.requiringNew().run(() -> {
            Mouvement.delete("article.reference in ?1", List.of(ART_SIMPLE, ART_TRACE));
            LigneReception.delete("article.reference in ?1", List.of(ART_SIMPLE, ART_TRACE));
            Reception.delete("fournisseur.code", FOURN);
            Article.delete("reference in ?1", List.of(ART_SIMPLE, ART_TRACE));
            Tiers.delete("code", FOURN);
        });
    }

    // ------------------------------------------------------------------
    //  Le geste central : 3 articles reçus = 1 document, 3 mouvements
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Valider une réception de 2 lignes génère 2 mouvements et fait apparaître le stock")
    void valider_genere_les_mouvements_et_fait_apparaitre_le_stock() {
        QuarkusTransaction.requiringNew().run(() -> {
            Emplacement quai = emplacement("QUAI-01");
            Reception r = reception(quai);
            ligne(r, articleSimple(), "150", null, null);
            ligne(r, articleTrace(), "80", "LOT-2026-0345", null);
            r.persist();

            // AVANT : rien en stock, le document ne suffit pas
            assertQuantite("0", Mouvement.stockPhysique(articleSimple(), quai));

            List<Mouvement> mouvements = r.valider();

            assertEquals(2, mouvements.size(), "un mouvement par ligne");
            assertEquals(EtatReception.VALIDEE, r.etat);
            // APRÈS : le stock EXISTE — et il est CALCULÉ, pas lu
            assertQuantite("150", Mouvement.stockPhysique(articleSimple(), quai));
            assertQuantite("80", Mouvement.stockPhysique(articleTrace(), quai));
        });
    }

    // ------------------------------------------------------------------
    //  Le sens : la marchandise vient de l'emplacement virtuel FOURNISSEUR
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Les mouvements générés partent bien de FOURNISSEUR et pointent vers la réception")
    void les_mouvements_partent_du_fournisseur_virtuel() {
        QuarkusTransaction.requiringNew().run(() -> {
            Reception r = reception(emplacement("QUAI-01"));
            ligne(r, articleSimple(), "150", null, null);
            r.persist();

            Mouvement m = r.valider().get(0);

            assertEquals("FOURNISSEUR", m.source.code);
            assertTrue(m.source.type.estVirtuel());
            assertEquals("QUAI-01", m.destination.code);
            // règle 7 : on peut remonter du mouvement au document qui l'a produit
            assertEquals("RECEPTION", m.origineType);
            assertEquals(r.id, m.origineId);
            // la date d'effet est celle du camion, pas l'heure de saisie
            assertEquals(r.dateReception, m.dateEffet);
        });
    }

    // ------------------------------------------------------------------
    //  Une ligne peut être dirigée ailleurs (lot litigieux -> zone de TRI)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Une ligne peut surcharger la destination de l'en-tête (ex. lot litigieux vers TRI)")
    void une_ligne_peut_surcharger_la_destination() {
        QuarkusTransaction.requiringNew().run(() -> {
            Emplacement quai = emplacement("QUAI-01");
            Emplacement tri = emplacement("TRI-01");
            Reception r = reception(quai);
            ligne(r, articleSimple(), "150", null, null);   // suit l'en-tête
            ligne(r, articleTrace(), "80", "LOT-1", tri);   // dirigée ailleurs
            r.persist();

            r.valider();

            assertQuantite("150", Mouvement.stockPhysique(articleSimple(), quai));
            assertQuantite("80", Mouvement.stockPhysique(articleTrace(), tri));
            assertQuantite("0", Mouvement.stockPhysique(articleTrace(), quai));
        });
    }

    // ------------------------------------------------------------------
    //  Traçabilité : pas de lot = pas de réception
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Un article suivi en LOT sans numéro de lot bloque la validation")
    void le_lot_est_obligatoire_pour_un_article_suivi() {
        QuarkusTransaction.requiringNew().run(() -> {
            Reception r = reception(emplacement("QUAI-01"));
            ligne(r, articleTrace(), "80", null, null); // lot manquant
            r.persist();

            IllegalStateException e = assertThrows(IllegalStateException.class, r::valider);
            assertTrue(e.getMessage().contains("numéro de lot"), e.getMessage());
            assertEquals(EtatReception.BROUILLON, r.etat, "la réception ne doit PAS être validée");
        });
    }

    // ------------------------------------------------------------------
    //  On ne range pas dans le vide
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Ranger dans un emplacement VIRTUEL est refusé")
    void on_ne_range_pas_dans_un_emplacement_virtuel() {
        QuarkusTransaction.requiringNew().run(() -> {
            Reception r = reception(emplacement("QUAI-01"));
            ligne(r, articleSimple(), "150", null, emplacement("PERTE"));
            r.persist();

            assertThrows(IllegalStateException.class, r::valider);
        });
    }

    // ------------------------------------------------------------------
    //  Immutabilité du document : on ne valide pas deux fois
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Valider deux fois est refusé (sinon le stock doublerait)")
    void on_ne_valide_pas_deux_fois() {
        QuarkusTransaction.requiringNew().run(() -> {
            Reception r = reception(emplacement("QUAI-01"));
            ligne(r, articleSimple(), "150", null, null);
            r.persist();
            r.valider();

            assertThrows(IllegalStateException.class, r::valider);
            assertQuantite("150", Mouvement.stockPhysique(articleSimple(), emplacement("QUAI-01")));
        });
    }

    // ------------------------------------------------------------------
    //  La pesée : deux mesures INDÉPENDANTES, et leur écart est une info
    // ------------------------------------------------------------------
    @Test
    @DisplayName("L'écart de pesée (camion − somme des lignes) est calculé, jamais déduit")
    void l_ecart_de_pesee_est_une_information_de_controle() {
        QuarkusTransaction.requiringNew().run(() -> {
            Reception r = reception(emplacement("QUAI-01"));
            r.poidsTotalPese = new BigDecimal("1250");        // pont-bascule
            ligne(r, articleSimple(), "150", null, null).poidsPese = new BigDecimal("800");
            ligne(r, articleTrace(), "80", "LOT-1", null).poidsPese = new BigDecimal("420");
            r.persist();

            assertQuantite("1220", r.poidsCumuleDesLignes());
            // 30 kg d'écart : emballages, palettes... une info, pas un bug
            assertQuantite("30", r.ecartDePesee());
        });
    }

    @Test
    @DisplayName("Sans pesée globale, l'écart vaut null (rien à comparer)")
    void pas_de_pesee_globale_pas_d_ecart() {
        QuarkusTransaction.requiringNew().run(() -> {
            Reception r = reception(emplacement("QUAI-01"));
            ligne(r, articleSimple(), "150", null, null);
            r.persist();

            assertNull(r.ecartDePesee());
        });
    }

    // ------------------------------------------------------------------
    //  Le numéro métier
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Le numéro suit le format REC-aa-0000 et ne se répète pas")
    void le_numero_est_genere_au_bon_format() {
        QuarkusTransaction.requiringNew().run(() -> {
            String n1 = Reception.prochainNumero();
            String n2 = Reception.prochainNumero();

            assertTrue(n1.matches("REC-\\d{2}-\\d{4,}"), "format inattendu : " + n1);
            assertNotEquals(n1, n2, "deux appels ne doivent jamais donner le même numéro");
        });
    }

    // ---------------------------- helpers ----------------------------

    private Reception reception(Emplacement arrivee) {
        Reception r = new Reception();
        r.numero = Reception.prochainNumero();
        r.fournisseur = fournisseur();
        r.emplacementArrivee = arrivee;
        r.numeroBlFournisseur = "BL-TEST-4471";
        return r;
    }

    private LigneReception ligne(Reception r, Article article, String quantite,
                                 String lot, Emplacement destination) {
        LigneReception l = new LigneReception();
        l.reception = r;
        l.article = article;
        l.quantite = new BigDecimal(quantite);
        l.numeroLot = lot;
        l.emplacementDestination = destination;
        r.lignes.add(l);
        return l;
    }

    private Tiers fournisseur() {
        Tiers t = Tiers.findByCode(FOURN);
        if (t == null) {
            t = new Tiers();
            t.code = FOURN;
            t.raisonSociale = "Scierie de test";
            t.estFournisseur = true;
            t.persist();
        }
        return t;
    }

    /** Article sans traçabilité : le n° de lot est facultatif. */
    private Article articleSimple() {
        return article(ART_SIMPLE, "Planche de test", ModeTracabilite.AUCUN);
    }

    /** Article suivi en LOT : le n° de lot devient obligatoire. */
    private Article articleTrace() {
        return article(ART_TRACE, "Poutre de test", ModeTracabilite.LOT);
    }

    private Article article(String reference, String designation, ModeTracabilite mode) {
        Article a = Article.findByReference(reference);
        if (a == null) {
            a = new Article();
            a.reference = reference;
            a.designation = designation;
            a.unite = "piece";
            a.tracabilite = mode;
            a.persist();
        }
        return a;
    }

    private Emplacement emplacement(String code) {
        Emplacement e = Emplacement.findByCode(code);
        assertNotNull(e, "emplacement introuvable : " + code);
        return e;
    }

    /** BigDecimal : comparer la VALEUR, pas la représentation (150 vs 150.0000). */
    private void assertQuantite(String attendu, BigDecimal reel) {
        assertEquals(0, new BigDecimal(attendu).compareTo(reel),
                "attendu " + attendu + " mais obtenu " + reel);
    }
}
