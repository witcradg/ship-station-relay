package io.witcradg.ordertrackingapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.witcradg.ordertrackingapi.entity.OrderHistory;

@Repository
public interface OrderRepository extends JpaRepository<OrderHistory, Long>{

}
