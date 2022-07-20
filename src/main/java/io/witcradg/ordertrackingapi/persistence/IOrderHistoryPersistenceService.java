package io.witcradg.ordertrackingapi.persistence;

public interface IOrderHistoryPersistenceService {
	public abstract void write(String message);
	public abstract void write(String orderNumber, String message);
}
