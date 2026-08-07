package com.fluxo.referentiel.parametrage;

import com.fluxo.referentiel.domain.Tiers;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verrouille le contrat de la colonne `champs_custom JSONB` de Tiers (§6octies du CDC).
 *
 * POURQUOI ces tests : la doc DECRIT une regle, elle ne l'EMPECHE pas d'etre violee. Ici on
 * PROUVE, a chaque build, que l'aller-retour Java <-> JSONB reste fidele. Le jour ou quelqu'un
 * change `quarkus.hibernate-orm.mapping.format.global`, monte de version Quarkus ou retouche
 * le DTO, le build CASSE — au lieu de corrompre des donnees en silence.
 *
 * Prerequis : la base Docker doit tourner (`docker compose up -d db`), cf. le profil %test.
 */
@QuarkusTest
class TiersChampsCustomTest {

    /*
     * ================== COMMENT LIRE LE JSON DE CES TESTS ==================
     *
     *  Chaque test envoie un corps JSON ecrit ainsi :
     *
     *      creer("""
     *            { "code": "%s", "raisonSociale": "..." }
     *            """.formatted(CODE));
     *
     *  1) Les triples guillemets = TEXT BLOCK (Java 15+) : une chaine sur plusieurs lignes,
     *     SANS \n ni guillemets echappes. Sinon il faudrait ecrire "{\"code\": \"...\"}"
     *     — illisible des que le JSON grossit.
     *
     *  2) %s = un EMPLACEMENT a remplir. `.formatted(CODE)` y injecte la valeur de CODE :
     *     c'est exactement `String.format(chaine, CODE)`, mais ecrit APRES la chaine.
     *     Le JSON reellement envoye devient donc : { "code": "TEST-AUTO-JSONB", ... }
     *     (Cousins de %s : %d pour un entier, %.2f pour un decimal a 2 chiffres.
     *      Plusieurs %s = plusieurs arguments, DANS L'ORDRE.)
     *
     *  3) POURQUOI passer par la constante plutot qu'ecrire le code en dur ? Parce que le
     *     MEME code sert a CREER le tiers et a le SUPPRIMER dans nettoyer() @AfterEach.
     *     En dur dans 6 tests, un renommage en oublierait un -> ce test laisserait des
     *     dechets en base. Une seule source de verite = desynchronisation impossible.
     *
     *  /!\ PIEGE : dans une chaine formatee, un % LITTERAL doit etre DOUBLE. Pour envoyer
     *      "remise": "10%", il faut ecrire 10%% — sinon UnknownFormatConversionException
     *      a l'execution. C'est le seul cout de .formatted().
     * =======================================================================
     */

    /** Code dedie aux tests : sert aussi de cle de nettoyage (aucune collision avec les seeds). */
    private static final String CODE = "TEST-AUTO-JSONB";

    @Inject
    EntityManager em;

    /** Chaque test repart d'une base propre : on supprime le tiers de test, meme en cas d'echec. */
    @AfterEach
    void nettoyer() {
        QuarkusTransaction.requiringNew().run(() -> Tiers.delete("code", CODE));
    }

    // ------------------------------------------------------------------
    //  1. Les 6 types JSON font l'aller-retour SANS deformation
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Les 6 types JSON (dont objet imbrique) reviennent intacts apres relecture en base")
    void round_trip_des_types() {
        long id = creer("""
                {
                  "code": "%s",
                  "raisonSociale": "Test automatise des types",
                  "champsCustom": {
                    "texte": "chene massif",
                    "entier": 15,
                    "decimal": 12.50,
                    "booleen": true,
                    "dateIso": "2026-08-07",
                    "vide": null,
                    "liste": ["PEFC", "FSC"],
                    "objet": { "nom": "Durand" }
                  }
                }
                """.formatted(CODE));

        // GET = relecture depuis la BASE (nouvelle transaction), pas l'objet reste en memoire.
        JsonPath json = relire(id);

        assertEquals("chene massif", json.getString("champsCustom.texte"), "texte");
        assertEquals(15, json.getInt("champsCustom.entier"), "entier");
        assertEquals(12.5, json.getDouble("champsCustom.decimal"), "decimal (12.50 -> 12.5)");
        assertTrue(json.getBoolean("champsCustom.booleen"), "booleen");
        assertNull(json.get("champsCustom.vide"), "null");
        assertEquals(List.of("PEFC", "FSC"), json.getList("champsCustom.liste"), "tableau");
        assertEquals("Durand", json.getString("champsCustom.objet.nom"), "objet imbrique");
    }

