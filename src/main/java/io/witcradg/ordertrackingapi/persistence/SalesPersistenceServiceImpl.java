package io.witcradg.ordertrackingapi.persistence;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.witcradg.ordertrackingapi.entity.Sales;
import io.witcradg.ordertrackingapi.repository.SalesRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class SalesPersistenceServiceImpl implements ISalesPersistenceService {

	@Autowired
	private SalesRepository salesRepository;

	@Override
	public void write(
			String orderNumber, 
			String sku, 
			String productName, 
			Integer quantitySold, 
			BigDecimal unitPrice, 
			BigDecimal totalPrice) {
		log.info(String.format("SalesPersistenceServiceImpl::write\n %s | %s | %s | %n",
				orderNumber, sku, productName, quantitySold, unitPrice, totalPrice));

		Sales sale = new Sales(orderNumber, sku, productName, quantitySold, unitPrice, totalPrice); 

		salesRepository.save(sale);
	}
}
