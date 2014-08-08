package de.unirostock.sems.cbarchive.web.provider;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/")
public class RestMultipartApplication extends ResourceConfig {

	public RestMultipartApplication() {
		super(MultiPartFeature.class);
	}

	
}
