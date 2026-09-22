package com.bct.fusionfinanceinterface.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for setting up a custom, resilient WebClient instance.
 * All key connection, pool, and timeout values are now configurable via properties.
 * These defaults are tuned for APIs that include long-running orchestration/workflow logic.
 */
@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    // --- Endpoint Configuration ---
    @Value("${fusion.base.url}")
    private String baseURL;

    // --- Connection Pool Configuration Values (Recommended Defaults) ---
    // Max connections in the pool
    @Value("${webclient.pool.max-connections:200}") 
    private int maxConnections;

    // Time a connection can remain idle before being closed
    @Value("${webclient.pool.idle-timeout-seconds:20}") 
    private long idleTimeoutSeconds;

    // Maximum lifespan of a connection before being recycled
    @Value("${webclient.pool.life-time-seconds:600}") 
    private long lifeTimeSeconds;

    // --- Timeout Configuration Values (Adjusted for Orchestration) ---
    // Time to establish the initial connection
    @Value("${webclient.connect-timeout-millis:5000}") // 50 seconds
    private int connectTimeoutMillis;

    // Time to wait for the next chunk of data on an established connection
    // Increased to 120s to allow time for long internal workflow steps.
    @Value("${webclient.read-timeout-seconds:120}") 
    private long readTimeoutSeconds;

    // Total time allowed for the entire request/response exchange
    // Increased to 300s to cover the full duration of a multi-step workflow.
    @Value("${webclient.response-timeout-seconds:300}") // 5 minutes (300 seconds)
    private long responseTimeoutSeconds;
    // ------------------------------------------

    /**
     * Configures and returns a custom WebClient bean.
     * The client uses Reactor Netty to set specific connection timeouts, read timeouts,
     * and includes an exponential backoff retry filter, with configurable connection pooling.
     *
     * @return A custom configured WebClient instance.
     */
    @Bean
    public WebClient webClient() {

        logger.debug("baseURL   ***************************>>     "+baseURL);
        
        // Define a connection pool provider for connection reuse and management
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom-pool")
            .maxConnections(maxConnections) 
            .maxIdleTime(Duration.ofSeconds(idleTimeoutSeconds)) 
            .maxLifeTime(Duration.ofSeconds(lifeTimeSeconds))
            .build();


        // 1. Configure the underlying HTTP client (Reactor Netty) using the connection pool
        HttpClient httpClient = HttpClient.create(connectionProvider)
            // Configurable Connect Timeout
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
            // Configurable Response Timeout
            .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds))
            
            // Configurable Read Timeout Handler
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS))
            );

        // 2. Build the WebClient
        return WebClient.builder()
                // Apply the custom HTTP client connector
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // Set the base URL for the client
                 .baseUrl(baseURL)
                 // Apply the custom retry logic filter
                 .filter(retryFilter())
                .build();
    }

    /**
     * Defines the retry logic using Reactor's Retry utility.
     * It implements an exponential backoff strategy for transient network failures.
     *
     * @return An ExchangeFilterFunction to handle retries.
     */
    private ExchangeFilterFunction retryFilter() {
        return (request, next) ->
            next.exchange(request)
                .retryWhen(
                    // Exponential backoff: 3 retries, starting delay of 2 seconds
                    Retry.backoff(3, Duration.ofSeconds(2))
                        // Only retry if the exception is an IOException (indicating a network/connection failure)
                        .filter(ex -> ex instanceof IOException)
                        // If all retries are exhausted, throw the original failure
                        .onRetryExhaustedThrow((retrySpec, signal) -> signal.failure())
                );
    }
}