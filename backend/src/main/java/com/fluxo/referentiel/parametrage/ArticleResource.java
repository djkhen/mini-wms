package com.fluxo.referentiel.parametrage;

import com.fluxo.referentiel.domain.Article ;
import com.fluxo.referentiel.domain.ModeTracabilite;
import io.quarkus.panache.common.Parameters;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
/**
 * API REST CRUD sur les emplacements de l'entrepôt.
 *
 * Tous les endpoints sont sous /emplacements et échangent du JSON.
 *
 *  GET    /article                   liste (filtres ?id &designation= &description= &unite=)
 *  GET    /article/{id}              détail
 *  POST   /article                   création (201)
 *  PUT    /article/{id}              modification
 *  DELETE /article/{id}              suppression (204)
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArticleResource {
	// GET /article  -> liste, filtrable par designation, description et/ou actif.



	@GET
	public List<Article> list(@QueryParam("designation") String designation ,
	                          @QueryParam("description") String description ,
							  @QueryParam("actif") String actif
	) {
		StringBuilder where = new StringBuilder("1=1");  //JSQL
		Parameters params = new Parameters();
		if (designation != null)  { where.append(" and designation = :designation");   params.and("designation", designation); }
		if (description != null)  { where.append(" and description = :description");   params.and("description", description); }
		if (actif != null) { where.append(" and actif = :actif"); params.and("actif", actif); }
		return Article.find(where.toString() ,params).list() ;

		//return Article.findAll().list();
	}
	@GET
	public List<ArticleDto> getArtcle(){

	    return com.fluxo.referentiel.parametrage.ArticleDto;
	}

	@GET
	@Path("/{id}")
	public Article detail(@PathParam("id") long id) {
		Article article = Article.findById(id);
		if(article== null)	{
			throw new WebApplicationException("Article "+id + " introuvable", 404);
		}
		return article ;
	}

}
