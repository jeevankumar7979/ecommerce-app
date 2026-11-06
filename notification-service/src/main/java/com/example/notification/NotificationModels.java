package com.example.notification;

public final class NotificationModels {
    private NotificationModels() {}

    public record OrderStatusChanged(String orderId, String customerId, String status) {}
}
