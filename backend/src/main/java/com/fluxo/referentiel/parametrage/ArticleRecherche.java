package com.fluxo.referentiel.parametrage;

import com.fluxo.referentiel.domain.ModeTracabilite;
import jakarta.ws.rs.QueryParam;

public class ArticleRecherche {
	@QueryParam("reference")
	public String reference;

	@QueryParam("designation")
	public String designation;

	@QueryParam("description")
	public String description;

	@QueryParam("unite")
	public String unite ;

	@QueryParam("tracabilite")
	public ModeTracabilite tracabilite;

	@QueryParam("actif")
	public Boolean actif;



}
