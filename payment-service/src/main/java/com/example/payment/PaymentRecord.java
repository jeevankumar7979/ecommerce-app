package com.example.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_record")
public class PaymentRecord {
    @Id
    @Column(length = 36)
    private String orderId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(length = 256)
    private String failureReason;
    @Column(nullable = false)
    private Instant createdAt;

    protected PaymentRecord() {}

    public PaymentRecord(PaymentModels.PaymentRequested request, boolean success) {
        this.orderId = request.orderId();
        this.amount = request.amount();
        this.status = success ? "SUCCEEDED" : "FAILED";
        this.failureReason = success ? null : "Payment declined by the local payment simulator";
        this.createdAt = Instant.now();
    }

    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
}
