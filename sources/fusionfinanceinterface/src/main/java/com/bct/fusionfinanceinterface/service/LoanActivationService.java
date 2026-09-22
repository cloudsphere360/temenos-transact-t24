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

import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.bct.fusionfinanceinterface.model.TransactionData;
import com.bct.fusionfinanceinterface.util.CommonConstants;
import com.bct.fusionfinanceinterface.util.CommonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import reactor.core.publisher.Mono;

/**
 * @@author Shunmuga Raja Gurunathan
 *
 */
@Service
public class LoanActivationService {
	
	private static final Logger logger = LogManager.getLogger(LoanActivationService.class);
	
	@Autowired
	private WebClient webClient;
	
	@Autowired
	private CommonUtil commonUtil;
 
	 
	@Value("${fusion.loan.activation.put.url}")
	private String loanActivationPutURL;
	
	public LoanActivationService(WebClient webClient) {
        this.webClient = webClient;
    }
    public Mono<TransactionData> LoanActivationUpdate(TransactionData data) {
    	loanActivationPutURL=loanActivationPutURL==null?data.getOrchestrationRequest().getLoanActivationURL():loanActivationPutURL;
    	logger.debug("loanActivationPutURL : "+loanActivationPutURL);
        return webClient.put()
        		  .uri(uriBuilder -> uriBuilder
                          .path( loanActivationPutURL)
                          .queryParamIfPresent(CommonConstants.COMPANY_ID, Optional.ofNullable(data.getCompanyId()))
                          .build(data.getAarrangementId()))
        		    .headers( headers -> {
                        if (data.getAccessToken() != null && !data.getAccessToken().isBlank()) {
                            headers.set(HttpHeaders.AUTHORIZATION, data.getAccessToken());
                            }
                        })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(data.getOrchestrationRequest().getLoanSimulation())
                .exchangeToMono(response -> processLoanActivationResponse(response, data))
                .timeout(Duration.ofSeconds(500))
                .doOnError(WebClientResponseException.class, e -> logger.error("create # LoanActivation API failed: {}", e.getMessage()));
    }
    
	private Mono<TransactionData> handleLoanActivationResponse(String t24Response, TransactionData data) {
		logger.info(" handle # LoanActivation  t24 response  :: " + t24Response);
		OrchestrationResponse orchestrationResponse = new OrchestrationResponse();
		try {
			JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
			if (commonUtil.isSucsess(root)) {
				orchestrationResponse.setLoanSimulationId(
						root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._ID).getAsString());
				orchestrationResponse.setStatus(CommonConstants._SUCCESS);
				logger.info(" handle # LoanActivation successful "
						+ root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._ID).getAsString());
			}

			else {
				orchestrationResponse.setStatus(CommonConstants._FAILED);
				orchestrationResponse.setMessage(commonUtil.getMessageFromResponse(t24Response));
				logger.info(" handle # Loan Activation  failed  :: " + t24Response);
			}

		} catch (Exception e) {

			orchestrationResponse.setStatus(CommonConstants._FAILED);
			logger.error(" handle # LoanActivation   failed  :: " + e.getMessage());

		}
		data.setOrchestrationResponse(orchestrationResponse);
		return Mono.just(data);
	}
    
    private Mono<TransactionData> processLoanActivationResponse(ClientResponse response, TransactionData data) {
        HttpStatus status = response.statusCode();

        if (status.is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                           .flatMap(body -> handleLoanActivationResponse(body, data));
        } else {
            return response.bodyToMono(String.class)
                           .defaultIfEmpty("")
                           .flatMap(body -> {
                               logger.error("process # LoanActivation API returned {}: {}", status.value(), body);
                               return handleLoanActivationResponse(body, data);
                           });
        }
    }

}
