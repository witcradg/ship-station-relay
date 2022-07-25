package io.witcradg.ordertrackingapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.witcradg.ordertrackingapi.entity.OrderItems;

@Repository
public interface OrderItemsRepository extends JpaRepository<OrderItems, Long>{
	public OrderItems findOrderItemsByOrderNumber(String orderNumber);
}
