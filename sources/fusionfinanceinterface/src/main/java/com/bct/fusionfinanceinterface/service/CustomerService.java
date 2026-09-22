package com.bct.fusionfinanceinterface.service;


/**
 * @@author Shunmuga Raja Gurunathan
 *
 */
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
import com.google.gson.JsonElement;
//import com.bct.FusionInterface.util.PropertyUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Duration;
import java.util.Optional;
@Service
public class CustomerService {
	
	private static final Logger logger = LogManager.getLogger(CustomerService.class);
	//@Autowired
	//private WebClientConfig webClientConfig; 
	
	
	
	@Autowired
	private WebClient webClient;
	
	 @Autowired 
	 CommonUtil commonUtil;
	 
	@Value("${fusion.customer.create.url}")
	private String customerCreateURL;
	
	public CustomerService(WebClient webClient) {
        this.webClient = webClient;
    }
	
	
    public Mono<TransactionData> createCustomer(TransactionData data) {
    	
    	
    	
    	customerCreateURL=customerCreateURL==null?data.getOrchestrationRequest().getCustomerCreateURL():customerCreateURL;
    	
    	logger.debug("customer url data obj: "+data.getOrchestrationRequest().getCustomerCreateURL());
    	logger.debug("customer url data customerURL: "+customerCreateURL);
        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                		
                        .path( customerCreateURL)
                        .queryParamIfPresent(CommonConstants.COMPANY_ID, Optional.ofNullable(data.getCompanyId()))
                        .build(data.getCustomerId()))
                .headers( headers -> {
                    if (data.getAccessToken() != null && !data.getAccessToken().isBlank()) {
                        headers.set(HttpHeaders.AUTHORIZATION, data.getAccessToken());
                        }
                    }) 
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildCustomerPayLoad(data))
                .exchangeToMono(response -> processCustomerResponse(response, data))
                .timeout(Duration.ofSeconds(500))
                .doOnError(WebClientResponseException.class, e -> logger.error("create # Customer API failed: {}", e.getMessage()));
    }
    
    
    
	private String buildCustomerPayLoad(TransactionData data) {
		try {
			JsonObject bodyTag = new JsonObject();
			JsonElement customerJson = JsonParser.parseString(data.getOrchestrationRequest().getCustomer());
			bodyTag.add(CommonConstants._BODY, customerJson);
			logger.debug(" build # CustomerPayLoad :: " + bodyTag.toString());
			return bodyTag.toString();

		} catch (Exception e) {
			logger.error(" build # CustomerPayLoad   :: ", e.getMessage());
		}
		return "";

	}

    private Mono<TransactionData> handleCustomerResponse(String t24Response, TransactionData data) {
    	 logger.info(" handle # Customer creation t24 response  :: "+t24Response); 
    	OrchestrationResponse orchestrationResponse =new  OrchestrationResponse();
    	try {
    	 JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
    	 if(commonUtil.isSucsess(root))
    	 {
    		 orchestrationResponse.setCustomerId( root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._ID).getAsString());
    		 orchestrationResponse.setCustomerCreation(CommonConstants._SUCCESS);
    		 orchestrationResponse.setCustomerResponse(t24Response);
    		  orchestrationResponse.setStatus(CommonConstants._SUCCESS);
    		 logger.info(" handle # Customer creation successful "+ root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._ID).getAsString());
    	 }
    	 else
    	 {
    		 orchestrationResponse.setCustomerCreation(CommonConstants._FAILED);
    		 orchestrationResponse.setCustomerResponse(t24Response);
    		 orchestrationResponse.setStatus(CommonConstants._FAILED);
    		 orchestrationResponse.setMessage(commonUtil.getMessageFromResponse(t24Response));
    		 logger.info(" handle # Customer creation failed  :: "+t24Response);
    	 }
    	 
    	}
    	catch (Exception e)
    	{
   		 orchestrationResponse.setStatus(CommonConstants._FAILED);
   		 orchestrationResponse.setMessage(commonUtil.getMessageFromResponse(t24Response));
   		 logger.error(" handle # Customer creation failed  :: "+e.getMessage());
    	}
    	data.setOrchestrationResponse(orchestrationResponse);
        return Mono.just(data);
    }
    
    
 
 
    
    
    private Mono<TransactionData> processCustomerResponse(ClientResponse response, TransactionData data) {
        HttpStatus status = response.statusCode();
        data.setHttpStatus(status);
        if (status.is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                           .flatMap(body -> handleCustomerResponse(body, data));
        } else {
            return response.bodyToMono(String.class)
                           .defaultIfEmpty(CommonConstants._EMPTY)
                           .flatMap(body -> {
                               logger.error("process # Customer API returned {}: {}", status.value(), body);
                               return handleCustomerResponse(body, data);
                           });
        }
    }

}
