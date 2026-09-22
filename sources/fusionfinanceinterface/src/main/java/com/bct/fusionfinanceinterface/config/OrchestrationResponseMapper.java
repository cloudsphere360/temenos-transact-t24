package com.bct.fusionfinanceinterface.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.bct.fusionfinanceinterface.model.OrchestrationResponse;
import com.bct.fusionfinanceinterface.util.CommonConstants;

public final class OrchestrationResponseMapper {

    private OrchestrationResponseMapper() {
        // Utility class – prevent instantiation
    }

    /**
     * Build HTTP response using:
     * 1. Downstream HTTP status (WebClient / API)
     * 2. Orchestration business status
     */
    public static ResponseEntity<OrchestrationResponse> buildHttpResponse(
            HttpStatus httpStatus,
            OrchestrationResponse response) {

        // 1️⃣ System failure (null response)
        if (response == null) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("SYSTEM_ERROR",
                            "Empty orchestration response"));
        }

        // 2️⃣ Business success → always 200 OK
        if (isSuccess(response)) {
            return ResponseEntity.ok(response);
        }

        // 3️⃣ Client-side errors (4xx from downstream)
        if (httpStatus != null && httpStatus.is4xxClientError()) {

            // Validation / input issues
            if (isValidationError(response)) {
                return ResponseEntity
                        .badRequest() // 400
                        .body(response);
            }

            // Other client-side business failures
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY) // 422
                    .body(response);
        }

        // 4️⃣ Server-side errors (5xx from downstream)
        if (httpStatus != null && httpStatus.is5xxServerError()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY) // 502
                    .body(response);
        }

        // 5️⃣ Fallback (unexpected cases)
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private static boolean isSuccess(OrchestrationResponse response) {
        return CommonConstants._SUCCESS.equals(response.getStatus());
    }

    private static boolean isValidationError(OrchestrationResponse response) {
    	 return CommonConstants._FAILED.equals(response.getStatus());
    }

    private static OrchestrationResponse buildErrorResponse(
            String code,
            String message) {

        OrchestrationResponse resp = new OrchestrationResponse();
        resp.setStatus(CommonConstants._FAILED);
        resp.setMessage(code + ": " + message);
        return resp;
    }
}

