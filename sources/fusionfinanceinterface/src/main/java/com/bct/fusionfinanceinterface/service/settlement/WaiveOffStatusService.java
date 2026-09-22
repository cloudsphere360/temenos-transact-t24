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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * @@author Shunmuga Raja Gurunathan
 *
 */
@Service
public class WaiveOffStatusService {
	private static final Logger logger = LogManager.getLogger(WaiveOffStatusService.class);
	
	@Autowired
	private WebClient webClient;
	
	@Autowired
	private CommonUtil  commonUtil;
 
	 
	@Value("${fusion.loan.settlement.waiveoffstatus.post.url}")
	private String waiveOffStatusPostURL;
	
	public WaiveOffStatusService(WebClient webClient) {
        this.webClient = webClient;
    }
    public Mono<TransactionData> updateLoanStatus(TransactionData data) {
        return webClient.post()
        		  .uri(uriBuilder -> uriBuilder
                          .path(waiveOffStatusPostURL)
                          .queryParamIfPresent(CommonConstants.COMPANY_ID, Optional.ofNullable(data.getCompanyId()))
                          .build())
        		  .headers( headers -> {
                      if (data.getAccessToken() != null && !data.getAccessToken().isBlank()) {
                          headers.set(HttpHeaders.AUTHORIZATION, data.getAccessToken());
                          }
                      })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildUpdateLoanStatusPayLoad(data))
                .exchangeToMono(response -> processCreateFullWaiveOffResponse(response, data))
                .timeout(Duration.ofSeconds(500))
                .doOnError(WebClientResponseException.class, e -> logger.error("create # waiveOffStatus API failed: {}", e.getMessage()));
    }

	private Mono<TransactionData> handleWaiveOffStatusResponse(String t24Response, TransactionData data) {
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
				
				logger.info(" handle # waiveOffStatus successful "
						+ root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._ID).getAsString());
			} else {
				orchestrationResponse.setStatus(CommonConstants._FAILED);
				orchestrationResponse.setTransactionCreation(CommonConstants._FAILED);
				orchestrationResponse.setMessage(commonUtil.getMessageFromResponse(t24Response));
				logger.info(" handle # waiveOffStatus  failed  :: " + t24Response);
			}
		} catch (Exception e) {

			orchestrationResponse.setStatus(CommonConstants._FAILED);
			orchestrationResponse.setMessage(commonUtil.getMessageFromResponse(t24Response));
			logger.error(" handle # waiveOffStatus   failed  :: " + e.getMessage());
		}
		data.setOrchestrationResponse(orchestrationResponse);
		return Mono.just(data);
	}
    
	private Mono<TransactionData> processCreateFullWaiveOffResponse(ClientResponse response, TransactionData data) {
		HttpStatus status = response.statusCode();

		if (status.is2xxSuccessful()) {
			return response.bodyToMono(String.class).flatMap(body -> handleWaiveOffStatusResponse(body, data));
		} else {
			return response.bodyToMono(String.class).defaultIfEmpty("").flatMap(body -> {
				logger.error("process # waiveOffStatus API returned {}: {}", status.value(), body);
				return handleWaiveOffStatusResponse(body, data);
			});
		}
	}

	private String buildUpdateLoanStatusPayLoad(TransactionData data) {
		try {
			JsonObject root = new JsonObject();
			JsonObject bodyTag = new JsonObject();
			
			JsonObject fullWaiveOffDetails = JsonParser.parseString( data.getOrchestrationRequest().getFullWaiveOffDetails()).getAsJsonObject();
	        JsonObject fullWaiveOffDetailsBody = fullWaiveOffDetails.getAsJsonObject(CommonConstants._BODY);
	        //  Extract the "loanRefNo" property as a string
	        if (fullWaiveOffDetailsBody != null && fullWaiveOffDetailsBody.has(CommonConstants.LOAN_REF_NO)) {
	        	bodyTag.addProperty(CommonConstants.LOAN_REF_NO, fullWaiveOffDetailsBody.get(CommonConstants.LOAN_REF_NO).getAsString());
	        }
			root.add(CommonConstants._BODY, bodyTag);
			logger.debug(" build # waiveOffStatus :: " + bodyTag.toString());
			return root.toString();

		} catch (Exception e) {
			logger.error(" build # waiveOffStatus   :: ", e.getMessage());
		}
		return "";

	}

}
