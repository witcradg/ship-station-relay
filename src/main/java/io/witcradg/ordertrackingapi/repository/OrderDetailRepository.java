package io.witcradg.ordertrackingapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.witcradg.ordertrackingapi.entity.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long>{

}

