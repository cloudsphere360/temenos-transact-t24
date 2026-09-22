package com.bct.fusionfinanceinterface.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrchestrationResponse {
	
    private String status;
    
	private String transactionId;
	private String transactionCreation;
	@JsonRawValue
	private String transactionDetails;
    

	
	
	private String customerId;
	private String loanSimulationId;
	private String arrangementId;
	
	
	
	
	private String customerCreation;
	private String loanSimulationCreation;
	private String loanDetailsCreation;
	private String payOffCreation;
	@JsonRawValue
	private String payOffDetails;
	@JsonIgnore
	private String accountCreation;
	

	
	@JsonIgnore 	
	private String customerResponse;
	
	@JsonIgnore 
	private String loanSimulationResponse;
	@JsonIgnore  
	private String loanDetailsResponse;
 	

	@JsonIgnore
	private String accountResponse;
	
	
	@JsonRawValue
	private String loanSimulation;
	
    @JsonRawValue
	private String message;
    
	@JsonRawValue
	private String error;

	    
    public String getCustomerResponse() {
		return customerResponse;
	}
	public void setCustomerResponse(String customerResponse) {
		this.customerResponse = customerResponse;
	}
	public String getAccountResponse() {
		return accountResponse;
	}
	public void setAccountResponse(String accountResponse) {
		this.accountResponse = accountResponse;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getCustomerCreation() {
		return customerCreation;
	}
	public void setCustomerCreation(String customerCreation) {
		this.customerCreation = customerCreation;
	}
	public String getAccountCreation() {
		return accountCreation;
	}
	public void setAccountCreation(String accountCreation) {
		this.accountCreation = accountCreation;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}
	

	
	public String getLoanSimulationId() {
		return loanSimulationId;
	}
	public void setLoanSimulationId(String loanSimulationId) {
		this.loanSimulationId = loanSimulationId;
	}
	/**
	 * @return the loanSimulationCreation
	 */
	public String getLoanSimulationCreation() {
		return loanSimulationCreation;
	}
	/**
	 * @param loanSimulationCreation the loanSimulationCreation to set
	 */
	public void setLoanSimulationCreation(String loanSimulationCreation) {
		this.loanSimulationCreation = loanSimulationCreation;
	}
	/**
	 * @return the loanSimulationResponse
	 */
	public String getLoanSimulationResponse() {
		return loanSimulationResponse;
	}
	/**
	 * @param loanSimulationResponse the loanSimulationResponse to set
	 */
	public void setLoanSimulationResponse(String loanSimulationResponse) {
		this.loanSimulationResponse = loanSimulationResponse;
	}
	/**
	 * @return the loanDetailsResponse
	 */
	public String getLoanDetailsResponse() {
		return loanDetailsResponse;
	}
	/**
	 * @param loanDetailsResponse the loanDetailsResponse to set
	 */
	public void setLoanDetailsResponse(String loanDetailsResponse) {
		this.loanDetailsResponse = loanDetailsResponse;
	}
	/**
	 * @return the loanDetailsCreation
	 */
	public String getLoanDetailsCreation() {
		return loanDetailsCreation;
	}
	/**
	 * @param loanDetailsCreation the loanDetailsCreation to set
	 */
	public void setLoanDetailsCreation(String loanDetailsCreation) {
		this.loanDetailsCreation = loanDetailsCreation;
	}
	/**
	 * @return the loanSimulation
	 */
	public String getLoanSimulation() {
		return loanSimulation;
	}
	/**
	 * @param loanSimulation the loanSimulation to set
	 */
	public void setLoanSimulation(String loanSimulation) {
		this.loanSimulation = loanSimulation;
	}
	/**
	 * @return the arrangementId
	 */
	public String getArrangementId() {
		return arrangementId;
	}
	/**
	 * @param arrangementId the arrangementId to set
	 */
	public void setArrangementId(String arrangementId) {
		this.arrangementId = arrangementId;
	}
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public String getTransactionCreation() {
		return transactionCreation;
	}
	public void setTransactionCreation(String transactionCreation) {
		this.transactionCreation = transactionCreation;
	}
	public String getTransactionDetails() {
		return transactionDetails;
	}
	public void setTransactionDetails(String transactionDetails) {
		this.transactionDetails = transactionDetails;
	}
	/**
	 * @return the payOffDetails
	 */
	public String getPayOffDetails() {
		return payOffDetails;
	}
	/**
	 * @param payOffDetails the payOffDetails to set
	 */
	public void setPayOffDetails(String payOffDetails) {
		this.payOffDetails = payOffDetails;
	}
	/**
	 * @return the payOffCreation
	 */
	public String getPayOffCreation() {
		return payOffCreation;
	}
	/**
	 * @param payOffCreation the payOffCreation to set
	 */
	public void setPayOffCreation(String payOffCreation) {
		this.payOffCreation = payOffCreation;
	}
	 
}



