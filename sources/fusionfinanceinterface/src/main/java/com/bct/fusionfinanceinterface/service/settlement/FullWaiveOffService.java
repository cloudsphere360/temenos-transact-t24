package com.bct.fusionfinanceinterface.service.settlement;

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

import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.bct.fusionfinanceinterface.model.TransactionData;
import com.bct.fusionfinanceinterface.util.CommonConstants;
import com.bct.fusionfinanceinterface.util.CommonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * @@author Shunmuga Raja Gurunathan
 *
 */
@Service
public class FullWaiveOffService {
	private static final Logger logger = LogManager.getLogger(FullWaiveOffService.class);
	
	@Autowired
	private WebClient webClient;
	
	@Autowired
	private CommonUtil  commonUtil;
 
	 
	@Value("${fusion.loan.settlement.fullwaiveoff.post.url}")
	private String fullWaiveOffPostURL;
	
	public FullWaiveOffService(WebClient webClient) {
        this.webClient = webClient;
    }
    public Mono<TransactionData> fullWaiveOff(TransactionData data) {
        return webClient.post()
        		  .uri(uriBuilder -> uriBuilder
                          .path(fullWaiveOffPostURL)
                          .queryParamIfPresent(CommonConstants.COMPANY_ID, Optional.ofNullable(data.getCompanyId()))
                          .build())
        		  .headers( headers -> {
                      if (data.getAccessToken() != null && !data.getAccessToken().isBlank()) {
                          headers.set(HttpHeaders.AUTHORIZATION, data.getAccessToken());
                          }
                      })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(data.getOrchestrationRequest(). getFullWaiveOffDetails())
                .exchangeToMono(response -> processCreateFullWaiveOffResponse(response, data))
                .timeout(Duration.ofSeconds(500))
                .doOnError(WebClientResponseException.class, e -> logger.error("create # Create Collection API failed: {}", e.getMessage()));
    }

	private Mono<TransactionData> handleCreateFullWaiveOffResponse(String t24Response, TransactionData data) {
		logger.info(" handle # CreateCollection  t24 response  :: " + t24Response);
		OrchestrationResponse orchestrationResponse = new OrchestrationResponse();
		try {
			JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
			// if(root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._STATUS).getAsString().equals(CommonConstants._SUCCESS))
			if (commonUtil.isSucsess(root)) {
				orchestrationResponse.setStatus(CommonConstants._SUCCESS);
				orchestrationResponse.setTransactionCreation(CommonConstants._SUCCESS);
				orchestrationResponse.setTransactionId(
						root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._ID).getAsString());
				orchestrationResponse.setTransactionDetails(
						root.get(CommonConstants._BODY).toString());
				
				logger.info(" handle # CreateCollection successful "
						+ root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._ID).getAsString());
			} else {
				orchestrationResponse.setStatus(CommonConstants._FAILED);
				orchestrationResponse.setTransactionCreation(CommonConstants._FAILED);
				orchestrationResponse.setMessage(commonUtil.getMessageFromResponse(t24Response));
				logger.info(" handle # CreateCollection  failed  :: " + t24Response);
			}
		} catch (Exception e) {

			orchestrationResponse.setStatus(CommonConstants._FAILED);
			orchestrationResponse.setMessage(commonUtil.getMessageFromResponse(t24Response));
			logger.error(" handle # CreateCollection   failed  :: " + e.getMessage());
		}
		data.setOrchestrationResponse(orchestrationResponse);
		return Mono.just(data);
	}
    
	private Mono<TransactionData> processCreateFullWaiveOffResponse(ClientResponse response, TransactionData data) {
		HttpStatus status = response.statusCode();

		if (status.is2xxSuccessful()) {
			return response.bodyToMono(String.class).flatMap(body -> handleCreateFullWaiveOffResponse(body, data));
		} else {
			return response.bodyToMono(String.class).defaultIfEmpty("").flatMap(body -> {
				logger.error("process # CreateCollection API returned {}: {}", status.value(), body);
				return handleCreateFullWaiveOffResponse(body, data);
			});
		}
	}

    

}
