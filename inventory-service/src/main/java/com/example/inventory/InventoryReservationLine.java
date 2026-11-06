package com.example.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_reservation_line")
public class InventoryReservationLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private InventoryReservation reservation;
    @Column(nullable = false, length = 128)
    private String sku;
    @Column(nullable = false)
    private int quantity;

    protected InventoryReservationLine() {}

    InventoryReservationLine(InventoryReservation reservation, String sku, int quantity) {
        this.reservation = reservation;
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
}
