package io.witcradg.ordertrackingapi.persistence;

import java.math.BigInteger;

public interface ISalesPersistenceService {
	public abstract void write(
			String orderNumber, 
			String sku,
			String productName, 
			Integer quantitySold, 
			BigInteger unitPrice, 
			BigInteger totalPrice);
}
