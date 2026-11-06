package com.example.payment;

import java.math.BigDecimal;

public final class PaymentModels {
    private PaymentModels() {}

    public record PaymentRequested(String orderId, String customerId, BigDecimal amount, String paymentMethod) {}
    public record PaymentResult(String orderId, String status, String failureReason) {}
}
