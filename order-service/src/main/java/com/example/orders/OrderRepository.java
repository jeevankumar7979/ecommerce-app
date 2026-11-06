package com.example.orders;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<OrderDocument, String> {
    List<OrderDocument> findTop100ByStatusOrderByCreatedAtAsc(String status);
    List<OrderDocument> findTop100ByStatusAndPaymentEventPublishedFalseOrderByCreatedAtAsc(String status);
    List<OrderDocument> findTop100ByStatusInAndStatusEventPublishedFalseOrderByCreatedAtAsc(List<String> statuses);
}
