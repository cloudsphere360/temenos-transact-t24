/**
 * 
 */
package com.bct.fusionfinanceinterface.service.collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.bct.fusionfinanceinterface.controller.CollectionOrchestrationController;
import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.bct.fusionfinanceinterface.model.TransactionData;
import com.bct.fusionfinanceinterface.util.CommonConstants;
import com.bct.fusionfinanceinterface.util.CommonUtil;

import reactor.core.publisher.Mono;

/**
 * @@author Shunmuga Raja Gurunathan
 *
 */
@Service
public class CollectionOrchestrationService {
	
	private static final Logger logger = LogManager.getLogger(CollectionOrchestrationService.class);
	
	private CollectionService collectionService;
	private CollectionDetailService    collectionDetailService;
	public CollectionOrchestrationService(CollectionService collectionService,CollectionDetailService collectionDetailService) {
		this.collectionService = collectionService;
		this.collectionDetailService=collectionDetailService;
	}
	
	
	public TransactionData collectionCreation(TransactionData transactionData) {
		logger.info("Starting Create Collection Orchestration ");

		return collectionService.createCollection(transactionData)
				.doOnSubscribe(sub -> logger.info(" Step 1: Create Collection"))
				.flatMap(result -> {
					logger.info("Act Step 2: Get Collection Response");
					if ((result.getOrchestrationResponse().getStatus() != null
							&& result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS))
							&& (result.getOrchestrationResponse().getTransactionId() != null
									&& !result.getOrchestrationResponse().getTransactionId().trim().isEmpty())) {
						return collectionDetailService.getCollectionDetail(result);
					} else {
						logger.error(" Orchestration failed: {}");
						return Mono.just(result);
					}
				}).map(status -> transactionData).onErrorResume(ex -> {
					logger.error(" Orchestration failed: {}", ex.getMessage());
					OrchestrationResponse orchestrationResponse = null;
					if (transactionData.getOrchestrationResponse() == null)
						orchestrationResponse = new OrchestrationResponse();
					else
						orchestrationResponse = transactionData.getOrchestrationResponse();
					orchestrationResponse.setStatus(CommonConstants._FAILED);
					orchestrationResponse.setError(ex.getMessage());
					transactionData.setOrchestrationResponse(orchestrationResponse);
					return Mono.just(transactionData);
				}).block();
	}	
	


}
