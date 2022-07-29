package io.witcradg.ordertrackingapi.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.witcradg.ordertrackingapi.entity.Inventory;
import io.witcradg.ordertrackingapi.repository.InventoryRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class InventoryPersistenceServiceimpl implements IInventoryPersistenceService {

	@Autowired
	private InventoryRepository inventoryRepository;

	@Override
	public Inventory read(String sku) {
		
		Inventory inventory = inventoryRepository.findInventoryBySku(sku);
		
		return inventory;
	}

	@Override
	public void write(String sku, String productName, Integer onHand) {
		log.debug(String.format("InventoryPersistenceServiceimpl::write\n %s | %s | %s", sku, productName, onHand));

		Inventory inventory = new Inventory(sku, productName, onHand);

		inventoryRepository.save(inventory);
	}
	
	@Override
	public void save(Inventory inventory) {
		inventoryRepository.save(inventory);
	}
	
	
}
