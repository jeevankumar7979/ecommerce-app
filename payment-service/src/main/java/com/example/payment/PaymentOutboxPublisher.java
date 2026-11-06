package com.example.payment;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxPublisher {
    private final PaymentOutboxRepository outbox;
    private final KafkaTemplate<String, Object> kafka;

    public PaymentOutboxPublisher(PaymentOutboxRepository outbox, KafkaTemplate<String, Object> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.delay-ms:1000}")
    public void publishPending() {
        for (PaymentOutboxEvent event : outbox.findTop100ByPublishedFalseOrderByCreatedAtAsc()) {
            try {
                kafka.send(event.getTopic(), event.getOrderId(),
                        new PaymentModels.PaymentResult(event.getOrderId(), event.getStatus(), event.getFailureReason()))
                        .get();
                event.markPublished();
                outbox.save(event);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not publish payment result for order " + event.getOrderId(),
                        exception);
            }
        }
    }
}
