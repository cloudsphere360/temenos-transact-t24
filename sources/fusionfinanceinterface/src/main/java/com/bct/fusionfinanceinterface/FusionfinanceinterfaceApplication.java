package com.bct.fusionfinanceinterface;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.bct.fusionfinanceinterface.model.OrchestrationResponse;

@SpringBootApplication
public class FusionfinanceinterfaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FusionfinanceinterfaceApplication.class, args);
	}
	
	   @Bean
	    public OrchestrationResponse orchestrationResponse() {
	        return new OrchestrationResponse();
	    }

}
