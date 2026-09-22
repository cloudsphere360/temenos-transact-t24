/**
 * 
 */
package com.bct.fusionfinanceinterface.service.payoff;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
public class PayOffDetailService {
	
	
	private static final Logger logger = LogManager.getLogger(PayOffDetailService.class);
	
	 @Value("${fusion.loan.payoff.response.get.url}")
	 private String payOffDetailsGetURL;
	 
	@Autowired
	private WebClient webClient;
	
	@Autowired 
	CommonUtil commonUtil;
 
    public PayOffDetailService (WebClient webClient) {
        this.webClient = webClient;
    }
	
	public Mono<TransactionData> getPayOffDetail(TransactionData data) {
		WebClient.RequestHeadersSpec<?> requestSpec =

				webClient.get().uri(
						uriBuilder -> uriBuilder.path(payOffDetailsGetURL).queryParam(CommonConstants.ARRANGEMENT_ID,
								data.getOrchestrationResponse().getArrangementId()).build());
		Optional.ofNullable(data.getCompanyId()).ifPresent(id -> requestSpec.header(CommonConstants.COMPANY_ID, id));
		Optional.ofNullable(data.getAccessToken()).ifPresent(token -> requestSpec.header(HttpHeaders.AUTHORIZATION, data.getAccessToken()));
		return requestSpec.retrieve().bodyToMono(String.class)
				.flatMap(response -> handlePayOffDetailsResponse(response, data))
				.doOnError(e -> logger.error("getPayOffDetail # Error calling GET API: {}", e.getMessage()));
	}

	private Mono<TransactionData> handlePayOffDetailsResponse(String t24Response, TransactionData data) {
		logger.info(" handle # getPayOffDetail GET  : {}. Status: {}",
				data.getOrchestrationResponse().getCustomerId(), t24Response);
		logger.debug(" handle #  getPayOffDetail  Status Response: {}", t24Response);
		try {

			JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
			if (commonUtil.isSucsess(root)) {
				data.getOrchestrationResponse().setStatus(CommonConstants._SUCCESS);
				data.getOrchestrationResponse().setPayOffDetails(root.get(CommonConstants._BODY).toString());
				logger.info(" handle #  getPayOffDetail GET  successful :: ");
			}

			else {
				data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
				data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
				logger.info(" handle #  getPayOffDetail  GET  failed  :: " + t24Response);
			}
		} catch (Exception e) {
			data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
			data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
			logger.error(" handle #  getPayOffDetail GET failed  :: " + e.getMessage());
		}
		return Mono.just(data);
	}

}
