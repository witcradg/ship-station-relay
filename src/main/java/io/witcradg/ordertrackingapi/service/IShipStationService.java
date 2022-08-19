package io.witcradg.ordertrackingapi.service;

import org.json.JSONObject;

import io.witcradg.ordertrackingapi.entity.CustomerOrder;

public interface IShipStationService {
	public abstract void postShipStationOrder(CustomerOrder customerOrder) throws Exception;
	public abstract void getShipStationOrder(String orderNumber);
	public abstract JSONObject getShipStationBatch(String resource_url);
	public abstract void processShipStationBatch(JSONObject shipstationData);
	public abstract void runShipStationUtility();

}
