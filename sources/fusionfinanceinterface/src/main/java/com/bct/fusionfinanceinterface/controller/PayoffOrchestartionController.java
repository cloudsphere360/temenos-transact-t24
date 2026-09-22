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
import com.bct.fusionfinanceinterface.service.payoff.PayOffOrchestrationService;


/**
 * @@author Shunmuga Raja Gurunathan
 *
 */

@RestController
public class PayoffOrchestartionController {
	private static final Logger logger = LogManager.getLogger(PayoffOrchestartionController.class);
	
	@Autowired 
	private PayOffOrchestrationService payOffOrchestrationService;
	
	@PostMapping("/orchestration/payoff")
	public ResponseEntity<OrchestrationResponse> payOffCreation(@RequestBody String transactionDataStr,
																	@RequestParam(required = false) String companyId,
																	@RequestHeader (value = HttpHeaders.AUTHORIZATION, required = false) String accessToken) {
		TransactionData transactionData = new TransactionData();
		transactionData.setCompanyId(companyId);
		transactionData.setAccessToken(accessToken);
		try {
			transactionData = payOffOrchestrationService
					.payoffCreation(getTransactionDataForCreatePayOff(transactionData, transactionDataStr));
			logger.info("collectionCreation orchestration succeeded for payload: {}");
			return OrchestrationResponseMapper.buildHttpResponse(transactionData.getHttpStatus(),transactionData.getOrchestrationResponse());
			//return ResponseEntity.ok(transactionData.getOrchestrationResponse());
		} catch (Exception e) {
			logger.error("collectionCreation orchestration failed for payload: {}", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(transactionData.getOrchestrationResponse());
		}

	}
	
	private TransactionData getTransactionDataForCreatePayOff(TransactionData transactionData,String transactionDataStr) {
		OrchestrationRequest orchestrationRequest = new OrchestrationRequest();
		try {
			 	orchestrationRequest.setPayOffDetails(transactionDataStr);
		} catch (Exception e) {
			logger.error(" Orchestration ::  parse getTransactionData :: ", "", e);
		}
		transactionData.setOrchestrationRequest(orchestrationRequest);
		return transactionData;
	}
	

}
