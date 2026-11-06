package com.example.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Component
public class OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final OrderRepository orders;
    private final KafkaTemplate<String, Object> kafka;
    private final InventoryClient inventory;

    public OrderEventPublisher(OrderRepository orders, KafkaTemplate<String, Object> kafka, InventoryClient inventory) {
        this.orders = orders;
        this.kafka = kafka;
        this.inventory = inventory;
    }

    @Scheduled(fixedDelayString = "${orders.publisher.delay-ms:1000}")
    public void publishPendingRequests() {
        reservePendingOrders();
        for (OrderDocument order : orders
                .findTop100ByStatusAndPaymentEventPublishedFalseOrderByCreatedAtAsc("PENDING_PAYMENT")) {
            try {
                kafka.send("payment-requests", order.getId(),
                        new OrderModels.PaymentRequested(order.getId(), order.getCustomerId(),
                                order.getAmount(), order.getPaymentMethod())).get();
                order.markPaymentEventPublished();
                orders.save(order);
            } catch (Exception exception) {
                log.error("Could not publish payment request for order {}; it will be retried", order.getId(), exception);
            }
        }
        publishPendingStatusEvents();
    }

    private void publishPendingStatusEvents() {
        for (OrderDocument order : orders.findTop100ByStatusInAndStatusEventPublishedFalseOrderByCreatedAtAsc(
                List.of("CONFIRMED", "PAYMENT_FAILED", "INVENTORY_FAILED"))) {
            try {
                kafka.send("order-status", order.getId(),
                        new OrderModels.OrderStatusChanged(order.getId(), order.getCustomerId(), order.getStatus())).get();
                order.markStatusEventPublished();
                orders.save(order);
            } catch (Exception exception) {
                log.error("Could not publish status event for order {}; it will be retried", order.getId(), exception);
            }
        }
    }

    private void reservePendingOrders() {
        for (OrderDocument order : orders.findTop100ByStatusOrderByCreatedAtAsc("RESERVING_INVENTORY")) {
            try {
                inventory.reserve(new OrderModels.InventoryReservationRequest(order.getId(), order.getItems()));
                order.markInventoryReserved();
                orders.save(order);
            } catch (HttpClientErrorException exception) {
                order.complete("INVENTORY_FAILED", "Inventory reservation rejected: " + exception.getStatusCode());
                orders.save(order);
            } catch (RuntimeException exception) {
                log.warn("Inventory reservation for order {} is still pending: {}", order.getId(), exception.toString());
            }
        }
    }
}
