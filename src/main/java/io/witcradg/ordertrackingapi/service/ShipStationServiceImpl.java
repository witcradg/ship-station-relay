package io.witcradg.ordertrackingapi.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.witcradg.ordertrackingapi.entity.CustomerOrder;
import io.witcradg.ordertrackingapi.entity.Inventory;
import io.witcradg.ordertrackingapi.entity.OrderItems;
import io.witcradg.ordertrackingapi.persistence.IInventoryPersistenceService;
import io.witcradg.ordertrackingapi.persistence.IOrderHistoryPersistenceService;
import io.witcradg.ordertrackingapi.persistence.IOrderItemsPersistenceService;
import io.witcradg.ordertrackingapi.persistence.ISalesPersistenceService;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class ShipStationServiceImpl implements IShipStationService {
	private int TAGID_NOT_PAID = 40227;
	private int TAGID_PAID = 40228;
	
	@Value("${controller.useSquareApi}")
	private boolean useSquareApi;

	@Value("${shipstation.api.key}")
	private String shipstationApiKey;

	@Value("${shipstation.api.secret}")
	private String shipstationApiSecret;

	@Value("${aftership.api.key}")
	private String aftershipApiKey;

	@Value("${configuration.aftership.url}")
	private String aftership_url_base;

	@Autowired
	IOrderHistoryPersistenceService orderHistoryPersistenceService;

	@Autowired
	ISalesPersistenceService salesPersistenceService;

	@Autowired
	IInventoryPersistenceService inventoryPersistenceService;

	@Autowired
	IOrderItemsPersistenceService orderItemsPersistenceService;

	private RestTemplate restTemplate = new RestTemplate();
	private HttpHeaders shipHeaders = new HttpHeaders();
	private HttpHeaders afterShipHeaders = new HttpHeaders();

	@PostConstruct
	private void loadHeaders() throws Exception {

		String shipStationData = shipstationApiKey + ":" + shipstationApiSecret;
		String shipStationDataEncodedStr = Base64.getEncoder()
				.encodeToString(shipStationData.getBytes(StandardCharsets.UTF_8.name()));

		shipHeaders.add("Authorization", "Basic " + shipStationDataEncodedStr);
		shipHeaders.setContentType(MediaType.APPLICATION_JSON);

		// https://developers.aftership.com/reference/authentication
		// https://developers.aftership.com/reference/post-trackings
		ArrayList<MediaType> acceptMediaTypes = new ArrayList<>();
		acceptMediaTypes.add(MediaType.APPLICATION_JSON);
		afterShipHeaders.setAccept(acceptMediaTypes);
		afterShipHeaders.setContentType(MediaType.APPLICATION_JSON);
		afterShipHeaders.add("aftership-api-key", "28bbce66-0779-4d6e-be34-693ea11c6a35");
	}

	@Override
	public void postShipStationOrder(CustomerOrder customerOrder) {
		String publishURL = null;
		JSONObject body = null;
		HttpEntity<String> requestEntity = null;
		String response = null;
		JSONObject responseShipStation = null;

		try {
			publishURL = String.format("https://ssapi.shipstation.com/orders/createorder");

			body = createShipStationOrderBody(customerOrder);

			requestEntity = new HttpEntity<String>(body.toString(), shipHeaders);

			response = restTemplate.postForObject(publishURL, requestEntity, String.class);

			// convert the response String to a JSON object
			responseShipStation = new JSONObject(response);
		} catch (Exception e) {
			log.error("Error in postShipStationOrder: " + shipHeaders.toString());
			log.debug("publishURL: " + publishURL);
			log.debug("request body: " + body);
			log.debug("requestEntity: " + requestEntity);
			log.debug("response get: " + response);
			log.debug("responseShipStation: " + responseShipStation);
			throw e;
		}
	}

	@Override
	public void getShipStationOrder(String orderNumber) {

		String publishURL = null;
		ResponseEntity<String> response = null;
		JSONObject responseShipStation = null;

		try {
			// String.format("https://ssapi.shipstation.com/orders?orderNumber=D8G-1198");
			publishURL = String.format("https://ssapi.shipstation.com/orders?orderNumber=" + orderNumber);
			// String publishURL = String.format("https://ssapi.shipstation.com/orders");

			HttpEntity<Void> requestEntity = new HttpEntity<>(shipHeaders);

			response = restTemplate.exchange(publishURL, HttpMethod.GET, requestEntity, String.class);

			// convert the response String to a JSON object
			responseShipStation = new JSONObject(response);

		} catch (Exception e) {
			log.error("Error in CommunicatorServiceImpl::getShipStationOrder");
			log.debug("publishURL: " + publishURL);
			log.debug("responseShipStation: " + responseShipStation);
			log.debug("response body: " + responseShipStation.get("body"));
			throw e;
		}
	}

	/******************************************************************
	 * SHIP STATION RELAY METHODS
	 *******************************************************************/

	/**
	 * A WebHook at ShipStation is triggered when a label is printed. These labels
	 * are printed in a batch of orders (may be a batch of one). ShipStation sends a
	 * URL that we use to retrieve the batch details. A record of the sale of each
	 * item is persisted and the inventory is decremented.
	 */

	@Override
	public JSONObject getShipStationBatch(String resource_url) {
		

		JSONObject responseShipStation = null;
		String bdy = null;
		JSONObject jsonObjectBody = null;

		try {

			HttpEntity<Void> requestEntity = new HttpEntity<>(shipHeaders);

			ResponseEntity<String> response = restTemplate.exchange(resource_url, HttpMethod.GET, requestEntity,
					String.class);

			// convert the response String to a JSON object
			responseShipStation = new JSONObject(response);

			bdy = responseShipStation.getString("body");

			jsonObjectBody = new JSONObject(bdy);

		} catch (Exception e) {
			log.error("Error in CommunicatorServiceImpl::getShipStationOrder");
			log.debug("resource_url: " + resource_url);
			log.debug("responseShipStation: " + responseShipStation);
			log.debug("getShipStationBatch response body: " + bdy);

			throw e;
		}

		return jsonObjectBody;
	}

	/**
	 * Once batch details have been retrieved containing one or more orders. We send
	 * each order to AfterShip.
	 */

	public void processShipStationBatch(JSONObject shipstationBatch) {
		Integer total = null;
		Integer page = null;
		Integer pages = null;
		JSONObject shipstationOrder = null;
		JSONObject aftershipRecord = null;
		HttpEntity<String> request = null;
		JSONObject aftershipResponse = null;

		try {
			log.debug("entering processBatch function");

			// extract data not in the array
			total = shipstationBatch.getInt("total");
			page = shipstationBatch.getInt("pages");
			pages = shipstationBatch.getInt("pages");

			// get the shipments array
			JSONArray shipments = shipstationBatch.getJSONArray("shipments");

			// loop over all order records received from ShipStation
			for (int i = 0; i < shipments.length(); i++) {
				shipstationOrder = shipments.getJSONObject(i);

				aftershipRecord = createAfterShipTrackingRecord(shipstationOrder);

				String orderNumber = aftershipRecord.getString("order_number");
				orderHistoryPersistenceService.write(orderNumber, "Label printed");

				JSONObject requestBody = new JSONObject();
				requestBody.put("tracking", aftershipRecord);

				request = new HttpEntity<String>(requestBody.toString(), afterShipHeaders);

				String response = restTemplate.postForObject(aftership_url_base + "/trackings", request, String.class);

				// response body https://developers.aftership.com/reference/body-envelope
				aftershipResponse = new JSONObject(response);

				// If an exception occurs let's allow this to be skipped. Every exception
				// I've seen is due to "Tracking already exists" 400 error. That means
				// the values being saved here were saved in the first POST.
				persistSaleAndInventory(orderNumber);
			}
		} catch (Exception e) {
			String msg = e.getMessage();
			// This is not an error condition
			if (!msg.contains("Tracking already exists")) {
				log.error("Error in CommunicatorServiceImpl::processShipStationBatch");
				log.debug("processBatch: " + shipstationBatch.toString());
				log.debug("total: " + total);
				log.debug("page: " + page);
				log.debug("pages: " + pages);
				log.debug("shipstationOrder: " + shipstationOrder.toString());
				log.debug("aftershipRecord: " + aftershipRecord.toString());
				log.debug("request: " + request.toString());
				log.debug("aftershipResponse: " + aftershipResponse.toString());
				throw e;
			} else {
				log.debug("AfterShip returned %s", msg);
			}
		}
	}

	private JSONObject createShipStationOrderBody(CustomerOrder customerOrder) {
		JSONObject requestBody = new JSONObject();

		requestBody.put("orderNumber", customerOrder.getScInvoiceNumber());
		requestBody.put("orderDate", customerOrder.getScOrderDate());
		requestBody.put("orderStatus", "awaiting_shipment");

		requestBody.put("billTo", buildAddress(customerOrder));
		requestBody.put("shipTo", buildAddress(customerOrder));

		requestBody.put("amountPaid", customerOrder.getScInvoiceTotal());
		requestBody.put("shippingPaid", customerOrder.getShippingTotal());
		requestBody.put("customerEmail", customerOrder.getEmailAddress());

		JSONObject advancedOptions = new JSONObject().put("customField1", "PAID as advanced option");
		requestBody.put("advancedOptions",  advancedOptions);
		
		int tagid = useSquareApi ? TAGID_NOT_PAID : TAGID_PAID;
		JSONArray tagIds = new JSONArray().put(tagid);
		requestBody.put("tagid", tagIds);
		/*
		 * Extract items from the customer order and add the relevant fields to the ship
		 * station order
		 */
		JSONArray items = customerOrder.getItems();
		ArrayList<JSONObject> shipItems = new ArrayList<>();

		for (int i = 0; i < items.length(); i++) {

			JSONObject orderItem = items.getJSONObject(i);

			JSONObject shipItem = new JSONObject();
			if (!orderItem.getString("name").equals("Recurring plan")) {
				shipItem.put("unitPrice", orderItem.getInt("price"));
				shipItem.put("quantity", orderItem.getInt("quantity"));
				shipItem.put("name", orderItem.getString("name"));
				shipItems.add(shipItem);
			}
		}
		requestBody.put("items", shipItems);
		log.info("requestBody: " + requestBody);
		return requestBody;
	}

	private JSONObject createAfterShipTrackingRecord(JSONObject shipstationOrder) {
		JSONObject trackingRecord = new JSONObject();

		// BigInteger shipmentNbr = shipstationOrder.getBigInteger("shipmentId");
		String orderNbr = shipstationOrder.getString("orderNumber");
		String trackingNbr = shipstationOrder.getString("trackingNumber");
		JSONObject shipTo = shipstationOrder.getJSONObject("shipTo");
		String customerName = shipTo.getString("name");
		// String shipDate = shipstationOrder.getString("shipDate"); //Actually the date
		// ShipStation printed the shipping labels

		trackingRecord.put("tracking_number", trackingNbr);

		// title
		String timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp);
		String title = "OrderTrackingAutomation-" + timeStamp;
		trackingRecord.put("title", title);

		// emails
		JSONArray emails = new JSONArray();
		String email = shipstationOrder.getString("customerEmail");
		log.debug(email);
		emails.put(email);
		trackingRecord.put("emails", emails);

		trackingRecord.put("order_id", orderNbr);

		trackingRecord.put("order_number", orderNbr);

		trackingRecord.put("language", "en");

		trackingRecord.put("customerName", customerName);

		return trackingRecord;
	}

	/**
	 * Write a record of each items sold for the order then decrement the number
	 * sold from the inventory table.
	 * 
	 * @param orderNumber
	 */
	private void persistSaleAndInventory(String orderNumber) {

		JSONArray orderItems = null;
		String sku = null;
		String productName = null;
		BigDecimal unitPrice = null;
		BigDecimal totalPrice = null;
		Integer quantitySold = null;
		Inventory inventory = null;

		try {

			OrderItems orderItemsObject = orderItemsPersistenceService.read(orderNumber);

			orderItems = new JSONArray(orderItemsObject.getOrderItems());

			Iterator<Object> it = orderItems.iterator();
			while (it.hasNext()) {
				JSONObject item = (JSONObject) it.next();
				sku = item.getString("id");
				productName = item.getString("name");
				unitPrice = item.getBigDecimal("unitPrice");
				totalPrice = item.getBigDecimal("totalPrice");
				quantitySold = item.getInt("quantity");

				// record the details of the item sold
				salesPersistenceService.write(orderNumber, sku, productName, quantitySold, unitPrice, totalPrice);

				// update the inventory stock on hand
				inventory = inventoryPersistenceService.read(sku);
				inventory.setOnHand(inventory.getOnHand() - quantitySold);
				inventoryPersistenceService.save(inventory);
			}
		} catch (Exception e) {
			log.debug("persistSaleAndInventory exception for order: " + orderNumber);
			log.debug("orderItems: " + orderItems);
			log.debug(String.format(
					"orderNumber: %s\n sku: %s\n productName: %s\n quantitySold: %d\n unitPrice: %d\n, totalPrice: %d\n",
					orderNumber, sku, productName, quantitySold, unitPrice, totalPrice));
			log.debug("inventory: " + inventory);
			throw e;
		}
	}

	private JSONObject buildAddress(CustomerOrder customerOrder) {
		JSONObject address = new JSONObject();
		address.put("name", customerOrder.getFullName());
		address.put("company", customerOrder.getCompanyName());
		address.put("street1", customerOrder.getAddressLine1());
		address.put("street2", customerOrder.getAddressLine2());
		address.put("street3", customerOrder.getAddressLine3());
		address.put("city", customerOrder.getCity());
		address.put("state", customerOrder.getState());
		address.put("postalCode", customerOrder.getPostalCode());
		address.put("country", "US");
		address.put("phone", customerOrder.getPhoneNumber());
		return address;
	}

	/**
	 * runShipStationUtility
	 * 
	 * This method is intended for use ONLY during development. I need to
	 * communicate with the ShipStation API to determine values that are not
	 * accessible from their GUI. Initially, this is the value of the tagids of Tags
	 * that Devin created. The tags can be manipulated from the GUI web page, but the
	 * tagid is needed to allow this application to change the state from Not Paid
	 * to Paid.
	 * 
	 * I anticipate I will be modifying this method as needed.
	 * 
	 */

	@Override
	public void runShipStationUtility() {
		String publishURL = null;
		ResponseEntity<String> response = null;
		JSONObject responseShipStation = null;

		try {
			publishURL = String.format("https://ssapi.shipstation.com/accounts/listtags");

			HttpEntity<Void> requestEntity = new HttpEntity<>(shipHeaders);

			response = restTemplate.exchange(publishURL, HttpMethod.GET, requestEntity, String.class);

			responseShipStation = new JSONObject(response);
			log.info("responseShipStation: " + responseShipStation);

		} catch (Exception e) {
			log.error("Error in CommunicatorServiceImpl::runShipStationUtility");
			log.debug("publishURL: " + publishURL);
			log.debug("responseShipStation: " + responseShipStation);
			log.debug("response body: " + responseShipStation.get("body"));
			throw e;
		}
	}

}
