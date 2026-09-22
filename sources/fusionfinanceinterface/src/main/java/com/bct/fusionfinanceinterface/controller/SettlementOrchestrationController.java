/**
 * 
 */
package com.bct.fusionfinanceinterface.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bct.fusionfinanceinterface.config.OrchestrationResponseMapper;
import com.bct.fusionfinanceinterface.model.OrchestrationRequest;
import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.bct.fusionfinanceinterface.model.TransactionData;
import com.bct.fusionfinanceinterface.service.settlement.SettlementOrchestrationService;


/**
 * @@author Shunmuga Raja Gurunathan
 *
 */

@RestController
public class SettlementOrchestrationController {
	private static final Logger logger = LogManager.getLogger(SettlementOrchestrationController.class);
	
	@Autowired 
	private SettlementOrchestrationService settlementOrchestrationService   ;
	
	@PostMapping("/orchestration/fullwaiveoff")
	public ResponseEntity<OrchestrationResponse> fullWaiveOff(@RequestBody String transactionDataStr,
																	@RequestParam(required = false) String companyId,
																	@RequestHeader (value = HttpHeaders.AUTHORIZATION, required = false) String accessToken) {
		TransactionData transactionData = new TransactionData();
		transactionData.setCompanyId(companyId);
		transactionData.setAccessToken(accessToken);
		try {
			transactionData = settlementOrchestrationService
					.fullWaiveOff(getTransactionDataForFullWaiveOff(transactionData, transactionDataStr));
			logger.info("fullwaiveoff orchestration succeeded for payload: {}");
			return OrchestrationResponseMapper.buildHttpResponse(transactionData.getHttpStatus(),transactionData.getOrchestrationResponse()); 
			//return ResponseEntity.ok(transactionData.getOrchestrationResponse());
		} catch (Exception e) {
			logger.error("fullwaiveoff orchestration failed for payload: {}", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(transactionData.getOrchestrationResponse());
		}

	}
	
	private TransactionData getTransactionDataForFullWaiveOff(TransactionData transactionData,String transactionDataStr) {
		OrchestrationRequest orchestrationRequest = new OrchestrationRequest();
		try {
			 	orchestrationRequest.setFullWaiveOffDetails(transactionDataStr);
		} catch (Exception e) {
			logger.error(" Orchestration ::  parse getTransactionData :: ", "", e);
		}
		transactionData.setOrchestrationRequest(orchestrationRequest);
		return transactionData;
	}
	

}
