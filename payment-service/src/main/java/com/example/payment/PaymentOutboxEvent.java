package com.example.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox")
public class PaymentOutboxEvent {
    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 80)
    private String topic;
    @Column(nullable = false, length = 36)
    private String orderId;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(length = 256)
    private String failureReason;
    @Column(nullable = false)
    private boolean published;
    @Column(nullable = false)
    private Instant createdAt;

    protected PaymentOutboxEvent() {}

    public PaymentOutboxEvent(String topic, String orderId, String status, String failureReason) {
        this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.orderId = orderId;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getTopic() { return topic; }
    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public boolean isPublished() { return published; }
    public void markPublished() { this.published = true; }
}
