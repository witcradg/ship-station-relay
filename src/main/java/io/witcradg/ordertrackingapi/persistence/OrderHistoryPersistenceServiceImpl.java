package io.witcradg.ordertrackingapi.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.witcradg.ordertrackingapi.entity.OrderHistory;
import io.witcradg.ordertrackingapi.repository.OrderHistoryRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class OrderHistoryPersistenceServiceImpl implements IOrderHistoryPersistenceService {

	@Autowired
	private OrderHistoryRepository orderHistoryRepository;

	@Override
	public void write(String message) {
		log.info(String.format("OrderHistoryPersistenceServiceImpl::write\n %s", message));
	}

	@Override
	public void write(String orderNumber, String message) {
		log.info(String.format("OrderHistoryPersistenceServiceImpl::write\n %s | %s", orderNumber, message));

		OrderHistory orderHistory = new OrderHistory(orderNumber, message);
		orderHistoryRepository.save(orderHistory);
	}
}
