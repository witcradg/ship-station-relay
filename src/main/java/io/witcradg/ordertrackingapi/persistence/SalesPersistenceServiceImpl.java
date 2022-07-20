package io.witcradg.ordertrackingapi.persistence;

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
	public void write(String message) {
		log.info(String.format("SalesPersistenceServiceImpl::write\n %s", message));
	}

	@Override
	public void write(String sku, String productName) {
		log.info(String.format("SalesPersistenceServiceImpl::write\n %s | %s", sku, productName));

		Sales sale = new Sales(sku, productName); 

		salesRepository.save(sale);
	}
}
