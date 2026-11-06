package com.example.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderModels {
    private OrderModels() {}

    public record OrderItem(@NotBlank String sku, @Positive int quantity) {}

    public record CreateOrderRequest(
            @NotBlank String customerId,
            @NotEmpty List<@Valid OrderItem> items,
            @Positive BigDecimal amount,
            @NotBlank String paymentMethod) {}

    public record OrderResponse(
            String id,
            String customerId,
            List<OrderItem> items,
            BigDecimal amount,
            String status,
            String failureReason,
            Instant createdAt) {}

    public record InventoryReservationRequest(String orderId, List<OrderItem> items) {}

    public record PaymentRequested(String orderId, String customerId, BigDecimal amount, String paymentMethod) {}

    public record PaymentResult(String orderId, String status, String failureReason) {}

    public record OrderStatusChanged(String orderId, String customerId, String status) {}
}
