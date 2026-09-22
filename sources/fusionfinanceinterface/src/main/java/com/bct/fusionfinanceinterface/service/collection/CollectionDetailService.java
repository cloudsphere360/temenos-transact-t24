/**
 * 
 */
package com.bct.fusionfinanceinterface.service.collection;

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
public class CollectionDetailService {
	
	
	private static final Logger logger = LogManager.getLogger(CollectionDetailService.class);
	
	 @Value("${fusion.loan.collection.response.get.url}")
	 private String collectionDetailsGetURL;
	 
	@Autowired
	private WebClient webClient;
	
	@Autowired 
	CommonUtil commonUtil;
 
    public CollectionDetailService (WebClient webClient) {
        this.webClient = webClient;
    }
	
	public Mono<TransactionData> getCollectionDetail(TransactionData data) {
		WebClient.RequestHeadersSpec<?> requestSpec =

				webClient.get().uri(
						uriBuilder -> uriBuilder.path(collectionDetailsGetURL).queryParam(CommonConstants.Transaction_ID,
								data.getOrchestrationResponse().getTransactionId()).build());
		//Optional.ofNullable(data.getCompanyId()).ifPresent(id -> requestSpec.header(CommonConstants.COMPANY_ID, id));
		//Optional.ofNullable(data.getAccessToken()).ifPresent(token -> requestSpec.header(HttpHeaders.AUTHORIZATION, data.getAccessToken()));
		
		if (data.getAccessToken() != null) {
			requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, data.getAccessToken());
		}if (data.getCompanyId() != null) {
			requestSpec = requestSpec.header(CommonConstants.COMPANY_ID,data.getCompanyId());
		}
		return requestSpec.retrieve().bodyToMono(String.class)
				.flatMap(response -> handleCollectionDetailsResponse(response, data))
				.doOnError(e -> logger.error("getCollectionDetail # Error calling GET API: {}", e.getMessage()));
	}

	private Mono<TransactionData> handleCollectionDetailsResponse(String t24Response, TransactionData data) {
		logger.info(" handle # getCollectionDetail GET  : {}. Status: {}",
				data.getOrchestrationResponse().getCustomerId(), t24Response);
		logger.debug(" handle #  getCollectionDetail  Status Response: {}", t24Response);
		try {

			JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
			if (commonUtil.isSucsess(root)) {
				data.getOrchestrationResponse().setStatus(CommonConstants._SUCCESS);
				data.getOrchestrationResponse().setTransactionDetails(root.get(CommonConstants._BODY).toString());
				logger.info(" handle #  getCollectionDetail GET  successful :: ");
			}

			else {
				data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
				data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
				logger.info(" handle #  getCollectionDetail  GET  failed  :: " + t24Response);
			}
		} catch (Exception e) {
			data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
			data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
			logger.error(" handle #  getCollectionDetail GET failed  :: " + e.getMessage());
		}
		return Mono.just(data);
	}

}
