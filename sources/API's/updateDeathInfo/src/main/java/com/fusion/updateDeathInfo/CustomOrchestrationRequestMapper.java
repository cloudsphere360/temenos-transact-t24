/*******************************************************************************
 * Copyright © Temenos Headquarters SA 2021.  All rights reserved.
 *******************************************************************************/
package com.fusion.updateDeathInfo;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

/**
 * TODO: Document me!
 *
 * @author ntejaswi
 *
 */
public class CustomOrchestrationRequestMapper implements Processor{
    /*Scenario 1: Map few fields from previous response when body is JSONArray
      Scenario 2: Map few fields from previous response when body is JSONObject
      Scenario 3: Add same field in response to payload
      Scenario 4: Add value of a field in response to differnt fieldname in payload
      Scenario 5: Remove few fields in response and send remaining fields in payload
      Scenario 6: To map multivalues to single field
      Scenario 7: To map multivalue to multivalue with diff groupName
      Scenario 8: To map multivalue to diff multivalue fieldName with different groupName
      Scenario 9: Change the datatype of field and set in payload
     */  
    @Override
    public void process(Exchange exchange) throws Exception {
        //Map the fields from response body
        JSONObject payload = new JSONObject();
        JSONObject response = null;
        final Object oBody = exchange.getIn().getBody();
        DocumentContext jsonContext = null;
        if (oBody instanceof Map) {
            jsonContext = JsonPath.parse(oBody);
        }
        if (oBody instanceof ObjectNode) {
            jsonContext = JsonPath.parse(oBody.toString());
        }
        if (oBody instanceof String) {
            jsonContext = JsonPath.parse(oBody.toString());
        }
        
        response = new JSONObject(jsonContext.jsonString());
        
        Object body = response.get("body"); 
        
        String responseFieldName = "rParam";
        String payloadFieldName = "pParam";
        //Scenario 1
        if(body instanceof JSONArray) {
            JSONArray bodyArray = (JSONArray)body;
            //Scenario 3
            addFieldInPayload(payload, bodyArray, responseFieldName, responseFieldName);
            // Scenario 4
            addFieldInPayload(payload, bodyArray, responseFieldName, payloadFieldName);
        }//Scenario 2
        else if(body instanceof JSONObject) { 
            JSONObject bodyObject = (JSONObject)body;
            //Scenario 5
            bodyObject.remove(responseFieldName);
            payload.put("body", bodyObject); //else set in exchange and terminate          
            
            //Scenario 3
            addFieldInPayload(payload, bodyObject, responseFieldName, responseFieldName);
            
            //Scenario 4
            addFieldInPayload(payload, bodyObject, responseFieldName, payloadFieldName);
            
            
            String rMultivalueGrpName = "mvGrpName";
            String pMultivalueGrpName1 = "mvGrpName1";
            
            //Scenario 6
            addFieldFromMultivalueInPayload(payload, responseFieldName, payloadFieldName, bodyObject,rMultivalueGrpName);
            
            //Scenario 7
            addMultiValueFieldInPayload(payload, responseFieldName, responseFieldName, bodyObject, rMultivalueGrpName, pMultivalueGrpName1);
            
            
            // Scenario 8
            addMultiValueFieldInPayload(payload, responseFieldName, payloadFieldName, bodyObject, rMultivalueGrpName,
                    pMultivalueGrpName1);
            
            //Scenario 9
            addFieldInPayloadWithDataType(payload, bodyObject, responseFieldName, payloadFieldName, "amount");
            
            //Set payload in exchange as below
            //exchange.getOut().setBody(payload);
        }
        
    }

    /**
     * @param payload
     * @param responseFieldName
     * @param payloadFieldName
     * @param bodyObject
     * @param multivalueGrpName
     * @param multivalueGrpName1
     */
    private void addMultiValueFieldInPayload(JSONObject payload, String responseFieldName, String payloadFieldName,
            JSONObject bodyObject, String multivalueGrpName, String multivalueGrpName1) {
        JSONArray mvValueArray = new JSONArray();
        JSONObject mvValueObj = new JSONObject();
        mvValueObj.put(payloadFieldName, bodyObject.getJSONArray(multivalueGrpName).getJSONObject(0).getString(responseFieldName));
        mvValueArray.put(mvValueObj);
        payload.put(multivalueGrpName1, mvValueArray);
    }
    
    /**
     * @param payload
     * @param responseFieldName
     * @param payloadFieldName
     * @param bodyObject
     * @param multivalueGrpName
     * @param multivalueGrpName1
     */
    private void addFieldFromMultivalueInPayload(JSONObject payload, String responseFieldName, String payloadFieldName,
            JSONObject bodyObject, String multivalueGrpName) {
        payload.put(payloadFieldName, bodyObject.getJSONArray(multivalueGrpName).getJSONObject(0).getString(responseFieldName));
        
    }

    /**
     * @param payload
     * @param object
     * @param responseFieldName
     * @param payloadFieldName
     */
    private void addFieldInPayload(JSONObject payload, JSONObject object, String responseFieldName,
            String payloadFieldName) {
        String paramValue = object.getString(responseFieldName);
        payload.put(payloadFieldName, paramValue);
    }

    /**
     * @param payload
     * @param bodyArray
     * @param paramName
     */
    private void addFieldInPayload(JSONObject payload, JSONArray bodyArray, String responseFieldName, String payloadFieldName) {
        String paramValue = bodyArray.getJSONObject(0).getString(responseFieldName);
        payload.put(payloadFieldName, paramValue);
    }
    
    /**
     * @param payload
     * @param object
     * @param responseFieldName
     * @param payloadFieldName
     */
    private void addFieldInPayloadWithDataType(JSONObject payload, JSONObject object, String responseFieldName,
            String payloadFieldName,String dataType) {
        String paramValue = object.getString(responseFieldName);
        if(dataType.equalsIgnoreCase("amount")) {
            BigDecimal value = new BigDecimal(paramValue);
            payload.put(payloadFieldName, value);
        }else {
            payload.put(payloadFieldName, paramValue);
        }
    }

}
