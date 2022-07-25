package io.witcradg.ordertrackingapi.persistence;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.witcradg.ordertrackingapi.entity.OrderItems;
import io.witcradg.ordertrackingapi.repository.OrderItemsRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class OrderItemsPersistenceServiceImpl implements IOrderItemsPersistenceService {
	
	@Autowired
	private OrderItemsRepository orderItemsRepository;
	
	@Override
	public OrderItems read(String orderNumber) {
		OrderItems orderItems = orderItemsRepository.findOrderItemsByOrderNumber(orderNumber);
		return orderItems;
	}
	
	@Override
	public void write(String orderNumber, String items) {
		log.info(String.format("OrderItemsPersistenceServiceImpl::write\n %s | %s", orderNumber, items));

		OrderItems order = new OrderItems(orderNumber, items);
		orderItemsRepository.save(order);
	}
}
