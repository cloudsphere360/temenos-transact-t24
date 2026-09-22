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
@JsonPropertyOrder({"status", "responseMessage", "responseCode", "data"})
public class FfMessageFailResponseApi {
   @JsonProperty("status")
   private String status;
   @JsonProperty("responseMessage")
   private String responseMessage;
   @JsonProperty("responseCode")
   private Integer responseCode;
   @JsonProperty("data")
   private Object data;
   @JsonIgnore
   private Map<String, Object> additionalProperties = new LinkedHashMap<>();

   @JsonProperty("status")
   public String getStatus() {
      return this.status;
   }

   @JsonProperty("status")
   public void setStatus(String status) {
      this.status = status;
   }

   @JsonProperty("responseMessage")
   public String getResponseMessage() {
      return this.responseMessage;
   }

   @JsonProperty("responseMessage")
   public void setResponseMessage(String responseMessage) {
      this.responseMessage = responseMessage;
   }

   @JsonProperty("responseCode")
   public Integer getResponseCode() {
      return this.responseCode;
   }

   @JsonProperty("responseCode")
   public void setResponseCode(Integer responseCode) {
      this.responseCode = responseCode;
   }

   @JsonProperty("data")
   public Object getData() {
      return this.data;
   }

   @JsonProperty("data")
   public void setData(Object data) {
      this.data = data;
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