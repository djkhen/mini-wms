package com.fluxo.flux.domain;

import com.fluxo.referentiel.domain.Article;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verrouille LE principe non négociable (§3) : le stock n'est jamais stocké, il est DÉRIVÉ.
 *
 * Ces tests rejouent le parcours d'un produit (docs/parcours-produit-mini_wms.html) et
 * vérifient qu'à chaque étape le stock CALCULÉ est le bon. Aucune table `Stock` n'est
 * consultée — il n'en existe pas : tout sort de la somme des mouvements.
 *
 * Prérequis : la base Docker doit tourner (`docker compose up -d db`), cf. le profil %test.
 */
@QuarkusTest
class MouvementStockDeriveTest {

    private static final String REF = "TEST-MVT-PLANCHE";

    /** On nettoie d'abord les mouvements (ils référencent l'article), puis l'article. */
    @AfterEach
    void nettoyer() {
        QuarkusTransaction.requiringNew().run(() -> {
            Mouvement.delete("article.reference", REF);
            Article.delete("reference", REF);
        });
    }

    // ------------------------------------------------------------------
    //  ② Le stock NAÎT d'un mouvement — il n'est jamais saisi
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Réception : FOURNISSEUR -> Quai fait apparaître 150 au quai, et rien ailleurs")
    void le_stock_nait_dun_mouvement() {
        QuarkusTransaction.requiringNew().run(() -> {
            Article a = creerArticle();
            mouvement(a, virtuel("FOURNISSEUR"), phys("QUAI-01"), "150", EtatMouvement.VALIDE);

            assertQuantite("150", Mouvement.stockPhysique(a, phys("QUAI-01")));
            assertQuantite("0", Mouvement.stockPhysique(a, phys("A-01-01-1")));
        });
    }

    // ------------------------------------------------------------------
    //  ③ Un déplacement change la RÉPARTITION, jamais le total
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Rangement : Quai -> A-01-01-1 vide le quai et remplit le rack (total constant)")
    void le_rangement_deplace_sans_changer_le_total() {
        QuarkusTransaction.requiringNew().run(() -> {
            Article a = creerArticle();
            mouvement(a, virtuel("FOURNISSEUR"), phys("QUAI-01"), "150", EtatMouvement.VALIDE);
            mouvement(a, phys("QUAI-01"), phys("A-01-01-1"), "150", EtatMouvement.VALIDE);

            assertQuantite("0", Mouvement.stockPhysique(a, phys("QUAI-01")));
            assertQuantite("150", Mouvement.stockPhysique(a, phys("A-01-01-1")));
        });
    }

    // ------------------------------------------------------------------
    //  ④ Le BROUILLON : une intention, pas un fait
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Allocation BROUILLON : le physique ne bouge pas, mais le disponible baisse")
    void le_brouillon_reserve_sans_toucher_au_physique() {
        QuarkusTransaction.requiringNew().run(() -> {
            Article a = creerArticle();
            mouvement(a, virtuel("FOURNISSEUR"), phys("A-01-01-1"), "150", EtatMouvement.VALIDE);
            // on promet 40 à un client : rien n'a bougé physiquement
            mouvement(a, phys("A-01-01-1"), virtuel("CLIENT"), "40", EtatMouvement.BROUILLON);

            assertQuantite("150", Mouvement.stockPhysique(a, phys("A-01-01-1")));
            assertQuantite("110", Mouvement.stockDisponible(a, phys("A-01-01-1")));
        });
    }

    // ------------------------------------------------------------------
    //  ⑦ Valider le brouillon = le camion part
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Expédition : valider le brouillon fait enfin baisser le stock physique")
    void valider_le_brouillon_fait_baisser_le_physique() {
        QuarkusTransaction.requiringNew().run(() -> {
            Article a = creerArticle();
            mouvement(a, virtuel("FOURNISSEUR"), phys("A-01-01-1"), "150", EtatMouvement.VALIDE);
            Mouvement expedition = mouvement(a, phys("A-01-01-1"), virtuel("CLIENT"), "40", EtatMouvement.BROUILLON);

            expedition.valider();
            expedition.persist();

            assertQuantite("110", Mouvement.stockPhysique(a, phys("A-01-01-1")));
            // le disponible n'a pas bougé : ces 40 étaient déjà décomptés en brouillon
            assertQuantite("110", Mouvement.stockDisponible(a, phys("A-01-01-1")));
        });
    }

