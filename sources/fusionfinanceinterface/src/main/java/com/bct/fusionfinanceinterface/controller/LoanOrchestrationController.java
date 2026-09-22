package com.bct.fusionfinanceinterface.controller;



/**
 * @@author Shunmuga Raja Gurunathan
 *
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.bct.fusionfinanceinterface.model.TransactionData;
import com.bct.fusionfinanceinterface.service.LoanOrchestrationService;
import com.bct.fusionfinanceinterface.util.CommonConstants;
import com.bct.fusionfinanceinterface.config.OrchestrationResponseMapper;
import com.bct.fusionfinanceinterface.model.OrchestrationRequest;
import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RestController
public class LoanOrchestrationController {

	private static final Logger logger = LogManager.getLogger(LoanOrchestrationController.class);

	@Autowired
	private   LoanOrchestrationService loanOrchestrationService;

	@Value("${fusion.customer.create.url}")
	private String createCustomerURL;
	
	@Value("${fusion.loan.simulation.create.url}")
	private String createLoanSimulationURL;
	
	@Value("${fusion.loan.detail.update.url}")
	private String updateLoanDetailsURL;
	
	@Value("${fusion.loan.schedule.get.url}")
	private String loanDetailsScheduleGetURL;
	
	@Value("${fusion.loan.activation.put.url}")
	private String loanActivationPutURL;
	
	
	@PostMapping("/orchestration/customerloansimulation")
	public ResponseEntity<OrchestrationResponse> customerLoanSimulation(@RequestBody String transactionDataStr,
																		@RequestParam(required = false) String companyId,
																		@RequestHeader (value = HttpHeaders.AUTHORIZATION, required = false) String accessToken
			) {
		TransactionData transactionData = new TransactionData();
		transactionData.setCompanyId(companyId);
		transactionData.setAccessToken(accessToken);
		try {
			transactionData = loanOrchestrationService.orchestrateCustomerAccountLoan(getTransactionDataForcustomerLoanSimulation(transactionData,transactionDataStr));
			logger.info("Manual orchestration succeeded for payload: {}");
			return OrchestrationResponseMapper.buildHttpResponse(transactionData.getHttpStatus(),transactionData.getOrchestrationResponse()); 	 
			//return ResponseEntity.ok(transactionData.getOrchestrationResponse());
		} catch (Exception e) {
			logger.error("Manual orchestration failed for payload: {}", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(transactionData.getOrchestrationResponse());
		}

	}
	
	@PutMapping("/orchestration/loansimulation/{id}")
	public ResponseEntity<OrchestrationResponse> updateLoanSimulation(@PathVariable(CommonConstants._ID) String id,@RequestBody String transactionDataStr,
																		@RequestParam(required = false) String companyId,
																		@RequestHeader (value = HttpHeaders.AUTHORIZATION, required = false) String accessToken
			) {
		TransactionData transactionData = new TransactionData();
		transactionData.setSimulationId(id);
		transactionData.setCompanyId(companyId);
		transactionData.setAccessToken(accessToken);
		try {
			transactionData = loanOrchestrationService.orchestrateUpdateLoanSimulation(getTransactionDataForUpdateLoanSimulation(transactionData,transactionDataStr));
			logger.info("Manual orchestration succeeded for payload: {}");
			return OrchestrationResponseMapper.buildHttpResponse(transactionData.getHttpStatus(),transactionData.getOrchestrationResponse()); 	 
			//return ResponseEntity.ok(transactionData.getOrchestrationResponse());
		} catch (Exception e) {
			logger.error("Manual orchestration failed for payload: {}", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(transactionData.getOrchestrationResponse());
		}

	}

	@PutMapping("/orchestration/loanactivation/{id}")
	public ResponseEntity<OrchestrationResponse> loanActivation(@PathVariable(CommonConstants._ID) String id,  @RequestBody String transactionDataStr,
																@RequestParam(required = false) String companyId,@RequestHeader (value = HttpHeaders.AUTHORIZATION, required = false) String accessToken					
			) {
		TransactionData transactionData = new TransactionData();
		transactionData.setCompanyId(companyId);
		transactionData.setAccessToken(accessToken);
		try {
			transactionData.setAarrangementId(id);
			transactionData = loanOrchestrationService.orchestrateLoanActivation(getTransactionDataForLoanActivation(transactionData,transactionDataStr));
			logger.info("LoanActivation orchestration succeeded for payload: {}");
			return OrchestrationResponseMapper.buildHttpResponse(transactionData.getHttpStatus(),transactionData.getOrchestrationResponse()); 
			//return ResponseEntity.ok(transactionData.getOrchestrationResponse());
		} catch (Exception e) {
			logger.error("LoanActivation orchestration failed for payload: {}", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(transactionData.getOrchestrationResponse());
		}

	}
	
	private TransactionData getTransactionDataForLoanActivation(TransactionData transactionData,String transactionDataStr) {
		OrchestrationRequest orchestrationRequest = new OrchestrationRequest();
		orchestrationRequest.setLoanActivationURL(loanActivationPutURL);
		orchestrationRequest.setLoanDetailsScheduleGetURL(loanDetailsScheduleGetURL);
		try {
			 	orchestrationRequest.setLoanSimulation(transactionDataStr);
		} catch (Exception e) {
			logger.error(" Orchestration ::  parse getTransactionData :: ", "", e);
		}
		transactionData.setOrchestrationRequest(orchestrationRequest);
		return transactionData;
	}
	
	private TransactionData getTransactionDataForcustomerLoanSimulation(TransactionData transactionData,String transactionDataStr) {
		OrchestrationRequest orchestrationRequest = new OrchestrationRequest();
		orchestrationRequest.setCustomerCreateURL(createCustomerURL);
		orchestrationRequest.setLoanSimulationCreateURL(createLoanSimulationURL);
		orchestrationRequest.setLoanDetailsUpdateURL(updateLoanDetailsURL);
		orchestrationRequest.setLoanDetailsScheduleGetURL(loanDetailsScheduleGetURL);
		try {
			JsonObject transactionDataJSON = JsonParser.parseString(transactionDataStr).getAsJsonObject();
			// Get ID from "header"
			if (transactionDataJSON.get(CommonConstants._CUSTOMER) != null) {
				logger.debug("customer Request :: \n" + transactionDataJSON.get(CommonConstants._CUSTOMER).toString());
				transactionData.setCustomerId( transactionDataJSON.getAsJsonObject(CommonConstants._CUSTOMER).get(CommonConstants._ID).getAsString());
				orchestrationRequest.setCustomer(transactionDataJSON.get(CommonConstants._CUSTOMER).toString());
			}
			if (transactionDataJSON.get(CommonConstants.LOAN_SIMULATION) != null) {
				logger.debug("loanSimulation Request :: \n" + transactionDataJSON.get(CommonConstants.LOAN_SIMULATION).toString());
				orchestrationRequest.setLoanSimulation(transactionDataJSON.get(CommonConstants.LOAN_SIMULATION).toString());
			}
			if (transactionDataJSON.get(CommonConstants.LOAN_DETAILS ) != null) {
				logger.debug("loanDetails Request :: \n" + transactionDataJSON.get(CommonConstants.LOAN_DETAILS).toString());
				orchestrationRequest.setLoanDetails(transactionDataJSON.get(CommonConstants.LOAN_DETAILS).toString());
			}
		} catch (Exception e) {
			logger.error(" Orchestration ::  parse getTransactionData :: ", "", e);
		}

		transactionData.setOrchestrationRequest(orchestrationRequest);
		return transactionData;
	}
	
	
	private TransactionData getTransactionDataForUpdateLoanSimulation(TransactionData transactionData,String transactionDataStr) {
		OrchestrationRequest orchestrationRequest = new OrchestrationRequest();
		orchestrationRequest.setCustomerCreateURL(createCustomerURL);
		orchestrationRequest.setLoanSimulationCreateURL(createLoanSimulationURL);
		orchestrationRequest.setLoanDetailsUpdateURL(updateLoanDetailsURL);
		orchestrationRequest.setLoanDetailsScheduleGetURL(loanDetailsScheduleGetURL);
		try {
			JsonObject transactionDataJSON = JsonParser.parseString(transactionDataStr).getAsJsonObject();
			if (transactionDataJSON.get(CommonConstants.LOAN_SIMULATION) != null) {
				logger.debug("loanSimulation Request :: \n" + transactionDataJSON.get(CommonConstants.LOAN_SIMULATION).toString());
				orchestrationRequest.setLoanSimulation(transactionDataJSON.get(CommonConstants.LOAN_SIMULATION).toString());
			}
			if (transactionDataJSON.get(CommonConstants.LOAN_DETAILS ) != null) {
				logger.debug("loanDetails Request :: \n" + transactionDataJSON.get(CommonConstants.LOAN_DETAILS).toString());
				orchestrationRequest.setLoanDetails(transactionDataJSON.get(CommonConstants.LOAN_DETAILS).toString());
			}
		} catch (Exception e) {
			logger.error(" Orchestration ::  parse getTransactionData :: ", "", e);
		}

		transactionData.setOrchestrationRequest(orchestrationRequest);
		return transactionData;
	}

}