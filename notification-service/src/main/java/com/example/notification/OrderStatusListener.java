package com.example.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusListener {
    private static final Logger log = LoggerFactory.getLogger(OrderStatusListener.class);

    @KafkaListener(topics = "order-status", groupId = "notification-service")
    public void onOrderStatusChanged(NotificationModels.OrderStatusChanged event) {
        log.info("Notification requested for order {} with status {}", event.orderId(), event.status());
    }
}