    // ------------------------------------------------------------------
    //  ⑧ La casse : aucun code spécial, encore un mouvement
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Casse vers PERTE : même mécanique, et la perte reste tracée")
    void la_casse_est_un_mouvement_comme_un_autre() {
        QuarkusTransaction.requiringNew().run(() -> {
            Article a = creerArticle();
            mouvement(a, virtuel("FOURNISSEUR"), phys("A-01-01-1"), "150", EtatMouvement.VALIDE);
            mouvement(a, phys("A-01-01-1"), virtuel("PERTE"), "3", EtatMouvement.VALIDE);

            assertQuantite("147", Mouvement.stockPhysique(a, phys("A-01-01-1")));
            // rien ne disparaît : les 3 planches sont comptabilisées en PERTE
            assertQuantite("3", Mouvement.stockPhysique(a, virtuel("PERTE")));
        });
    }

    // ------------------------------------------------------------------
    //  Règle 6 — on ne corrige JAMAIS, on inverse
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Erreur de saisie : le mouvement inverse annule l'effet SANS effacer la trace")
    void le_mouvement_inverse_annule_leffet() {
        QuarkusTransaction.requiringNew().run(() -> {
            Article a = creerArticle();
            Mouvement faute = mouvement(a, virtuel("FOURNISSEUR"), phys("A-01-01-1"), "150", EtatMouvement.VALIDE);
            assertQuantite("150", Mouvement.stockPhysique(a, phys("A-01-01-1")));

            Mouvement correction = faute.inverse();
            correction.persist();

            assertQuantite("0", Mouvement.stockPhysique(a, phys("A-01-01-1")));
            // LES DEUX lignes subsistent : l'erreur ET sa correction (journal auditable)
            assertEquals(2, Mouvement.count("article.reference", REF));
            assertEquals("CORRECTION", correction.origineType);
            assertEquals(faute.id, correction.origineId);
        });
    }

    // ------------------------------------------------------------------
    //  Règle 1 — la base elle-même refuse une quantité négative
    // ------------------------------------------------------------------
    @Test
    @DisplayName("La contrainte CHECK rejette une quantité négative, même en forçant")
    void la_base_refuse_une_quantite_negative() {
        assertThrows(Exception.class, () ->
                QuarkusTransaction.requiringNew().run(() -> {
                    Article a = creerArticle();
                    mouvement(a, virtuel("FOURNISSEUR"), phys("QUAI-01"), "-10", EtatMouvement.VALIDE);
                    Mouvement.flush(); // force l'envoi en base : c'est PostgreSQL qui refuse
                }),
                "la règle « quantité > 0 » est gravée dans la base, pas seulement dans le code");
    }

    // ---------------------------- helpers ----------------------------

    private Article creerArticle() {
        Article a = new Article();
        a.reference = REF;
        a.designation = "Planche de test";
        a.unite = "piece";
        a.persist();
        return a;
    }

    private Mouvement mouvement(Article a, Emplacement source, Emplacement destination,
                                String quantite, EtatMouvement etat) {
        Mouvement m = new Mouvement();
        m.article = a;
        m.quantite = new BigDecimal(quantite);
        m.unite = a.unite;
        m.source = source;
        m.destination = destination;
        m.etat = etat;
        m.dateEffet = LocalDateTime.now();
        m.persist();
        return m;
    }

    /** Emplacement physique du jeu de démo. */
    private Emplacement phys(String code) {
        return trouver(code);
    }

    /** Emplacement virtuel créé par la migration 006 (structurel, toujours présent). */
    private Emplacement virtuel(String code) {
        Emplacement e = trouver(code);
        assertTrue(e.type.estVirtuel(), code + " doit être un emplacement VIRTUEL");
        return e;
    }

    private Emplacement trouver(String code) {
        Emplacement e = Emplacement.findByCode(code);
        assertNotNull(e, "emplacement introuvable : " + code);
        return e;
    }

    /** BigDecimal : on compare la VALEUR (compareTo), pas la représentation (150 vs 150.0000). */
    private void assertQuantite(String attendu, BigDecimal reel) {
        assertEquals(0, new BigDecimal(attendu).compareTo(reel),
                "attendu " + attendu + " mais obtenu " + reel);
    }
}
