package com.example.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
public class InventoryApplicationService {
    private final InventoryItemRepository items;
    private final InventoryReservationRepository reservations;

    public InventoryApplicationService(InventoryItemRepository items, InventoryReservationRepository reservations) {
        this.items = items;
        this.reservations = reservations;
    }

    @Transactional
    public InventoryModels.ItemResponse putItem(InventoryModels.ItemRequest request) {
        InventoryItem item = items.findById(request.sku())
                .map(existing -> {
                    if (existing.getReservedQuantity() > request.availableQuantity()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Available quantity is below reserved stock");
                    }
                    return existing;
                })
                .orElseGet(() -> new InventoryItem(request.sku(), request.availableQuantity()));
        items.save(item);
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<InventoryModels.ItemResponse> getItems() {
        return items.findAll().stream().map(InventoryApplicationService::toResponse).toList();
    }

    @Transactional
    public InventoryModels.ReservationResponse reserve(InventoryModels.ReservationRequest request) {
        var existing = reservations.findById(request.orderId());
        if (existing.isPresent()) {
            InventoryReservation reservation = existing.get();
            if (!"RESERVED".equals(reservation.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation is already " + reservation.getStatus());
            }
            return response(reservation);
        }

        Map<String, Integer> requested = aggregate(request.items());
        List<InventoryItem> locked = items.findAllForUpdate(new TreeSet<>(requested.keySet()));
        if (locked.size() != requested.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more SKUs do not exist");
        }
        Map<String, InventoryItem> bySku = new HashMap<>();
        locked.forEach(item -> bySku.put(item.getSku(), item));
        for (var entry : requested.entrySet()) {
            InventoryItem item = bySku.get(entry.getKey());
            if (item.getAvailableQuantity() - item.getReservedQuantity() < entry.getValue()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient inventory for " + entry.getKey());
            }
        }
        requested.forEach((sku, quantity) -> bySku.get(sku).reserve(quantity));
        reservations.save(new InventoryReservation(request.orderId(), requested.entrySet().stream()
                .map(entry -> new InventoryModels.RequestedItem(entry.getKey(), entry.getValue())).toList()));
        return new InventoryModels.ReservationResponse(request.orderId(), "RESERVED");
    }

    @Transactional
    public InventoryModels.ReservationResponse confirm(String orderId) {
        InventoryReservation reservation = findReservationForUpdate(orderId);
        if ("CONFIRMED".equals(reservation.getStatus())) {
            return response(reservation);
        }
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation is already " + reservation.getStatus());
        }
        Map<String, Integer> lines = lineQuantities(reservation);
        Map<String, InventoryItem> locked = lockItems(lines);
        lines.forEach((sku, quantity) -> locked.get(sku).commit(quantity));
        reservation.setStatus("CONFIRMED");
        return response(reservation);
    }

    @Transactional
    public InventoryModels.ReservationResponse release(String orderId) {
        InventoryReservation reservation = findReservationForUpdate(orderId);
        if ("RELEASED".equals(reservation.getStatus())) {
            return response(reservation);
        }
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot release reservation in " + reservation.getStatus());
        }
        Map<String, Integer> lines = lineQuantities(reservation);
        Map<String, InventoryItem> locked = lockItems(lines);
        lines.forEach((sku, quantity) -> locked.get(sku).release(quantity));
        reservation.setStatus("RELEASED");
        return response(reservation);
    }

    private InventoryReservation findReservation(String orderId) {
        return reservations.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    private InventoryReservation findReservationForUpdate(String orderId) {
        return reservations.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    private Map<String, InventoryItem> lockItems(Map<String, Integer> lines) {
        List<InventoryItem> locked = items.findAllForUpdate(new TreeSet<>(lines.keySet()));
        if (locked.size() != lines.size()) {
            throw new IllegalStateException("Inventory item missing for existing reservation");
        }
        Map<String, InventoryItem> bySku = new HashMap<>();
        locked.forEach(item -> bySku.put(item.getSku(), item));
        return bySku;
    }

    private static Map<String, Integer> aggregate(List<InventoryModels.RequestedItem> requestedItems) {
        Map<String, Integer> quantities = new HashMap<>();
        requestedItems.forEach(item -> quantities.merge(item.sku(), item.quantity(), Math::addExact));
        return quantities;
    }

    private static Map<String, Integer> lineQuantities(InventoryReservation reservation) {
        Map<String, Integer> quantities = new HashMap<>();
        reservation.getLines().forEach(line -> quantities.merge(line.getSku(), line.getQuantity(), Math::addExact));
        return quantities;
    }

    private static InventoryModels.ItemResponse toResponse(InventoryItem item) {
        return new InventoryModels.ItemResponse(item.getSku(), item.getAvailableQuantity(), item.getReservedQuantity());
    }

    private static InventoryModels.ReservationResponse response(InventoryReservation reservation) {
        return new InventoryModels.ReservationResponse(reservation.getOrderId(), reservation.getStatus());
    }
}
