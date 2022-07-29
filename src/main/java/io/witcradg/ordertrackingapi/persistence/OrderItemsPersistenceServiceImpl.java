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

	/**
	 * The details of each sold item in the order are saved when the order is
	 * received from SnipCart. These records are later used to decrement inventory
	 * records after the labels are printed at ShipStation when we can be certain
	 * the order has not been cancelled for one of several reasons.
	 */

	@Override
	public void write(String orderNumber, String items) {
		log.info(String.format("OrderItemsPersistenceServiceImpl::write\n %s ", orderNumber));

		OrderItems order = new OrderItems(orderNumber, items);
		orderItemsRepository.save(order);
	}
}
