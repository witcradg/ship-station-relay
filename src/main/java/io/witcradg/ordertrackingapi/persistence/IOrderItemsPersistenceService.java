package io.witcradg.ordertrackingapi.persistence;

import io.witcradg.ordertrackingapi.entity.OrderItems;

public interface IOrderItemsPersistenceService {
	public abstract OrderItems read(String orderNumber);
	public abstract void write(String orderNumber, String items);
}
