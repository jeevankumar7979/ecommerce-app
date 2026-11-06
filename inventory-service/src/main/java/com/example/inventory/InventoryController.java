package com.example.inventory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryApplicationService service;

    public InventoryController(InventoryApplicationService service) {
        this.service = service;
    }

    @PutMapping("/items/{sku}")
    public InventoryModels.ItemResponse putItem(@PathVariable String sku,
                                                 @Valid @RequestBody InventoryModels.ItemRequest request) {
        if (!sku.equals(request.sku())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Path SKU must match body SKU");
        }
        return service.putItem(request);
    }

    @GetMapping("/items")
    public List<InventoryModels.ItemResponse> getItems() {
        return service.getItems();
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryModels.ReservationResponse reserve(@Valid @RequestBody InventoryModels.ReservationRequest request) {
        return service.reserve(request);
    }

    @PostMapping("/reservations/{orderId}/confirm")
    public InventoryModels.ReservationResponse confirm(@PathVariable String orderId) {
        return service.confirm(orderId);
    }

    @PostMapping("/reservations/{orderId}/release")
    public InventoryModels.ReservationResponse release(@PathVariable String orderId) {
        return service.release(orderId);
    }
}
