package com.example.orders;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderRepository orders;
    private final InventoryClient inventory;
    private final OrderEventPublisher events;

    public OrderController(OrderRepository orders, InventoryClient inventory, OrderEventPublisher events) {
        this.orders = orders;
        this.inventory = inventory;
        this.events = events;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderModels.OrderResponse create(@Valid @RequestBody OrderModels.CreateOrderRequest request) {
        String id = UUID.randomUUID().toString();
        OrderDocument order = orders.save(new OrderDocument(id, request));
        try {
            inventory.reserve(new OrderModels.InventoryReservationRequest(id, request.items()));
            order.markInventoryReserved();
            orders.save(order);
        } catch (HttpClientErrorException exception) {
            order.complete("INVENTORY_FAILED", "Inventory reservation rejected: " + exception.getStatusCode());
            orders.save(order);
        } catch (RuntimeException exception) {
            log.warn("Inventory reservation for order {} is pending retry: {}", id, exception.toString());
        }
        events.publishPendingRequests();
        return toResponse(order);
    }

    @GetMapping("/{id}")
    public OrderModels.OrderResponse get(@PathVariable String id) {
        return orders.findById(id).map(OrderController::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private static OrderModels.OrderResponse toResponse(OrderDocument order) {
        return new OrderModels.OrderResponse(order.getId(), order.getCustomerId(), order.getItems(),
                order.getAmount(), order.getStatus(), order.getFailureReason(), order.getCreatedAt());
    }
}
