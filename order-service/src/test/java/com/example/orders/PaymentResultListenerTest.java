package com.example.orders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PaymentResultListenerTest {
    private OrderRepository orders;
    private InventoryClient inventory;
    private PaymentResultListener listener;
    private OrderDocument order;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        inventory = mock(InventoryClient.class);
        listener = new PaymentResultListener(orders, inventory);
        order = new OrderDocument("order-1", new OrderModels.CreateOrderRequest(
                "customer-1", List.of(new OrderModels.OrderItem("SKU-1", 1)),
                new BigDecimal("10.00"), "DEMO"));
        order.markInventoryReserved();
        when(orders.findById("order-1")).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void successfulPaymentConfirmsInventory() {
        listener.onPaymentResult(new OrderModels.PaymentResult("order-1", "SUCCEEDED", null));

        assertEquals("CONFIRMED", order.getStatus());
        verify(inventory).confirm("order-1");
        verify(inventory, never()).release(any());
        verify(orders).save(order);
    }

    @Test
    void failedPaymentReleasesInventoryAndIsIdempotent() {
        var failed = new OrderModels.PaymentResult("order-1", "FAILED", "declined");
        listener.onPaymentResult(failed);
        listener.onPaymentResult(failed);

        assertEquals("PAYMENT_FAILED", order.getStatus());
        verify(inventory).release("order-1");
        verify(inventory, never()).confirm(any());
        verify(orders, times(1)).save(order);
    }
}
