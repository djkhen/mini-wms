package com.fluxo.referentiel.parametrage;

import com.fluxo.referentiel.domain.Tiers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST CRUD sur les TIERS du référentiel (partenaire polyvalent : fournisseur/transporteur/client).
 *
 *  GET    /tiers        liste (filtres ?code= &raisonSociale= &estFournisseur= &estTransporteur= &estClient= &actif=)
 *  GET    /tiers/{id}   détail
 *  POST   /tiers        création (201)
 *  PUT    /tiers/{id}   modification COMPLÈTE (remplace tout)
 *  PATCH  /tiers/{id}   modification PARTIELLE (seuls les champs envoyés changent)
 *  DELETE /tiers/{id}   suppression (204)
 */
@Path("/tiers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TiersResource {

    @Inject
    ObjectMapper mapper; // fusionne un JSON partiel sur une entité (PATCH)

    // GET /tiers -> liste filtrable. @Transactional : le mapping vers DTO parcourt les entités.
    @GET
    @Transactional
    public List<TiersDto> liste(@QueryParam("code") String code,
                                @QueryParam("raisonSociale") String raisonSociale,
                                @QueryParam("estFournisseur") Boolean estFournisseur,
                                @QueryParam("estTransporteur") Boolean estTransporteur,
                                @QueryParam("estClient") Boolean estClient,
                                @QueryParam("actif") Boolean actif,
                                @DefaultValue("raisonSociale") @QueryParam("tri") String tri) {
        StringBuilder jpql = new StringBuilder("from Tiers t");
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (code != null)            { conditions.add("t.code = :code");                       params.put("code", code); }
        if (raisonSociale != null)   { conditions.add("t.raisonSociale = :raisonSociale");      params.put("raisonSociale", raisonSociale); }
        if (estFournisseur != null)  { conditions.add("t.estFournisseur = :estFournisseur");    params.put("estFournisseur", estFournisseur); }
        if (estTransporteur != null) { conditions.add("t.estTransporteur = :estTransporteur");  params.put("estTransporteur", estTransporteur); }
        if (estClient != null)       { conditions.add("t.estClient = :estClient");              params.put("estClient", estClient); }
        if (actif != null)           { conditions.add("t.actif = :actif");                      params.put("actif", actif); }

        if (!conditions.isEmpty()) {
            jpql.append(" where ").append(String.join(" and ", conditions));
        }
        // Tri au choix du client (?tri=), via une LISTE BLANCHE.
        // ⚠️ SÉCURITÉ : ne JAMAIS concaténer un `?tri=` libre dans le JPQL (injection) → switch fermé.
        String champTri = switch (tri) {
            case "code" -> "t.code";
            case "siret" -> "t.siret";
            default -> "t.raisonSociale"; // défaut sûr (nom du partenaire)
        };
        jpql.append(" order by ").append(champTri);

        List<Tiers> tiers = Tiers.find(jpql.toString(), params).list();
        return tiers.stream().map(TiersDto::de).toList();
    }

    // GET /tiers/{id}
    @GET
    @Path("/{id}")
    @Transactional
    public TiersDto detail(@PathParam("id") long id) {
        Tiers t = Tiers.findById(id);
        if (t == null) {
            throw new WebApplicationException("Tiers " + id + " introuvable", 404);
        }
        return TiersDto.de(t);
    }

    // POST /tiers -> crée un tiers
    @POST
    @Transactional
    public Response creer(Tiers tiers) {
        if (tiers.id != null) {
            throw new WebApplicationException("L'id ne doit pas être fourni à la création", 422);
        }
        valider(tiers);
        if (Tiers.count("code", tiers.code) > 0) {
            throw new WebApplicationException("Le code '" + tiers.code + "' existe déjà", 409);
        }
        tiers.persist();
        return Response.status(Response.Status.CREATED).entity(TiersDto.de(tiers)).build();
    }

    // PUT /tiers/{id} -> modification COMPLÈTE (le client envoie le tiers entier ; remplace tout)
    @PUT
    @Path("/{id}")
    @Transactional
    public TiersDto modifierComplet(@PathParam("id") Long id, Tiers data) {
        Tiers t = Tiers.findById(id);
        if (t == null) {
            throw new WebApplicationException("Tiers " + id + " introuvable", 404);
        }
        valider(data);
        Tiers homonyme = Tiers.findByCode(data.code);
        if (homonyme != null && !homonyme.id.equals(id)) {
            throw new WebApplicationException("Le code '" + data.code + "' existe déjà", 409);
        }
        t.code = data.code;
        t.raisonSociale = data.raisonSociale;
        t.siret = data.siret;
        t.email = data.email;
        t.telephone = data.telephone;
        t.estFournisseur = data.estFournisseur;
        t.estTransporteur = data.estTransporteur;
        t.estClient = data.estClient;
        t.actif = data.actif;
        return TiersDto.de(t); // dirty checking Hibernate -> UPDATE au commit
    }

    // PATCH /tiers/{id} -> mise à jour PARTIELLE : seuls les champs PRÉSENTS dans le JSON changent.
    @PATCH
    @Path("/{id}")
    @Transactional
    public TiersDto modifierPartiel(@PathParam("id") Long id, JsonNode patch) {
        Tiers t = Tiers.findById(id);
        if (t == null) {
            throw new WebApplicationException("Tiers " + id + " introuvable", 404);
        }
        // Sécurité : on n'autorise JAMAIS à changer l'id (clé primaire) via le corps.
        if (patch instanceof ObjectNode obj) {
            obj.remove("id");
        }
        // Fusionne SEULEMENT les champs présents ; un JSON invalide (mauvais type) -> 400 (faute client).
        try {
            mapper.readerForUpdating(t).readValue(mapper.treeAsTokens(patch));
        } catch (IOException e) {
            throw new WebApplicationException("Le JSON fourni est invalide.", 400);
        }
        valider(t); // valider APRÈS la fusion
        Tiers homonyme = Tiers.findByCode(t.code);
        if (homonyme != null && !homonyme.id.equals(id)) {
            throw new WebApplicationException("Le code '" + t.code + "' existe déjà", 409);
        }
        return TiersDto.de(t);
    }

    // DELETE /tiers/{id}
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response supprimer(@PathParam("id") Long id) {
        boolean supprime = Tiers.deleteById(id);
        if (!supprime) {
            throw new WebApplicationException("Tiers " + id + " introuvable", 404);
        }
        return Response.noContent().build();
    }

    /** Règles métier minimales, partagées entre création et modification (422 si violées). */
    private void valider(Tiers t) {
        if (t.code == null || t.code.isBlank()) {
            throw new WebApplicationException("Le code est obligatoire", 422);
        }
        if (t.raisonSociale == null || t.raisonSociale.isBlank()) {
            throw new WebApplicationException("La raison sociale est obligatoire", 422);
        }
    }
}
