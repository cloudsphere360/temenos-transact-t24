/**
 * 
 */
package com.bct.fusionfinanceinterface.service.payoff;
 
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class PayOffOrchestrationService {
	
	private static final Logger logger = LogManager.getLogger(PayOffOrchestrationService.class);
	
	private PayOffService payOffService;
	private PayOffDetailService    payOffDetailService;
	public PayOffOrchestrationService(PayOffService payOffService,PayOffDetailService    payOffDetailService) {
		this.payOffService = payOffService;
		this.payOffDetailService=payOffDetailService;
	}
	@Autowired
	private CommonUtil commonUtil;
	
	public TransactionData payoffCreation(TransactionData transactionData) {
		logger.info("Starting Create PayOff Orchestration ");

		return payOffService.createPayOff(transactionData)
				.doOnSubscribe(sub -> logger.info(" Step 1: Create PayOff"))
				
				.flatMap(commonUtil::delayPayOff )
				.flatMap(result -> {
					logger.info("Act Step 2: Get PayOff Response");
					if ((result.getOrchestrationResponse().getStatus() != null
							&& result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS))
							&& (result.getOrchestrationResponse().getArrangementId() != null
									&& !result.getOrchestrationResponse().getArrangementId().trim().isEmpty())) {
						return payOffDetailService.getPayOffDetail(result);
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
