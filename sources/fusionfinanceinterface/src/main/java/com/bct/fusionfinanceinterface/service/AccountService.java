package com.bct.fusionfinanceinterface.service;
import reactor.core.publisher.Mono;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.bct.fusionfinanceinterface.model.TransactionData;

import java.time.Duration;
public class AccountService {
    private final WebClient webClient;
    private static final Logger logger = LogManager.getLogger(AccountService.class);

    public AccountService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<TransactionData> createAccount(TransactionData data) {
        String payload = buildPayload(data.getOrchestrationResponse().getCustomerId());
        return webClient.post()
                .uri("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                //.bodyToMono(AccountResponse.class)
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .flatMap(response -> handleAccountResponse(response, data))
                .doOnError(WebClientResponseException.class, e -> logger.error("Account API failed: {}", e.getMessage()));
    }

    private String buildPayload(String customerId) {
        return "{\"customerId\":\"" + customerId + "\",\"accountType\":\"SAVINGS\"}";
    }

    private Mono<TransactionData> handleAccountResponse(String response, TransactionData data) {
        if (!"ACCOUNT_CREATED".equalsIgnoreCase(response)) {
            return Mono.error(new RuntimeException("Account creation failed: " + response));
        }
        logger.info("✅ Account creation successful for customer: {}", data.getOrchestrationResponse().getCustomerId());
        logger.debug("AccountResponse: {}", response);
        return Mono.just(data);
    }
}