    // ------------------------------------------------------------------
    //  2. LA regle : une date n'est PAS une date, c'est une chaine ISO 8601
    // ------------------------------------------------------------------
    @Test
    @DisplayName("JSON n'a pas de type date : une date revient en String (convention ISO 8601)")
    void la_date_revient_en_chaine() {
        long id = creer("""
                {
                  "code": "%s",
                  "raisonSociale": "Test date",
                  "champsCustom": { "dateIso": "2026-08-07", "dateAvecHeure": "2026-08-07T14:30:00" }
                }
                """.formatted(CODE));

        JsonPath json = relire(id);

        // Le coeur de la regle : le type JSON d'une date est `string`, jamais une date.
        assertInstanceOf(String.class, json.get("champsCustom.dateIso"));
        assertInstanceOf(String.class, json.get("champsCustom.dateAvecHeure"));
        assertEquals("2026-08-07", json.getString("champsCustom.dateIso"));
        assertEquals("2026-08-07T14:30:00", json.getString("champsCustom.dateAvecHeure"));
    }

    // ------------------------------------------------------------------
    //  3. ...mais une chaine ISO reste EXPLOITABLE en SQL (c'est tout l'interet)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Une date ISO se caste en vraie date SQL et supporte l'arithmetique (+ 30 jours)")
    void la_date_iso_se_caste_en_sql() {
        creer("""
                {
                  "code": "%s",
                  "raisonSociale": "Test cast SQL",
                  "champsCustom": { "dateIso": "2026-08-07" }
                }
                """.formatted(CODE));

        // ->> extrait le TEXTE, ::date le convertit : Postgres fait alors du vrai calcul de dates.
        Object dans30j = QuarkusTransaction.requiringNew().call(() ->
                em.createNativeQuery(
                                "SELECT (champs_custom->>'dateIso')::date + 30 FROM tiers WHERE code = :code")
                        .setParameter("code", CODE)
                        .getSingleResult());

        assertEquals("2026-09-06", dans30j.toString(), "7 aout + 30 jours = 6 septembre");
    }

    // ------------------------------------------------------------------
    //  4. Le format francais jj/mm/aaaa casserait le tri : on documente POURQUOI il est banni
    // ------------------------------------------------------------------
    @Test
    @DisplayName("ISO 8601 : tri alphabetique = tri chronologique ; jj/mm/aaaa inverse l'ordre")
    void le_tri_alphabetique_vaut_tri_chronologique() {
        // 7 aout 2026 est AVANT 6 septembre 2026.
        assertTrue("2026-08-07".compareTo("2026-09-06") < 0,
                "ISO : l'ordre alphabetique respecte la chronologie");

        // Meme paire de dates en format francais : l'ordre alphabetique s'INVERSE (07 > 06).
        assertTrue("07/08/2026".compareTo("06/09/2026") > 0,
                "jj/mm/aaaa : l'ordre alphabetique contredit la chronologie -> format banni");
    }

    // ------------------------------------------------------------------
    //  5. La colonne n'impose AUCUN type : c'est un reservoir, pas un contrat
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Le JSONB accepte 'entier': \"quinze\" : la validation incombe a l'application")
    void le_jsonb_n_impose_aucun_type() {
        long id = creer("""
                {
                  "code": "%s",
                  "raisonSociale": "Test absence de contrainte",
                  "champsCustom": { "entier": "quinze" }
                }
                """.formatted(CODE));

        // La base ne bronche pas : d'ou la necessite de `config_champ.type` + validation applicative.
        assertEquals("quinze", relire(id).getString("champsCustom.entier"));
    }

    // ------------------------------------------------------------------
    //  6. Un PATCH peut remplacer le JSON par des cles TOTALEMENT differentes
    // ------------------------------------------------------------------
    @Test
    @DisplayName("PATCH remplace le JSON par d'autres cles (chaque client a les siennes)")
    void patch_remplace_les_cles() {
        long id = creer("""
                {
                  "code": "%s",
                  "raisonSociale": "Test patch",
                  "champsCustom": { "codeChantier": "CH-2024-42" }
                }
                """.formatted(CODE));

        given().contentType(APPLICATION_JSON)
                .body("""
                        { "champsCustom": { "numeroLotBois": "LOT-77", "essence": "chene" } }
                        """)
                .when().patch("/tiers/" + id)
                .then().statusCode(200);

        JsonPath json = relire(id);
        assertEquals("LOT-77", json.getString("champsCustom.numeroLotBois"));
        assertEquals("chene", json.getString("champsCustom.essence"));
        assertNull(json.get("champsCustom.codeChantier"), "l'ancienne cle a bien disparu");
    }

    // ---------------------------- helpers ----------------------------

    /** POST /tiers -> 201, renvoie l'id genere. */
    private long creer(String corpsJson) {
        return given().contentType(APPLICATION_JSON).body(corpsJson)
                .when().post("/tiers")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    /** GET /tiers/{id} -> 200 : force une RELECTURE depuis la base. */
    private JsonPath relire(long id) {
        return given()
                .when().get("/tiers/" + id)
                .then().statusCode(200)
                .extract().jsonPath();
    }
}
