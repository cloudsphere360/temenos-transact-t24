package com.bct.fusionfinanceinterface.model;

public class Customer {
	
    private Long recordId; // This is now the primary key
   
    
    private Long id;
	
    private String name;
	
	private String email;
    
    private String customerID;
    
    private String accountID;
    
    private String loanID;
    
    private String apiResponse;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
	/**
	 * @return the apiResponse
	 */
    
	public String getApiResponse() {
		return apiResponse;
	}
	/**
	 * @param apiResponse the apiResponse to set
	 */
	public void setApiResponse(String apiResponse) {
		this.apiResponse = apiResponse;
	}
	public String getCustomerID() {
		return customerID;
	}
	public void setCustomerID(String customerID) {
		this.customerID = customerID;
	}
	public String getAccountID() {
		return accountID;
	}
	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}
	public String getLoanID() {
		return loanID;
	}
	public void setLoanID(String loanID) {
		this.loanID = loanID;
	}
 
 
}

 