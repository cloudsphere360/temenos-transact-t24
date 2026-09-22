/**
 * 
 */
package com.bct.fusionfinanceinterface.service;

import java.time.Duration;
import java.util.Optional;

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

import com.bct.fusionfinanceinterface.model.TransactionData;
import com.bct.fusionfinanceinterface.util.CommonConstants;
import com.bct.fusionfinanceinterface.util.CommonUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import reactor.core.publisher.Mono;

/**
 * @@author Shunmuga Raja Gurunathan
 *
 */

@Service
public class LoanDetailService {
	
 
	 @Value("${fusion.loan.detail.update.url}")
	 private String loanDetailUpdateURL;
	 
	 @Autowired 
	 CommonUtil commonUtil;
	@Autowired
	private WebClient webClient;
	
    	
	private static final Logger logger = LogManager.getLogger(LoanDetailService.class);
	

    public LoanDetailService(WebClient webClient) {
        this.webClient = webClient;
    }
	
    public Mono<TransactionData> createLoanDeails(TransactionData data) {
    	loanDetailUpdateURL=loanDetailUpdateURL==null?data.getOrchestrationRequest().getLoanDetailsUpdateURL():loanDetailUpdateURL;
    	
        return webClient.put()
        		.uri(uriBuilder -> uriBuilder
        		        .path( loanDetailUpdateURL)
        		        .queryParamIfPresent(CommonConstants.COMPANY_ID, Optional.ofNullable(data.getCompanyId()))
        		        .build(data.getOrchestrationResponse().getArrangementId()))
        	     .headers( headers -> {
                     if (data.getAccessToken() != null && !data.getAccessToken().isBlank()) {
                         headers.set(HttpHeaders.AUTHORIZATION, data.getAccessToken());
                         }
                     }) 
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildLoanPayLoad(data))
                .exchangeToMono(response -> processLoanResponse(response, data))
                .timeout(Duration.ofSeconds(500))
                .doOnError(WebClientResponseException.class, e -> logger.error("create # Loan API failed: {}", e.getMessage()));
    }

	private String buildLoanPayLoad(TransactionData data) {
		try {
			
			JsonObject bodyTag = new JsonObject();
			JsonElement loanDetailsJson = JsonParser.parseString(data.getOrchestrationRequest().getLoanDetails());
			bodyTag.add(CommonConstants._BODY,loanDetailsJson );
			logger.debug(" build # CustomerPayLoad :: " + bodyTag.toString());
			
			return bodyTag.toString();
		} catch (Exception e) {
			logger.error(" Orchestration :: build ::   Loan Details Response :: ", e.getMessage());
		}
		return "";

	}

	private Mono<TransactionData> handleLoanResponse(String t24Response, TransactionData data) {
        //logger.info(" handle # Loan Details creation   : {}. Status: {}", data.getOrchestrationResponse().getCustomerId(), t24Response);
        logger.debug("handle # Loan Details Status Response: {}", t24Response);
        try {
        
    	JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
   	 if(commonUtil.isSucsess(root))
   	 {
   		data.getOrchestrationResponse().setLoanDetailsCreation(CommonConstants._SUCCESS);
   		data.getOrchestrationResponse().setStatus(CommonConstants._SUCCESS);
   		 logger.info(" handle # Loan Details  creation successful :: ");
   	 }
   	 
   	 else
   	 {
   		data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
		data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
   		data.getOrchestrationResponse().setLoanDetailsCreation(CommonConstants._FAILED);
   		 logger.info("handle # Loan Details creation failed  :: "+t24Response);
   	 }
        }
   	catch (Exception e)
	{
   		data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
   		data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
   		data.getOrchestrationResponse().setLoanDetailsCreation(CommonConstants._FAILED);
		 logger.error("handle #  Loan Details creation failed  :: "+e.getMessage() );
	}
   	 return Mono.just(data);
    }
	
	
    private Mono<TransactionData> processLoanResponse(ClientResponse response, TransactionData data) {
        HttpStatus status = response.statusCode();
        data.setHttpStatus(status);
        if (status.is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                           .flatMap(body -> handleLoanResponse(body, data));
        } else {
            return response.bodyToMono(String.class)
                           .defaultIfEmpty(CommonConstants._EMPTY)
                           .flatMap(body -> {
                               logger.error("process # Loan details API returned {}: {}", status.value(), body);
                               return handleLoanResponse(body, data);
                           });
        }
    }

}
