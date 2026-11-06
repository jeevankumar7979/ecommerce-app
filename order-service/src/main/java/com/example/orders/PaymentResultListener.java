package com.example.orders;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {
    private final OrderRepository orders;
    private final InventoryClient inventory;

    public PaymentResultListener(OrderRepository orders, InventoryClient inventory) {
        this.orders = orders;
        this.inventory = inventory;
    }

    @KafkaListener(topics = "payment-results", groupId = "order-service")
    public void onPaymentResult(OrderModels.PaymentResult result) {
        OrderDocument order = orders.findById(result.orderId())
                .orElseThrow(() -> new IllegalStateException("Payment result references unknown order " + result.orderId()));
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            return;
        }
        switch (result.status()) {
            case "SUCCEEDED" -> {
                inventory.confirm(order.getId());
                order.complete("CONFIRMED", null);
            }
            case "FAILED" -> {
                inventory.release(order.getId());
                order.complete("PAYMENT_FAILED", result.failureReason());
            }
            default -> throw new IllegalArgumentException("Unknown payment result status: " + result.status());
        }
        orders.save(order);
    }
}
