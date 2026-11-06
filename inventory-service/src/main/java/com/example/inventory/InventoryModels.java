package com.example.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class InventoryModels {
    private InventoryModels() {}

    public record ItemRequest(@NotBlank String sku, @Min(0) int availableQuantity) {}
    public record ItemResponse(String sku, int availableQuantity, int reservedQuantity) {}
    public record RequestedItem(@NotBlank String sku, @Positive int quantity) {}
    public record ReservationRequest(@NotBlank String orderId, @NotEmpty List<@Valid RequestedItem> items) {}
    public record ReservationResponse(String orderId, String status) {}
}
