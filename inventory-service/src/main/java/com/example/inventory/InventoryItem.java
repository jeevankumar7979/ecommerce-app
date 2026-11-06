package com.example.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {
    @Id
    @Column(length = 128)
    private String sku;
    @Column(nullable = false)
    private int availableQuantity;
    @Column(nullable = false)
    private int reservedQuantity;
    @Version
    private long version;

    protected InventoryItem() {}

    public InventoryItem(String sku, int availableQuantity) {
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    public String getSku() { return sku; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public void reserve(int quantity) { reservedQuantity += quantity; }
    public void release(int quantity) { reservedQuantity -= quantity; }
    public void commit(int quantity) {
        reservedQuantity -= quantity;
        availableQuantity -= quantity;
    }
}
