package io.witcradg.ordertrackingapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import io.witcradg.ordertrackingapi.entity.Sales;

@Repository
public interface SalesRepository extends JpaRepository<Sales, Long>{

}
