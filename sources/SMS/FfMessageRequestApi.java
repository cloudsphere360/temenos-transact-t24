package com.tem.msg.fusion;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({"templateType", "loanAccountNumber", "mobileNumber", "customerId", "params", "isOtp", "applicationId","userId" ,"userName"})
public class FfMessageRequestApi {
   @JsonProperty("templateType")
   private String templateType;
   @JsonProperty("loanAccountNumber")
   private String loanAccountNumber;
   @JsonProperty("mobileNumber")
   private String mobileNumber;
   @JsonProperty("userId")
   private String userId;
   @JsonProperty("userName")
   private String userName;
   @JsonProperty("customerId")
   private Integer customerId;
   @JsonProperty("params")
   private Params params;
   @JsonProperty("isOtp")
   private Boolean isOtp;
   @JsonProperty("applicationId")
   private Integer applicationId;
   @JsonIgnore
   private Map<String, Object> additionalProperties = new LinkedHashMap<>();

   @JsonProperty("templateType")
   public String getTemplateType() {
      return this.templateType;
   }

   @JsonProperty("templateType")
   public void setTemplateType(String templateType) {
      this.templateType = templateType;
   }

   @JsonProperty("loanAccountNumber")
   public String getLoanAccountNumber() {
      return this.loanAccountNumber;
   }

   @JsonProperty("loanAccountNumber")
   public void setLoanAccountNumber(String loanAccountNumber) {
      this.loanAccountNumber = loanAccountNumber;
   }

   @JsonProperty("mobileNumber")
   public String getMobileNumber() {
      return this.mobileNumber;
   }

   @JsonProperty("mobileNumber")
   public void setMobileNumber(String mobileNumber) {
      this.mobileNumber = mobileNumber;
   }
   
   @JsonProperty("userId")
   public String getUserId() {
      return this.userId;
   }

   @JsonProperty("userId")
   public void setUserId(String userId) {
      this.userId = userId;
   }
   
   @JsonProperty("userName")
   public String getUserName() {
      return this.userName;
   }

   @JsonProperty("userName")
   public void setUserName(String userName) {
      this.userName = userName;
   }

   @JsonProperty("customerId")
   public Integer getCustomerId() {
      return this.customerId;
   }

   @JsonProperty("customerId")
   public void setCustomerId(Integer customerId) {
      this.customerId = customerId;
   }

   @JsonProperty("params")
   public Params getParams() {
      return this.params;
   }

   @JsonProperty("params")
   public void setParams(Params params) {
      this.params = params;
   }

   @JsonProperty("isOtp")
   public Boolean getIsOtp() {
      return this.isOtp;
   }

   @JsonProperty("isOtp")
   public void setIsOtp(Boolean isOtp) {
      this.isOtp = isOtp;
   }

   @JsonProperty("applicationId")
   public Integer getApplicationId() {
      return this.applicationId;
   }

   @JsonProperty("applicationId")
   public void setApplicationId(Integer applicationId) {
      this.applicationId = applicationId;
   }

   @JsonAnyGetter
   public Map<String, Object> getAdditionalProperties() {
      return this.additionalProperties;
   }

   @JsonAnySetter
   public void setAdditionalProperty(String name, Object value) {
      this.additionalProperties.put(name, value);
   }
}