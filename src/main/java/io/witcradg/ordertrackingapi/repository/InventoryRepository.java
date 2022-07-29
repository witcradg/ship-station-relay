package io.witcradg.ordertrackingapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.witcradg.ordertrackingapi.entity.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>{

	Inventory findInventoryBySku(String sku);

}
