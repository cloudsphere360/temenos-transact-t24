/**
 * 
 */
package com.bct.fusionfinanceinterface.service;

/**
 * @@author Shunmuga Raja Gurunathan
 *
 */

import reactor.core.publisher.Mono;

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
@Service
public class RepaymentScheduleService {
 
	 @Value("${fusion.loan.schedule.get.url}")
	 private String loanScheduleGetURL;
	 
	 @Autowired 
	 CommonUtil commonUtil;
	 
	@Autowired
	private WebClient webClient;
	
    	
	private static final Logger logger = LogManager.getLogger(RepaymentScheduleService.class);
	

/*    public BulkLoanService(WebClient webClient) {
        this.webClient = webClient;
    }*/

    public RepaymentScheduleService(WebClient webClient) {
        this.webClient = webClient;
    }
	
	public Mono<TransactionData> getRepaymentSchedule(TransactionData data) {
		loanScheduleGetURL = loanScheduleGetURL == null ? data.getOrchestrationRequest().getLoanDetailsScheduleGetURL()
				: loanScheduleGetURL;
		logger.debug(" GET #  RepaymentSchedule  URL :: " + loanScheduleGetURL
				+ data.getOrchestrationResponse().getLoanSimulationId());
		WebClient.RequestHeadersSpec<?> requestSpec =

				webClient.get()
						.uri(uriBuilder -> uriBuilder.path(loanScheduleGetURL).queryParam(CommonConstants.SIMULATION_ID,
								data.getOrchestrationResponse().getLoanSimulationId()).build());
		//Optional.ofNullable(data.getCompanyId()).ifPresent(id -> requestSpec.header(CommonConstants.COMPANY_ID, id));
		//Optional.ofNullable(data.getAccessToken()).ifPresent(token -> requestSpec.header(HttpHeaders.AUTHORIZATION, data.getAccessToken()));
		if (data.getAccessToken() != null) {
			requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, data.getAccessToken());
		}if (data.getCompanyId() != null) {
			requestSpec = requestSpec.header(CommonConstants.COMPANY_ID,data.getCompanyId());
		}
		return requestSpec.retrieve().bodyToMono(String.class)
				.flatMap(response -> handleRepaymentScheduleResponse(response, data))
				.doOnError(e -> logger.error("RepaymentSchedule # Error calling GET API: {}", e.getMessage()));
	}
	
	private Mono<TransactionData> handleRepaymentScheduleResponse(String t24Response, TransactionData data) {
        logger.info(" handle # RepaymentSchedule GET  : {}. Status: {}", data.getOrchestrationResponse().getCustomerId(), t24Response);
        logger.debug(" handle #  RepaymentSchedule  Status Response: {}", t24Response);
        try {
        
    	JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
   	 if(root.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._STATUS).getAsString().equals(CommonConstants._SUCCESS))
   	 {
   		//data.getOrchestrationResponse().setRepaymentScheduleFetch("success");
   		//data.getOrchestrationResponse().setRepaymentScheduleResponse(t24Response);
   		data.getOrchestrationResponse().setStatus(CommonConstants._SUCCESS);
   		data.getOrchestrationResponse().setLoanSimulation(root.get(CommonConstants._BODY).toString());
   		 logger.info(" handle #  RepaymentSchedule GET  successful :: ");
   	 }
   	 
   	 else
   	 {
   		data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
		data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
		
   		//data.getOrchestrationResponse().setRepaymentScheduleFetch("failed");
   		//data.getOrchestrationResponse().setRepaymentScheduleResponse(t24Response);
   		 logger.info(" handle #  RepaymentSchedule  GET  failed  :: "+t24Response);
   	 }
        }
   	catch (Exception e)
	{
   		//data.getOrchestrationResponse().setLoanId("");
   		//data.getOrchestrationResponse().setRepaymentScheduleFetch("failed");
   		//data.getOrchestrationResponse().setRepaymentScheduleFetch(t24Response);
   		data.getOrchestrationResponse().setStatus(CommonConstants._FAILED);
   		data.getOrchestrationResponse().setMessage(commonUtil.getMessageFromResponse(t24Response));
		 logger.error(" handle #  RepaymentSchedule GET failed  :: "+e.getMessage());
	}
   	 return Mono.just(data);
    }
	
	
  
}