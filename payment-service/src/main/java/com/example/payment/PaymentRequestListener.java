package com.example.payment;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentRequestListener {
    private final PaymentRecordRepository payments;
    private final PaymentOutboxRepository outbox;

    public PaymentRequestListener(PaymentRecordRepository payments, PaymentOutboxRepository outbox) {
        this.payments = payments;
        this.outbox = outbox;
    }

    @KafkaListener(topics = "payment-requests", groupId = "payment-service")
    @Transactional
    public void onPaymentRequested(PaymentModels.PaymentRequested request) {
        if (payments.existsById(request.orderId())) {
            return;
        }
        boolean success = !"DECLINE".equalsIgnoreCase(request.paymentMethod())
                && !"FAIL".equalsIgnoreCase(request.paymentMethod());
        PaymentRecord payment = payments.save(new PaymentRecord(request, success));
        outbox.save(new PaymentOutboxEvent("payment-results", payment.getOrderId(),
                payment.getStatus(), payment.getFailureReason()));
    }
}
