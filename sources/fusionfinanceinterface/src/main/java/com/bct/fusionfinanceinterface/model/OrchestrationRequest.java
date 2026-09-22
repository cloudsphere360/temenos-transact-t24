package com.bct.fusionfinanceinterface.model;


public class OrchestrationRequest {
	
	private String customerCreateURL;
	private String loanSimulationCreateURL;
	private String loanDetailsUpdateURL;
	private String loanDetailsScheduleGetURL;
	private String loanActivationURL;
	
	
	private String customer;
	private String loanSimulation;
	private String loanDetails;
	private String aarrangementId;
	
	
	
	//Collection
	private String payOffDetails;
	private String collectionDetails;
	private String fullWaiveOffDetails;
 
	
	public String getLoanDetails() {
		return loanDetails;
	}
	
	public void setLoanDetails(String loanDetails) {
		this.loanDetails = loanDetails;
	}

	/**
	 * @return the customer
	 */
	public String getCustomer() {
		return customer;
	}

	/**
	 * @param customer the customer to set
	 */
	public void setCustomer(String customer) {
		this.customer = customer;
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

	public String getCustomerCreateURL() {
		return customerCreateURL;
	}

	public void setCustomerCreateURL(String customerCreateURL) {
		this.customerCreateURL = customerCreateURL;
	}


	/**
	 * @return the loanDetailsUpdateURL
	 */
	public String getLoanDetailsUpdateURL() {
		return loanDetailsUpdateURL;
	}

	/**
	 * @param loanDetailsUpdateURL the loanDetailsUpdateURL to set
	 */
	public void setLoanDetailsUpdateURL(String loanDetailsUpdateURL) {
		this.loanDetailsUpdateURL = loanDetailsUpdateURL;
	}

	/**
	 * @return the loanDetailsScheduleGetURL
	 */
	public String getLoanDetailsScheduleGetURL() {
		return loanDetailsScheduleGetURL;
	}

	/**
	 * @param loanDetailsScheduleGetURL the loanDetailsScheduleGetURL to set
	 */
	public void setLoanDetailsScheduleGetURL(String loanDetailsScheduleGetURL) {
		this.loanDetailsScheduleGetURL = loanDetailsScheduleGetURL;
	}

	/**
	 * @return the loanSimulationCreateURL
	 */
	public String getLoanSimulationCreateURL() {
		return loanSimulationCreateURL;
	}

	/**
	 * @param loanSimulationCreateURL the loanSimulationCreateURL to set
	 */
	public void setLoanSimulationCreateURL(String loanSimulationCreateURL) {
		this.loanSimulationCreateURL = loanSimulationCreateURL;
	}

	/**
	 * @return the loanActivationURL
	 */
	public String getLoanActivationURL() {
		return loanActivationURL;
	}

	/**
	 * @param loanActivationURL the loanActivationURL to set
	 */
	public void setLoanActivationURL(String loanActivationURL) {
		this.loanActivationURL = loanActivationURL;
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
	 * @return the collectionDetails
	 */
	public String getCollectionDetails() {
		return collectionDetails;
	}

	/**
	 * @param collectionDetails the collectionDetails to set
	 */
	public void setCollectionDetails(String collectionDetails) {
		this.collectionDetails = collectionDetails;
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
	 * @return the fullWaiveOffDetails
	 */
	public String getFullWaiveOffDetails() {
		return fullWaiveOffDetails;
	}

	/**
	 * @param fullWaiveOffDetails the fullWaiveOffDetails to set
	 */
	public void setFullWaiveOffDetails(String fullWaiveOffDetails) {
		this.fullWaiveOffDetails = fullWaiveOffDetails;
	}


}