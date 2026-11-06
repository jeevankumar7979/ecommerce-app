package com.example.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from InventoryReservation reservation where reservation.orderId = :orderId")
    java.util.Optional<InventoryReservation> findByIdForUpdate(@Param("orderId") String orderId);
}
