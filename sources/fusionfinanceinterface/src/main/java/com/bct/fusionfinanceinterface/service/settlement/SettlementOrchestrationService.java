/**
 * 
 */
package com.bct.fusionfinanceinterface.service.settlement;

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
public class SettlementOrchestrationService {
	
	private static final Logger logger = LogManager.getLogger(SettlementOrchestrationService.class);
	
	private FullWaiveOffService   fullWaiveOffService;
	private WaiveOffStatusService    waiveOffStatusService;
	public SettlementOrchestrationService(FullWaiveOffService   fullWaiveOffService,WaiveOffStatusService waiveOffStatusService) {
		this.fullWaiveOffService = fullWaiveOffService;
		this.waiveOffStatusService=waiveOffStatusService;
	}
	
	
	public TransactionData fullWaiveOff(TransactionData transactionData) {
		logger.info("Starting fullWaiveOff Orchestration ");

		return waiveOffStatusService.updateLoanStatus(transactionData)
				.doOnSubscribe(sub -> logger.info(" Step 1: Update Loan Status"))
				.flatMap(result -> {
					logger.info("Act Step 2: Post Full WaiveOff");
					if ((result.getOrchestrationResponse().getStatus() != null
							&& result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS))
							&& (result.getOrchestrationResponse().getTransactionId() != null
									&& !result.getOrchestrationResponse().getTransactionId().trim().isEmpty())) {
						return fullWaiveOffService.fullWaiveOff(result);
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
