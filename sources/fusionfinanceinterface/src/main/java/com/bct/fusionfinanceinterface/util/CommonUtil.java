/**
 * 
 */
package com.bct.fusionfinanceinterface.util;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.bct.fusionfinanceinterface.model.TransactionData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import reactor.core.publisher.Mono;

/**
 * @@author Shunmuga Raja Gurunathan
 *
 */
@Component
public class CommonUtil {
	
	@Value("${fusion.loan.schedule.delay.seconds}")
	private int delayInSec;
	@Value("${fusion.loan.schedule.delayPayOff.seconds}")
	private int delayPayOff;
	
	
	private static final Logger logger = LogManager.getLogger(CommonUtil.class);
	
	
	
	/*
	 * public ResponseEntity<OrchestrationResponse> customResponse( TransactionData
	 * data) { if(data.getOrchestrationResponse().getStatus()!=null &&
	 * data.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS))
	 * return ResponseEntity.ok(data.getOrchestrationResponse()); else return
	 * ResponseEntity.status(data.getHttpStatus()).body(data.
	 * getOrchestrationResponse()); }
	 */
	
	public boolean isSucsess(String t24Response) {
		try {
			return isSucsess(JsonParser.parseString(t24Response).getAsJsonObject());
		} catch (Exception e) {
			logger.debug("isSucsess # Invalid JSON format", e.getMessage());
		}
		return false;

	}

	public boolean isSucsess(JsonObject t24Response) {
		try {
			return t24Response.getAsJsonObject(CommonConstants._HEADER).get(CommonConstants._STATUS).getAsString()
					.equalsIgnoreCase(CommonConstants._SUCCESS);
		} catch (Exception e) {
			logger.debug("isSucsess # Invalid JSON format", e.getMessage());
		}
		return false;
	}
	
	public String getMessageFromResponse(String t24Response) {
		try {
			if (t24Response != null && !t24Response.isEmpty()) {
				JsonObject root = JsonParser.parseString(t24Response).getAsJsonObject();
				if (root.has(CommonConstants._OVERRIDE)) {
					return root.get(CommonConstants._OVERRIDE).toString();
				} else if (root.has(CommonConstants._ERROR)) {
					return root.get(CommonConstants._ERROR).toString();
				}
			}

		} catch (Exception e) {
			logger.debug("getMessageFromResponse # Invalid JSON format", e.getMessage());
		}
		return CommonConstants._EMPTY;
	}
	
	
	public Mono<TransactionData> delayInSec(TransactionData data) {
		if ((data.getOrchestrationResponse().getStatus() != null
				&& data.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS)))
	    return delay(data, delayInSec);
		else return delay(data, 0);
	}
	public Mono<TransactionData> delayPayOff(TransactionData data) {
		if ((data.getOrchestrationResponse().getStatus() != null
				&& data.getOrchestrationResponse().getStatus().equals(CommonConstants._SUCCESS)))
	    return delay(data, delayPayOff);
		else return delay(data, 0);
	}
	
	private Mono<TransactionData> delay(TransactionData data, int seconds) {
	    logger.info("Delaying for {} seconds...", seconds);
	    return Mono.just(data).delayElement(Duration.ofSeconds(seconds));
	}

}
