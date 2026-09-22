package com.bct.fusionfinanceinterface.model;

import org.springframework.http.HttpStatus;

public class TransactionData {
	
	private Customer  customer;
	
	private HttpStatus httpStatus;
	private String accessToken;
	private String  companyId;
	private String customerId; 
	private String aarrangementId;
	private String simulationId;
	
	private OrchestrationRequest orchestrationRequest;
	private OrchestrationResponse orchestrationResponse;
	/**
	 * @return the orchestrationRequest
	 */
	public OrchestrationRequest getOrchestrationRequest() {
		return orchestrationRequest;
	}
	/**
	 * @param orchestrationRequest the orchestrationRequest to set
	 */
	public void setOrchestrationRequest(OrchestrationRequest orchestrationRequest) {
		this.orchestrationRequest = orchestrationRequest;
	}
	/**
	 * @return the orchestrationResponse
	 */
	public OrchestrationResponse getOrchestrationResponse() {
		return orchestrationResponse;
	}
	/**
	 * @param orchestrationResponse the orchestrationResponse to set
	 */
	public void setOrchestrationResponse(OrchestrationResponse orchestrationResponse) {
		this.orchestrationResponse = orchestrationResponse;
	}
	/**
	 * @return the customer
	 */
	public Customer getCustomer() {
		return customer;
	}
	/**
	 * @param customer the customer to set
	 */
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	/**
	 * @return the customerId
	 */
	public String getCustomerId() {
		return customerId;
	}
	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	/**
	 * @return the aarrangementId
	 */
	public String getAarrangementId() {
		return aarrangementId;
	}
	/**
	 * @param aarrangementId the aarrangementId to set
	 */
	public void setAarrangementId(String aarrangementId) {
		this.aarrangementId = aarrangementId;
	}
	/**
	 * @return the companyId
	 */
	public String getCompanyId() {
		return companyId;
	}
	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	/**
	 * @return the httpStatus
	 */
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
	/**
	 * @param httpStatus the httpStatus to set
	 */
	public void setHttpStatus(HttpStatus httpStatus) {
		this.httpStatus = httpStatus;
	}
	/**
	 * @return the accessToken
	 */
	public String getAccessToken() {
		return accessToken;
	}
	/**
	 * @param accessToken the accessToken to set
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	/**
	 * @return the simulationId
	 */
	public String getSimulationId() {
		return simulationId;
	}
	/**
	 * @param simulationId the simulationId to set
	 */
	public void setSimulationId(String simulationId) {
		this.simulationId = simulationId;
	}
 
	
	

}
