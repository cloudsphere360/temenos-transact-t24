package com.bct.fusionfinanceinterface.simulation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.bct.fusionfinanceinterface.model.TransactionData;
import com.bct.fusionfinanceinterface.service.CustomerService;
import com.bct.fusionfinanceinterface.service.LoanActivationService;
import com.bct.fusionfinanceinterface.service.LoanDetailService;
import com.bct.fusionfinanceinterface.service.LoanSimulationService;
import com.bct.fusionfinanceinterface.service.RepaymentScheduleService;
import com.bct.fusionfinanceinterface.util.CommonConstants;
import com.bct.fusionfinanceinterface.util.CommonUtil;

import reactor.core.publisher.Mono;

@Service

public class SimulationOrchestrationService {
    private static final Logger logger = LogManager.getLogger(SimulationOrchestrationService.class);
   
	private CustomerService customerService;
	private LoanSimulationService loanSimulationService;
	private LoanDetailService loanDetailService;
	private RepaymentScheduleService repaymentScheduleService;
	private LoanActivationService loanActivationService;
	@Autowired
	private CommonUtil commonUtil;

	public SimulationOrchestrationService(CustomerService customerService, LoanSimulationService loanSimulationService,
			LoanDetailService loanDetailService, RepaymentScheduleService repaymentScheduleService,
			LoanActivationService loanActivationService) {
		this.customerService = customerService;
		this.loanSimulationService = loanSimulationService;
		this.loanDetailService = loanDetailService;
		this.repaymentScheduleService = repaymentScheduleService;
		this.loanActivationService = loanActivationService;

	}
    
 
	public TransactionData createSimulation(TransactionData transactionData) {
		logger.info("Starting orchestration for customer ID :: ");

		return customerService.createCustomer(transactionData)
				.doOnSubscribe(sub -> logger.info("Step 1: Creating customer...")).flatMap(result -> {
					logger.info("Step 2: Creating loan Simulation...");
					if ((result.getOrchestrationResponse().getStatus() != null
							&& result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS))
							&& (result.getOrchestrationResponse().getCustomerId() != null
									&& !result.getOrchestrationResponse().getCustomerId().trim().isEmpty())) {
						return loanSimulationService.createLoanSimulation(result);
					} else {
						logger.error(" Orchestration failed: {}");
						return Mono.just(result);
					}
				}).
				flatMap(result -> {
					logger.info("Step 3: Update Loan Details...");
					if ((result.getOrchestrationResponse().getStatus() != null
							&& result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS))
							&& (result.getOrchestrationResponse().getArrangementId()!= null
									&& !result.getOrchestrationResponse().getArrangementId().trim().isEmpty())) {
						return loanDetailService.createLoanDeails(result);
					} else {
						logger.error(" Orchestration failed: {}");
						return Mono.just(result);
					}

				})//.flatMap(commonUtil::delayInSec)
				/*
				 * . flatMap(result -> {
				 * logger.info("Step 4: Get Customer Loan Repayment Schedule Details..."); if
				 * ((result.getOrchestrationResponse().getStatus() != null &&
				 * result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS
				 * )) && (result.getOrchestrationResponse().getLoanSimulationId() != null &&
				 * !result.getOrchestrationResponse().getLoanSimulationId().trim().isEmpty())) {
				 * return repaymentScheduleService.getRepaymentSchedule(result); } else {
				 * logger.error(" Orchestration failed: {}"); return Mono.just(result); } })
				 */.map(status -> transactionData).onErrorResume(ex -> {
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
	
	
	
	public TransactionData modifySimulation(TransactionData transactionData) {
		logger.info("Starting orchestration for customer ID :: ");

		return loanSimulationService.updateLoanSimulation(transactionData)
				.doOnSubscribe(sub -> logger.info("Step 1: Update Loan simulation")).
				flatMap(result -> {
					logger.info("Step 2: Update Loan Details...");
					if ((result.getOrchestrationResponse().getStatus() != null
							&& result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS))
							&& (result.getOrchestrationResponse().getArrangementId()!= null
									&& !result.getOrchestrationResponse().getArrangementId().trim().isEmpty())) {
						return loanDetailService.createLoanDeails(result);
					} else {
						logger.error(" Orchestration failed: {}");
						return Mono.just(result);
					}

				})/*
					 * .flatMap(commonUtil::delayInSec). flatMap(result -> {
					 * logger.info("Step 3: Get Customer Loan Repayment Schedule Details..."); if
					 * ((result.getOrchestrationResponse().getStatus() != null &&
					 * result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS
					 * )) && (result.getOrchestrationResponse().getLoanSimulationId() != null &&
					 * !result.getOrchestrationResponse().getLoanSimulationId().trim().isEmpty())) {
					 * return repaymentScheduleService.getRepaymentSchedule(result); } else {
					 * logger.error(" Orchestration failed: {}"); return Mono.just(result); } })
					 */.map(status -> transactionData).onErrorResume(ex -> {
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
	
	public TransactionData activateSimulation(TransactionData transactionData) {
		logger.info("Starting Loan Activation Orchestration ");

		return loanActivationService.LoanActivationUpdate(transactionData)
				.doOnSubscribe(sub -> logger.info("Act Step 1: Loan Activation"))
				.flatMap(result -> {
					logger.info("Act Step 2: Get Customer Loan Repayment Schedule Details...");
					if ((result.getOrchestrationResponse().getStatus() != null
							&& result.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS))
							&& (result.getOrchestrationResponse().getLoanSimulationId() != null
									&& !result.getOrchestrationResponse().getLoanSimulationId().trim().isEmpty())) {
						return repaymentScheduleService.getRepaymentSchedule(result);
					} else {
						logger.error(" Orchestration failed: {}");
						return Mono.just(result);
					}
				}).map(status -> transactionData).onErrorResume(ex -> {
					logger.error(" Orchestration failed: {}", ex.getMessage());
					OrchestrationResponse orchestrationResponse = new OrchestrationResponse();
					orchestrationResponse.setStatus(CommonConstants._FAILED);
					orchestrationResponse.setError(ex.getMessage());
					transactionData.setOrchestrationResponse(orchestrationResponse);
					return Mono.just(transactionData);
				}).block();
	}	
	

	
	
	
	
	
}