package io.witcradg.shipstationrelayapi.service;

import io.witcradg.shipstationrelayapi.entity.CustomerOrder;
import org.json.JSONObject;

public interface ICommunicatorService {
	public abstract void createCustomer(CustomerOrder customerOrder) throws Exception;
	public abstract void createOrder(CustomerOrder customerOrder) throws Exception;
	public abstract void createInvoice(CustomerOrder customerOrder) throws Exception;
	public abstract void publishInvoice(CustomerOrder customerOrder) throws Exception;
	public abstract void sendSms(CustomerOrder customerOrder) throws Exception;
	public abstract void postShipStationOrder(CustomerOrder customerOrder) throws Exception;
	public abstract void getShipStationOrder(String orderNumber);
	public abstract JSONObject getShipStationBatch(String resource_url);
	public abstract void processShipStationBatch(JSONObject shipstationData);
}
