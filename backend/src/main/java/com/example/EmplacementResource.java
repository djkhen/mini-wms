package com.example;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.panache.common.Parameters;

import java.util.List;

/**
 * API REST CRUD sur les emplacements de l'entrepôt.
 *
 * Tous les endpoints sont sous /emplacements et échangent du JSON.
 *
 *  GET    /emplacements                   liste (filtres ?zone= &type= &actif=)
 *  GET    /emplacements/{id}              détail
 *  POST   /emplacements                   création (201)
 *  PUT    /emplacements/{id}              modification
 *  DELETE /emplacements/{id}              suppression (204)
 */
@Path("/emplacements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmplacementResource {

    // GET /emplacements  -> liste, filtrable par zone, type et/ou actif.
    @GET
    public List<Emplacement> liste(@QueryParam("zone") String zone,
                                   @QueryParam("type") TypeEmplacement type,
                                   @QueryParam("actif") Boolean actif) {
        StringBuilder where = new StringBuilder("1=1");
        Parameters params = new Parameters();
        if (zone != null)  { where.append(" and zone = :zone");   params.and("zone", zone); }
        if (type != null)  { where.append(" and type = :type");   params.and("type", type); }
        if (actif != null) { where.append(" and actif = :actif"); params.and("actif", actif); }
        return Emplacement.find(where.toString(), params).list();
    }

    // GET /emplacements/{id}  -> un emplacement précis
    @GET
    @Path("/{id}")
    public Emplacement detail(@PathParam("id") Long id) {
        Emplacement e = Emplacement.findById(id);
        if (e == null) {
            throw new WebApplicationException("Emplacement " + id + " introuvable", 404);
        }
        return e;
    }

    // POST /emplacements  -> crée un emplacement
    @POST
    @Transactional
    public Response creer(Emplacement emplacement) {
        if (emplacement.id != null) {
            throw new WebApplicationException("L'id ne doit pas être fourni à la création", 422);
        }
        valider(emplacement);
        // Le code est l'identifiant métier unique : on refuse un doublon.
        if (Emplacement.count("code", emplacement.code) > 0) {
            throw new WebApplicationException(
                    "Le code '" + emplacement.code + "' existe déjà", 409);
        }
        emplacement.persist();
        return Response.status(Response.Status.CREATED).entity(emplacement).build();
    }

    // PUT /emplacements/{id}  -> met à jour un emplacement
    @PUT
    @Path("/{id}")
    @Transactional
    public Emplacement modifier(@PathParam("id") Long id, Emplacement data) {
        Emplacement e = Emplacement.findById(id);
        if (e == null) {
            throw new WebApplicationException("Emplacement " + id + " introuvable", 404);
        }
        valider(data);
        // Code unique : interdit de le dupliquer sur un AUTRE emplacement.
        Emplacement homonyme = Emplacement.findByCode(data.code);
        if (homonyme != null && !homonyme.id.equals(id)) {
            throw new WebApplicationException(
                    "Le code '" + data.code + "' existe déjà", 409);
        }
        e.code = data.code;
        e.libelle = data.libelle;
        e.type = data.type;
        e.zone = data.zone;
        e.allee = data.allee;
        e.travee = data.travee;
        e.niveau = data.niveau;
        e.actif = data.actif;
        return e; // dirty checking Hibernate → mise à jour en base au commit
    }

    // DELETE /emplacements/{id}  -> supprime un emplacement
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response supprimer(@PathParam("id") Long id) {
        boolean supprime = Emplacement.deleteById(id);
        if (!supprime) {
            throw new WebApplicationException("Emplacement " + id + " introuvable", 404);
        }
        return Response.noContent().build();
    }

    /** Règles métier minimales partagées entre création et modification. */
    private void valider(Emplacement e) {
        if (e.code == null || e.code.isBlank()) {
            throw new WebApplicationException("Le code est obligatoire", 422);
        }
        if (e.type == null) {
            throw new WebApplicationException("Le type d'emplacement est obligatoire", 422);
        }
    }
}
