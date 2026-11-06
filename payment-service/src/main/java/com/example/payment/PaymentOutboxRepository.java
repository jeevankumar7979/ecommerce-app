package com.example.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxEvent, String> {
    List<PaymentOutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
