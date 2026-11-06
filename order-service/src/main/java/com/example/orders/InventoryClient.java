package com.example.orders;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;
import java.time.Duration;
import java.net.http.HttpClient;

@Component
public class InventoryClient {
    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    private final Retry retry;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public InventoryClient(
            RestClient.Builder builder,
            @Value("${inventory.base-url:http://localhost:8082}") String baseUrl,
            @Value("${inventory.connect-timeout:2s}") Duration connectTimeout,
            @Value("${inventory.read-timeout:3s}") Duration readTimeout,
            CircuitBreakerRegistry circuitBreakers,
            RateLimiterRegistry rateLimiters,
            RetryRegistry retries,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    var authorizedClient = this.authorizedClientManager.authorize(
                            OAuth2AuthorizeRequest.withClientRegistrationId("inventory")
                                    .principal("order-service").build());
                    if (authorizedClient == null) {
                        throw new IllegalStateException("Could not obtain an inventory service access token");
                    }
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION,
                            "Bearer " + authorizedClient.getAccessToken().getTokenValue());
                    return execution.execute(request, body);
                }).build();
        this.circuitBreaker = circuitBreakers.circuitBreaker("inventory");
        this.rateLimiter = rateLimiters.rateLimiter("inventory");
        this.retry = retries.retry("inventory");
    }

    public void reserve(OrderModels.InventoryReservationRequest request) {
        execute(() -> restClient.post().uri("/api/inventory/reservations")
                .body(request).retrieve().toBodilessEntity());
    }

    public void confirm(String orderId) {
        execute(() -> restClient.post().uri("/api/inventory/reservations/{orderId}/confirm", orderId)
                .retrieve().toBodilessEntity());
    }

    public void release(String orderId) {
        execute(() -> restClient.post().uri("/api/inventory/reservations/{orderId}/release", orderId)
                .retrieve().toBodilessEntity());
    }

    private void execute(Supplier<?> operation) {
        Supplier<?> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, operation);
        decorated = RateLimiter.decorateSupplier(rateLimiter, decorated);
        decorated = Retry.decorateSupplier(retry, decorated);
        decorated.get();
    }
}
