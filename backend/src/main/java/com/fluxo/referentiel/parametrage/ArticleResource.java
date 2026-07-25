package com.fluxo.referentiel.parametrage;

import com.fluxo.referentiel.domain.Article;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST CRUD sur les ARTICLES du référentiel.
 * Tous les endpoints sont sous /articles et échangent du JSON (des ArticleDto en sortie).
 *
 *  GET    /articles        liste (filtres ?reference= &designation= &tracabilite= &actif=)
 *  GET    /articles/{id}   détail
 *  POST   /articles        création (201)
 *  PUT    /articles/{id}   modification
 *  DELETE /articles/{id}   suppression (204)
 */
@Path("/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArticleResource {

    // GET /articles -> liste filtrable. @BeanParam regroupe les critères d'URL dans un objet.
    // @Transactional : le mapping vers DTO parcourt les entités pendant que la session est ouverte.
    @GET
    @Transactional
    public List<ArticleDto> liste(@BeanParam ArticleRecherche recherche) {
        StringBuilder jpql = new StringBuilder("from Article art");
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (recherche.reference != null) {
            conditions.add("art.reference = :reference");
            params.put("reference", recherche.reference);
        }
        if (recherche.designation != null) {
            conditions.add("art.designation = :designation");
            params.put("designation", recherche.designation);
        }
        if (recherche.tracabilite != null) {
            conditions.add("art.tracabilite = :tracabilite");
            params.put("tracabilite", recherche.tracabilite);
        }
        if (recherche.actif != null) {
            conditions.add("art.actif = :actif");
            params.put("actif", recherche.actif);
        }
        if (!conditions.isEmpty()) {
            jpql.append(" where ").append(String.join(" and ", conditions));
        }
        jpql.append(" order by art.reference");

        List<Article> articles = Article.find(jpql.toString(), params).list();
        return articles.stream().map(ArticleDto::de).toList();
    }

    // GET /articles/{id} -> un article précis
    @GET
    @Path("/{id}")
    @Transactional
    public ArticleDto detail(@PathParam("id") long id) {
        Article article = Article.findById(id);
        if (article == null) {
            throw new WebApplicationException("Article " + id + " introuvable", 404);
        }
        return ArticleDto.de(article);
    }

    // POST /articles -> crée un article
    @POST
    @Transactional
    public Response creer(Article article) {
        if (article.id != null) {
            throw new WebApplicationException("L'id ne doit pas être fourni à la création", 422);
        }
        valider(article);
        // La reference est l'identifiant métier unique : on refuse un doublon.
        if (Article.count("reference", article.reference) > 0) {
            throw new WebApplicationException(
                    "La référence '" + article.reference + "' existe déjà", 409);
        }
        article.persist();
        return Response.status(Response.Status.CREATED).entity(ArticleDto.de(article)).build();
    }

    // PUT /articles/{id} -> met à jour un article
    @PUT
    @Path("/{id}")
    @Transactional
    public ArticleDto modifier(@PathParam("id") Long id, Article data) {
        Article article = Article.findById(id);
        if (article == null) {
            throw new WebApplicationException("Article " + id + " introuvable", 404);
        }
        valider(data);
        // Référence unique : interdit de la dupliquer sur un AUTRE article.
        Article homonyme = Article.findByReference(data.reference);
        if (homonyme != null && !homonyme.id.equals(id)) {
            throw new WebApplicationException(
                    "La référence '" + data.reference + "' existe déjà", 409);
        }
        article.reference = data.reference;
        article.designation = data.designation;
        article.description = data.description;
        article.unite = data.unite;
        article.tracabilite = data.tracabilite;
        article.actif = data.actif;
        return ArticleDto.de(article); // dirty checking Hibernate -> UPDATE au commit
    }

    // DELETE /articles/{id} -> supprime un article
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response supprimer(@PathParam("id") Long id) {
        boolean supprime = Article.deleteById(id);
        if (!supprime) {
            throw new WebApplicationException("Article " + id + " introuvable", 404);
        }
        return Response.noContent().build();
    }

    /** Règles métier minimales, partagées entre création et modification (422 si violées). */
    private void valider(Article a) {
        if (a.reference == null || a.reference.isBlank()) {
            throw new WebApplicationException("La référence est obligatoire", 422);
        }
        if (a.designation == null || a.designation.isBlank()) {
            throw new WebApplicationException("La désignation est obligatoire", 422);
        }
        if (a.unite == null || a.unite.isBlank()) {
            throw new WebApplicationException("L'unité est obligatoire", 422);
        }
        if (a.tracabilite == null) {
            throw new WebApplicationException("Le mode de traçabilité est obligatoire", 422);
        }
    }
}
