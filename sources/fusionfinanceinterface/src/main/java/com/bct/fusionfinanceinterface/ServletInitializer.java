package com.bct.fusionfinanceinterface;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(FusionfinanceinterfaceApplication.class);
	}
	
	@Bean
	public WebClient.Builder webClientBuilder() {
	    return WebClient.builder();
	}

}
