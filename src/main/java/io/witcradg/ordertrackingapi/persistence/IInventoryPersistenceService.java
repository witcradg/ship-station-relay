package io.witcradg.ordertrackingapi.persistence;

import io.witcradg.ordertrackingapi.entity.Inventory;

public interface IInventoryPersistenceService {

	public abstract Inventory read(String sku);

	public abstract void write(String sku, String productName, Integer onHand);

	public abstract void save(Inventory inventory);
}
