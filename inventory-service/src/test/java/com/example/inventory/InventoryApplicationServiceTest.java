package com.example.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class InventoryApplicationServiceTest {
    private InventoryItemRepository items;
    private InventoryReservationRepository reservations;
    private InventoryApplicationService service;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        items = mock(InventoryItemRepository.class);
        reservations = mock(InventoryReservationRepository.class);
        service = new InventoryApplicationService(items, reservations);
        item = new InventoryItem("BOOK-1", 5);
        when(items.findAllForUpdate(anyCollection())).thenReturn(List.of(item));
        when(reservations.findById(any())).thenReturn(Optional.empty());
        when(reservations.findByIdForUpdate(any())).thenReturn(Optional.empty());
        when(reservations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reservationAndConfirmationUpdateStockAtomically() {
        var request = new InventoryModels.ReservationRequest("order-1",
                List.of(new InventoryModels.RequestedItem("BOOK-1", 2)));

        assertEquals("RESERVED", service.reserve(request).status());
        assertEquals(2, item.getReservedQuantity());
        when(reservations.findByIdForUpdate("order-1")).thenReturn(Optional.of(
                new InventoryReservation("order-1", request.items())));
        assertEquals("CONFIRMED", service.confirm("order-1").status());
        assertEquals(3, item.getAvailableQuantity());
        assertEquals(0, item.getReservedQuantity());
    }

    @Test
    void reservationRejectsOverselling() {
        var request = new InventoryModels.ReservationRequest("order-2",
                List.of(new InventoryModels.RequestedItem("BOOK-1", 6)));

        assertThrows(ResponseStatusException.class, () -> service.reserve(request));
        assertEquals(0, item.getReservedQuantity());
        verify(reservations, never()).save(any());
    }

    @Test
    void releaseReturnsReservedStock() {
        var request = new InventoryModels.ReservationRequest("order-3",
                List.of(new InventoryModels.RequestedItem("BOOK-1", 3)));
        service.reserve(request);
        when(reservations.findByIdForUpdate("order-3")).thenReturn(Optional.of(
                new InventoryReservation("order-3", request.items())));

        assertEquals("RELEASED", service.release("order-3").status());
        assertEquals(0, item.getReservedQuantity());
        assertEquals(5, item.getAvailableQuantity());
    }
}
