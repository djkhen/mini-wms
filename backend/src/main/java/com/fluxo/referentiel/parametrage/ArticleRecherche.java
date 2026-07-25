package com.fluxo.referentiel.parametrage;

import com.fluxo.referentiel.domain.ModeTracabilite;
import jakarta.ws.rs.QueryParam;

public class ArtcleRecherche {
	@QueryParam("reference")
	String reference;

	@QueryParam("designation")
	String designation;

	@QueryParam("description")
	String description;

	@QueryParam("unite")
	String unite ;

	@QueryParam("tracabilite")
	ModeTracabilite tracabilite;

	@QueryParam("actif")
	boolean actif;



}
