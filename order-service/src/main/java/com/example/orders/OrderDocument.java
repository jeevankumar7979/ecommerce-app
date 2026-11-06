package com.example.orders;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document("orders")
public class OrderDocument {
    @Id
    private String id;
    private String customerId;
    private List<OrderModels.OrderItem> items;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String failureReason;
    private boolean paymentEventPublished;
    private boolean statusEventPublished;
    private Instant createdAt;

    protected OrderDocument() {}

    public OrderDocument(String id, OrderModels.CreateOrderRequest request) {
        this.id = id;
        this.customerId = request.customerId();
        this.items = List.copyOf(request.items());
        this.amount = request.amount();
        this.paymentMethod = request.paymentMethod();
        this.status = "RESERVING_INVENTORY";
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public List<OrderModels.OrderItem> getItems() { return items; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public boolean isPaymentEventPublished() { return paymentEventPublished; }
    public boolean isStatusEventPublished() { return statusEventPublished; }
    public Instant getCreatedAt() { return createdAt; }
    public void markPaymentEventPublished() { this.paymentEventPublished = true; }
    public void markStatusEventPublished() { this.statusEventPublished = true; }
    public void markInventoryReserved() { this.status = "PENDING_PAYMENT"; }
    public void complete(String status, String failureReason) {
        this.status = status;
        this.failureReason = failureReason;
    }
}
