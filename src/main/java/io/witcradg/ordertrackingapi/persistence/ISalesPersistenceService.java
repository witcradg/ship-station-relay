package io.witcradg.ordertrackingapi.persistence;

import java.math.BigDecimal;

public interface ISalesPersistenceService {
	public abstract void write(
			String orderNumber, 
			String sku,
			String productName, 
			Integer quantitySold, 
			BigDecimal unitPrice, 
			BigDecimal totalPrice);
}
