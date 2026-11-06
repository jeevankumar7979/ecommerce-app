package com.example.inventory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_reservation")
public class InventoryReservation {
    @Id
    @Column(length = 36)
    private String orderId;
    @Column(nullable = false, length = 20)
    private String status;
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InventoryReservationLine> lines = new ArrayList<>();

    protected InventoryReservation() {}

    public InventoryReservation(String orderId, List<InventoryModels.RequestedItem> items) {
        this.orderId = orderId;
        this.status = "RESERVED";
        items.forEach(item -> lines.add(new InventoryReservationLine(this, item.sku(), item.quantity())));
    }

    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public List<InventoryReservationLine> getLines() { return lines; }
    public void setStatus(String status) { this.status = status; }
}
