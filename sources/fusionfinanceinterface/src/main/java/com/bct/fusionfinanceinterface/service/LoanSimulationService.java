package com.bct.fusionfinanceinterface.service;
import reactor.core.publisher.Mono;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.bct.fusionfinanceinterface.model.TransactionData;
import com.bct.fusionfinanceinterface.util.CommonConstants;
import com.bct.fusionfinanceinterface.util.CommonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Duration;
import java.util.Optional;
@Service
public class LoanSimulationService {
 
	 @Value("${fusion.loan.simulation.create.url}")
	 private String loanSimulationCreateURL;
	 
	 @Value("${fusion.loan.simulation.update.url}")
	 private String loanSimulationUpdateURL;
	 
	@Autowired
	private WebClient webClient;
	
	 @Autowired 
	 CommonUtil commonUtil;
    	
	private static final Logger logger = LogManager.getLogger(LoanSimulationService.class);
	

/*    public BulkLoanService(WebClient webClient) {
        this.webClient = webClient;
    }*/

    public LoanSimulationService(WebClient webClient) {
        this.webClient = webClient;
    }
	
    public Mono<TransactionData> createLoanSimulation(TransactionData data) {
    	loanSimulationCreateURL=loanSimulationCreateURL==null?data.getOrchestrationRequest().getLoanSimulationCreateURL():loanSimulationCreateURL;
    	
        return webClient.post()
              //  .uri(baseURL+loanSimulationCreateURL)
        		.uri(uriBuilder -> uriBuilder
        		        .path( loanSimulationCreateURL)
        		        .queryParamIfPresent(CommonConstants.COMPANY_ID, Optional.ofNullable(data.getCompanyId()))
        		        .build())
        	     .headers( headers -> {
                     if (data.getAccessToken() != null && !data.getAccessToken().isBlank()) {
                         headers.set(HttpHeaders.AUTHORIZATION, data.getAccessToken());
                         }
                     }) 
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildLoanSimulationPayLoad(data))
                .exchangeToMono(response -> processLoanSimulationResponse(response, data))
                .timeout(Duration.ofSeconds(500))
                .doOnError(WebClientResponseException.class, e -> logger.error(" create # Loan Simulation API failed: {}", e.getMessage()));
    }
    
    
    public Mono<TransactionData> updateLoanSimulation(TransactionData data) {
    	OrchestrationResponse orchestrationResponse = new OrchestrationResponse();
    	data.setOrchestrationResponse(orchestrationResponse);
        return webClient.put()
        		.uri(uriBuilder -> uriBuilder
        		        .path( loanSimulationUpdateURL)
        		        .queryParamIfPresent(CommonConstants.COMPANY_ID, Optional.ofNullable(data.getCompanyId()))
        		        .build(data.getSimulationId()))
        	     .headers( headers -> {
                     if (data.getAccessToken() != null && !data.getAccessToken().isBlank()) {
                         headers.set(HttpHeaders.AUTHORIZATION, data.getAccessToken());
                         }
                     }) 
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildUpdateLoanSimulationPayLoad(data))
                .exchangeToMono(response -> processLoanSimulationResponse(response, data))
                .timeout(Duration.ofSeconds(500))
                .doOnError(WebClientResponseException.class, e -> logger.error(" create # Loan Simulation API failed: {}", e.getMessage()));
    }

	private String buildLoanSimulationPayLoad(TransactionData data) {
		try {
			JsonObject loanDetails = JsonParser.parseString(data.getOrchestrationRequest().getLoanSimulation()).getAsJsonObject();
			JsonObject headerTag = new JsonObject();
			JsonObject reqRootTag = new JsonObject();
			JsonObject overrides = loanDetails.getAsJsonObject(CommonConstants._OVERRIDE);
			
			headerTag.add(CommonConstants._OVERRIDE, overrides);
			reqRootTag.add(CommonConstants._HEADER,headerTag);
			loanDetails.remove(CommonConstants._OVERRIDE);
			JsonArray partyIdObj = new JsonArray();
			loanDetails.remove(CommonConstants.PARTY_IDS);
			// Add one or more names
			JsonObject partyId1 = new JsonObject();
			partyId1.addProperty(CommonConstants.CUSTOMER_ID, data.getOrchestrationResponse().getCustomerId());
			partyIdObj.add(partyId1);
			loanDetails.add(CommonConstants.PARTY_IDS, partyIdObj);
			reqRootTag.add(CommonConstants._BODY,loanDetails);
			
			logger.debug(" build # Simulation   loan Payload :: " + reqRootTag.toString());
			return reqRootTag.toString();

		} catch (Exception e) {
			logger.error(" Orchestration :: Simulation   :: ", e.getMessage());
		}
		return "";

	}

	private String buildUpdateLoanSimulationPayLoad(TransactionData data) {
		try {
			JsonObject loanDetails = JsonParser.parseString(data.getOrchestrationRequest().getLoanSimulation()).getAsJsonObject();
			JsonObject headerTag = new JsonObject();
			JsonObject reqRootTag = new JsonObject();
			JsonObject overrides = loanDetails.getAsJsonObject(CommonConstants._OVERRIDE);
			
			headerTag.add(CommonConstants._OVERRIDE, overrides);
			reqRootTag.add(CommonConstants._HEADER,headerTag);
			loanDetails.remove(CommonConstants._OVERRIDE);
		/*	JsonArray partyIdObj = new JsonArray();
			loanDetails.remove(CommonConstants.PARTY_IDS);
			// Add one or more names
			JsonObject partyId1 = new JsonObject();
			partyId1.addProperty(CommonConstants.CUSTOMER_ID, data.getCustomerId());
			partyIdObj.add(partyId1);
			loanDetails.add(CommonConstants.PARTY_IDS, partyIdObj);*/
			reqRootTag.add(CommonConstants._BODY,loanDetails);
			
			logger.debug(" build # Simulation   loan Payload :: " + reqRootTag.toString());
			return reqRootTag.toString();

		} catch (Exception e) {
			logger.error(" Orchestration :: Simulation   :: ", e.getMessage());
		}
		return "";

	}
	
	
	private Mono<TransactionData> handleLoanSimulationResponse(String t24Response, TransactionData data) {
       // logger.info(" handle # Loan Simulation creation : {}. Status: {}", data.getOrchestrationResponse().getCustomerId(), t24Response);
        logger.debug(" handle # Loan Simulation Status Response: {}", t24Response);
        try {
        
    	JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
   	 if(commonUtil.isSucsess(root))
   	 {
   		data.getOrchestrationResponse().setLoanSimulationCreation(CommonConstants._SUCCESS);
   		//data.getOrchestrationResponse().setLoanSimulationResponse(t24Response);
   		data.getOrchestrationResponse().setStatus(CommonConstants._SUCCESS);
   		
   		data.getOrchestrationResponse().setLoanSimulationId(root.getAsJsonObject(CommonConstants._BODY).getAsJsonObject(CommonConstants.ARRANGMENT_ACTIVITY).get(CommonConstants.SIMULATION_RUN_REFERENCE).getAsString());
   		data.getOrchestrationResponse().setArrangementId(root.getAsJsonObject(CommonConstants._BODY).getAsJsonObject(CommonConstants.ARRANGMENT_ACTIVITY).get(CommonConstants.ARRANGEMENT).getAsString());
   		 logger.info(" handle # Loan Simulation creation successful :: "+ data.getOrchestrationResponse().getLoanSimulationId());
   	 }
   	 
   	 else
   	 {
   		data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
   		data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
   		data.getOrchestrationResponse().setLoanSimulationCreation(CommonConstants._FAILED);
   		 logger.info(" handle # Loan Simulation creation failed  :: "+t24Response);
   	 }
        }
   	catch (Exception e)
	{
   		data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
   		data.getOrchestrationResponse().setLoanSimulationCreation(CommonConstants._FAILED);
   		data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
		 logger.error(" handle # Loan Simulation creation failed  :: "+e.getMessage());
	}
   	 return Mono.just(data);
    }
	
	
    private Mono<TransactionData> processLoanSimulationResponse(ClientResponse response, TransactionData data) {
        HttpStatus status = response.statusCode();
        data.setHttpStatus(status);
        if (status.is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                           .flatMap(body -> handleLoanSimulationResponse(body, data));
        } else {
            return response.bodyToMono(String.class)
                           .defaultIfEmpty(CommonConstants._EMPTY)
                           .flatMap(body -> {
                               logger.error(" process # Simulation API returned {}: {}", status.value(), body);
                               return handleLoanSimulationResponse(body, data);
                           });
        }
    }
    
    
 
    
}