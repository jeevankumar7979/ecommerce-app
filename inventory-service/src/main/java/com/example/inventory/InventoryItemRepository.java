package com.example.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItem item where item.sku in :skus order by item.sku")
    List<InventoryItem> findAllForUpdate(@Param("skus") Collection<String> skus);
}
