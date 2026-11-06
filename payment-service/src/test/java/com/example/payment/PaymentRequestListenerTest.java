package com.example.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentRequestListenerTest {
    private PaymentRecordRepository payments;
    private PaymentOutboxRepository outbox;
    private PaymentRequestListener listener;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRecordRepository.class);
        outbox = mock(PaymentOutboxRepository.class);
        listener = new PaymentRequestListener(payments, outbox);
        when(payments.existsById("order-1")).thenReturn(false);
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void successfulPaymentIsRecordedWithResultInOutbox() {
        listener.onPaymentRequested(new PaymentModels.PaymentRequested(
                "order-1", "customer-1", new BigDecimal("24.50"), "DEMO"));

        var payment = org.mockito.ArgumentCaptor.forClass(PaymentRecord.class);
        verify(payments).save(payment.capture());
        assertEquals("SUCCEEDED", payment.getValue().getStatus());
        var event = org.mockito.ArgumentCaptor.forClass(PaymentOutboxEvent.class);
        verify(outbox).save(event.capture());
        assertEquals("payment-results", event.getValue().getTopic());
        assertEquals("SUCCEEDED", event.getValue().getStatus());
    }

    @Test
    void declinedPaymentCreatesFailureOutcome() {
        listener.onPaymentRequested(new PaymentModels.PaymentRequested(
                "order-1", "customer-1", new BigDecimal("24.50"), "DECLINE"));

        var payment = org.mockito.ArgumentCaptor.forClass(PaymentRecord.class);
        verify(payments).save(payment.capture());
        assertEquals("FAILED", payment.getValue().getStatus());
        verify(outbox).save(any(PaymentOutboxEvent.class));
    }

    @Test
    void duplicateOrderDoesNotCreateAnotherPaymentOrEvent() {
        when(payments.existsById("order-1")).thenReturn(true);

        listener.onPaymentRequested(new PaymentModels.PaymentRequested(
                "order-1", "customer-1", new BigDecimal("24.50"), "DEMO"));

        verify(payments, never()).save(any());
        verify(outbox, never()).save(any());
    }
}
