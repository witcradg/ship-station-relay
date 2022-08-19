package io.witcradg.ordertrackingapi.service;

//import java.math.BigDecimal;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
//import java.util.Base64;
//import java.util.Calendar;
//import java.util.Iterator;
import java.util.UUID;

//import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.log4j.Log4j2;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import io.witcradg.ordertrackingapi.entity.CustomerOrder;
import io.witcradg.ordertrackingapi.exception.InvalidPhoneNumberException;
import io.witcradg.ordertrackingapi.persistence.IOrderHistoryPersistenceService;

//import io.witcradg.ordertrackingapi.entity.Inventory;
//import io.witcradg.ordertrackingapi.entity.OrderItems;
//import io.witcradg.ordertrackingapi.persistence.IInventoryPersistenceService;
//import io.witcradg.ordertrackingapi.persistence.IOrderItemsPersistenceService;
//import io.witcradg.ordertrackingapi.persistence.ISalesPersistenceService;

@Log4j2
@Service
public class CommunicatorServiceImpl implements ICommunicatorService {

	@Value("${configuration.square.url}")
	private String url_base;

	@Value("${configuration.square.access}")
	private String auth;

	@Value("${configuration.square.location}")
	private String location;

	@Value("${configuration.square.delivery}")
	private String delivery;

	@Value("${twilio.account.sid}")
	private String twilioSid;

	@Value("${twilio.auth.token}")
	private String twilioAuthToken;

	@Value("${twilio.phone.number}")
	private String twilioPhoneNumber;

//	@Value("${shipstation.api.key}")
//	private String shipstationApiKey;
//
//	@Value("${shipstation.api.secret}")
//	private String shipstationApiSecret;
//
//	@Value("${aftership.api.key}")
//	private String aftershipApiKey;
//
//	@Value("${configuration.aftership.url}")
//	private String aftership_url_base;

	@Autowired
	IOrderHistoryPersistenceService orderHistoryPersistenceService;

//	@Autowired
//	IOrderItemsPersistenceService orderItemsPersistenceService;

//	@Autowired
//	ISalesPersistenceService salesPersistenceService;
//
//	@Autowired
//	IInventoryPersistenceService inventoryPersistenceService;

	private RestTemplate restTemplate = new RestTemplate();
	private HttpHeaders headers = new HttpHeaders();
//	private HttpHeaders shipHeaders = new HttpHeaders();
//	private HttpHeaders afterShipHeaders = new HttpHeaders();

//	@PostConstruct
//	private void loadHeaders() throws Exception {
//		headers.add("Square-Version", "2021-04-21");
//		headers.add("Authorization", "Bearer " + auth);
//		headers.setContentType(MediaType.APPLICATION_JSON);
//
//		String shipStationData = shipstationApiKey + ":" + shipstationApiSecret;
//		String shipStationDataEncodedStr = Base64.getEncoder()
//				.encodeToString(shipStationData.getBytes(StandardCharsets.UTF_8.name()));
//
//		shipHeaders.add("Authorization", "Basic " + shipStationDataEncodedStr);
//		shipHeaders.setContentType(MediaType.APPLICATION_JSON);
//
//		// https://developers.aftership.com/reference/authentication
//		// https://developers.aftership.com/reference/post-trackings
//		ArrayList<MediaType> acceptMediaTypes = new ArrayList<>();
//		acceptMediaTypes.add(MediaType.APPLICATION_JSON);
//		afterShipHeaders.setAccept(acceptMediaTypes);
//		afterShipHeaders.setContentType(MediaType.APPLICATION_JSON);
//		afterShipHeaders.add("aftership-api-key", "28bbce66-0779-4d6e-be34-693ea11c6a35");
//	}

	@Override
	public void createCustomer(CustomerOrder customerOrder) throws Exception {

		JSONObject requestBody = null;
		HttpEntity<String> request = null;
		String response = null;
		String customerId = null;

		try {
			JSONObject addressObject = new JSONObject();
			addressObject.put("address_line_1", customerOrder.getAddressLine1());
			addressObject.put("address_line_2", customerOrder.getAddressLine2());
			addressObject.put("address_line_3", customerOrder.getAddressLine3());
			addressObject.put("administrative_district_level_1", customerOrder.getCity());
			addressObject.put("administrative_district_level_2", customerOrder.getState());

			addressObject.put("country", "US");
			addressObject.put("postal_code", customerOrder.getPostalCode());

			requestBody = new JSONObject();
			requestBody.put("email_address", customerOrder.getEmailAddress());
			requestBody.put("family_name", customerOrder.getFamilyName());
			requestBody.put("given_name", customerOrder.getGivenName());
			requestBody.put("idempotency_key", UUID.randomUUID().toString());
			requestBody.put("phone_number", customerOrder.getPhoneNumber());
			requestBody.put("address", addressObject);

			request = new HttpEntity<String>(requestBody.toString(), headers);

			response = restTemplate.postForObject(url_base + "/customers", request, String.class);
			JSONObject responseCustomer = new JSONObject(response);
			customerId = responseCustomer.getJSONObject("customer").getString("id");
			customerOrder.setSqCustomerId(customerId);

		} catch (Exception e) {
			log.error("Error in CommunicatorServiceImpl::createCustomer: " + customerOrder.toString());
			log.debug("request: " + request.getBody());
			log.debug("response: " + response);
			log.debug("responseCustomer id: " + customerId);
			throw e;
		}
	}

	@Override
	public void createOrder(CustomerOrder customerOrder) throws Exception {

		HttpEntity<String> request = null;
		String response = null;
		String responseOrderId = null;

		try {

			JSONObject basePriceMoney = new JSONObject();
			basePriceMoney.put("amount", customerOrder.getScInvoiceTotal());
			basePriceMoney.put("currency", "USD");

			JSONObject lineItem = new JSONObject();
			lineItem.put("quantity", "1");
			lineItem.put("base_price_money", basePriceMoney);
			lineItem.put("name", customerOrder.getScInvoiceNumber());

			ArrayList<JSONObject> lineItemArray = new ArrayList<>();
			lineItemArray.add(lineItem);

			JSONObject order = new JSONObject();
			order.put("location_id", location);
			order.put("line_items", lineItemArray);

			JSONObject requestBody = new JSONObject();
			requestBody.put("idempotency_key", UUID.randomUUID().toString());
			requestBody.put("order", order);

			request = new HttpEntity<String>(requestBody.toString(), headers);

			response = restTemplate.postForObject(url_base + "/orders", request, String.class);

			JSONObject responseOrder = new JSONObject(response);
			responseOrderId = responseOrder.getJSONObject("order").getString("id");
			customerOrder.setSqOrderId(responseOrderId);

		} catch (Exception e) {
			log.error("Error in CommunicatorServiceImpl::createOrder");
			log.debug("createOrder: " + customerOrder.toString());
			log.debug("request: " + request.getBody());
			log.debug(" responseOrder id: " + responseOrderId);
			throw e;
		}
	}

	@Override
	public void createInvoice(CustomerOrder customerOrder) {

		String scheduledAt = null;
		String dueDate = null;
		String invoiceId = null;
		int version = 0;
		HttpEntity<String> request = null;
		String response = null;

		try {

			JSONObject primaryRecipient = new JSONObject();
			primaryRecipient.put("customer_id", customerOrder.getSqCustomerId());

			JSONObject acceptedPaymentMethods = new JSONObject();
			acceptedPaymentMethods.put("bank_account", false); // TODO string or boolean?
			acceptedPaymentMethods.put("card", true); // TODO string or boolean?
			acceptedPaymentMethods.put("square_gift_card", false);

			JSONObject paymentRequest = new JSONObject();
			paymentRequest.put("automatic_payment_source", "NONE");

			paymentRequest.put("request_type", "BALANCE");

			JSONArray paymentRequests = new JSONArray();
			paymentRequests.put(paymentRequest);

			JSONObject invoiceObject = new JSONObject();
			invoiceObject.put("accepted_payment_methods", acceptedPaymentMethods);
			invoiceObject.put("delivery_method", delivery);
			invoiceObject.put("invoice_number", customerOrder.getScInvoiceNumber());
			invoiceObject.put("location_id", location);
			invoiceObject.put("order_id", customerOrder.getSqOrderId());
			invoiceObject.put("payment_requests", paymentRequests);

			// Date operations

			Instant scheduledInstant = Instant.now().plus(1, ChronoUnit.MINUTES);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
					.withZone(ZoneId.of("UTC"));
			scheduledAt = formatter.format(scheduledInstant);

			invoiceObject.put("scheduled_at", scheduledAt);

			dueDate = scheduledInstant.toString().substring(0, 10);
			paymentRequest.put("due_date", dueDate);

			invoiceObject.put("primary_recipient", primaryRecipient);

			JSONObject requestBody = new JSONObject();
			requestBody.put("idempotency_key", UUID.randomUUID().toString());
			requestBody.put("invoice", invoiceObject);

			request = new HttpEntity<String>(requestBody.toString(), headers);

			response = restTemplate.postForObject(url_base + "/invoices", request, String.class);

			JSONObject responseInvoice = new JSONObject(response);
			JSONObject invoice = responseInvoice.getJSONObject("invoice");
			invoiceId = invoice.getString("id");
			version = invoice.getInt("version");
			customerOrder.setSqInvoiceId(invoiceId);
			customerOrder.setSqInvoiceVersion(version);
		} catch (Exception e) {
			log.error("Error in CommunicatorServiceImpl::createInvoice	");
			log.debug("createInvoice: " + customerOrder.toString());
			log.debug("scheduled_at: " + scheduledAt);
			log.debug("due_date: " + dueDate);
			log.debug("request: " + request.getBody());
			log.debug("response: \n" + response);
			log.debug("invoice id: " + invoiceId);
			log.debug("invoice version: " + version);
			throw e;
		}
	}

	@Override
	public void publishInvoice(CustomerOrder customerOrder) {

		HttpEntity<String> request = null;
		String response = null;
		JSONObject invoiceObject = null;

		try {
			JSONObject requestBody = new JSONObject();
			requestBody.put("idempotency_key", UUID.randomUUID().toString());
			requestBody.put("version", customerOrder.getSqInvoiceVersion().intValue());

			request = new HttpEntity<String>(requestBody.toString(), headers);
			String publishURL = String.format(url_base + "/invoices/%s/publish", customerOrder.getSqInvoiceId());
			response = restTemplate.postForObject(publishURL, request, String.class);

			// convert the response String to a json object
			JSONObject responseInvoice = new JSONObject(response);

			// from the response object get the invoice
			invoiceObject = responseInvoice.getJSONObject("invoice");

			// work-around
			String publicURL = "https://squareup.com/pay-invoice/" + invoiceObject.getString("id");
			log.debug("publicURL for setPaymentURL: " + publicURL);

			customerOrder.setPaymentURL(publicURL);
		} catch (Exception e) {
			log.error("Error in CommunicatorServiceImpl::publishInvoice");
			log.debug("customerOrder: " + customerOrder);
			log.debug("request: " + request.getBody());
			log.debug("response publish invoice: " + response);
			log.debug("invoiceObject: " + invoiceObject);

			throw e;
		}
	}

	@Override
	public void sendSms(CustomerOrder customerOrder) throws InvalidPhoneNumberException {

		String sendTo = null;

		try {
			String str = customerOrder.getPhoneNumber();
			if (str == null) {
				log.info("Mising phone number. Skipping SMS for order: " + customerOrder);
				return;
			}
			
			StringBuilder stringBuilder = new StringBuilder();

			for (char dig : str.toCharArray()) {
				if (Character.isDigit(dig)) {
					stringBuilder.append(dig);
				}
			}

			String tmp = stringBuilder.toString();

			if (tmp.length() == 10) {
				stringBuilder.insert(0, "+1");
			} else if (tmp.length() == 11 && tmp.startsWith("1")) {
				stringBuilder.insert(0, '+');
			} else if (tmp.length() != 12 || !tmp.startsWith("+1")) {
				log.info("invalid phone number " + customerOrder.getPhoneNumber());
				throw new InvalidPhoneNumberException(customerOrder.getPhoneNumber());
			}

			sendTo = stringBuilder.toString();

			String messageContent = String.format(
					"Hey %s, Thank You for your Order on the D8G website. Use this link to Complete Your Purchase: %s "
							+ "**Be Advised It takes up to 2 minutes before you can Complete Your Payment**\n",
					customerOrder.getFullName(), customerOrder.getPaymentURL());

			Twilio.init(twilioSid, twilioAuthToken);

			Message message = Message
					.creator(new PhoneNumber(sendTo), new PhoneNumber(twilioPhoneNumber), messageContent).create();

			log.info("twilio message sid: " + message.getSid());
		} catch (Exception e) {
			log.error("Error in CommunicatorServiceImpl::sendSms");
			log.debug("sendTo: " + sendTo);
			log.debug("customerOrder: " + customerOrder);
			throw e;
		}
	}

	/******************************************************************
	 * SHIP STATION METHODS
	 *******************************************************************/

//	@Override
//	public void postShipStationOrder(CustomerOrder customerOrder) {
//		String publishURL = null;
//		JSONObject body = null;
//		HttpEntity<String> requestEntity = null;
//		String response = null;
//        JSONObject responseShipStation = null;
//
//		try {
//			publishURL = String.format("https://ssapi.shipstation.com/orders/createorder");
//
//			body = createShipStationOrderBody(customerOrder);
//
//			requestEntity = new HttpEntity<String>(body.toString(), shipHeaders);
//
//			response = restTemplate.postForObject(publishURL, requestEntity, String.class);
//
//			// convert the response String to a JSON object
//			responseShipStation = new JSONObject(response);
//		} catch (Exception e) {
//			log.error("Error in postShipStationOrder: " + shipHeaders.toString());
//			log.debug("publishURL: " + publishURL);
//			log.debug("request body: " + body);
//			log.debug("requestEntity: " + requestEntity);
//			log.debug("response get: " + response);
//            log.debug("responseShipStation: " + responseShipStation);
//			throw e;
//		}
//	}
//
//	@Override
//	public void getShipStationOrder(String orderNumber) {
//
//		String publishURL = null;
//		ResponseEntity<String> response = null;
//		JSONObject responseShipStation = null;
//
//		try {
//			// String.format("https://ssapi.shipstation.com/orders?orderNumber=D8G-1198");
//			publishURL = String.format("https://ssapi.shipstation.com/orders?orderNumber=" + orderNumber);
//			// String publishURL = String.format("https://ssapi.shipstation.com/orders");
//
//			HttpEntity<Void> requestEntity = new HttpEntity<>(shipHeaders);
//
//			response = restTemplate.exchange(publishURL, HttpMethod.GET, requestEntity, String.class);
//
//			// convert the response String to a JSON object
//			responseShipStation = new JSONObject(response);
//
//		} catch (Exception e) {
//			log.error("Error in CommunicatorServiceImpl::getShipStationOrder");
//			log.debug("publishURL: " + publishURL);
//			log.debug("responseShipStation: " + responseShipStation);
//			log.debug("response body: " + responseShipStation.get("body"));
//			throw e;
//		}
//	}
//
//	/******************************************************************
//	 * SHIP STATION RELAY METHODS
//	 *******************************************************************/
//
//	/**
//	 * A WebHook at ShipStation is triggered when a label is printed. These labels
//	 * are printed in a batch of orders (may be a batch of one). ShipStation sends a
//	 * URL that we use to retrieve the batch details. A record of the sale of each
//	 * item is persisted and the inventory is decremented.
//	 */
//
//	@Override
//	public JSONObject getShipStationBatch(String resource_url) {
//
//		JSONObject responseShipStation = null;
//		String bdy = null;
//		JSONObject jsonObjectBody = null;
//
//		try {
//
//			HttpEntity<Void> requestEntity = new HttpEntity<>(shipHeaders);
//
//			ResponseEntity<String> response = restTemplate.exchange(resource_url, HttpMethod.GET, requestEntity,
//					String.class);
//
//			// convert the response String to a JSON object
//			responseShipStation = new JSONObject(response);
//
//			bdy = responseShipStation.getString("body");
//
//			jsonObjectBody = new JSONObject(bdy);
//
//		} catch (Exception e) {
//			log.error("Error in CommunicatorServiceImpl::getShipStationOrder");
//			log.debug("resource_url: " + resource_url);
//			log.debug("responseShipStation: " + responseShipStation);
//			log.debug("getShipStationBatch response body: " + bdy);
//
//			throw e;
//		}
//
//		return jsonObjectBody;
//	}
//
//	/**
//	 * Once batch details have been retrieved containing one or more orders. We send
//	 * each order to AfterShip.
//	 */
//
//	public void processShipStationBatch(JSONObject shipstationBatch) {
//		Integer total = null;
//		Integer page = null;
//		Integer pages = null;
//		JSONObject shipstationOrder = null;
//		JSONObject aftershipRecord = null;
//		HttpEntity<String> request = null;
//		JSONObject aftershipResponse = null;
//
//		try {
//			log.debug("entering processBatch function");
//
//			// extract data not in the array
//			total = shipstationBatch.getInt("total");
//			page = shipstationBatch.getInt("pages");
//			pages = shipstationBatch.getInt("pages");
//
//			// get the shipments array
//			JSONArray shipments = shipstationBatch.getJSONArray("shipments");
//
//			// loop over all order records received from ShipStation
//			for (int i = 0; i < shipments.length(); i++) {
//				shipstationOrder = shipments.getJSONObject(i);
//
//				aftershipRecord = createAfterShipTrackingRecord(shipstationOrder);
//
//				String orderNumber = aftershipRecord.getString("order_number");
//				orderHistoryPersistenceService.write(orderNumber, "Label printed");
//
//				JSONObject requestBody = new JSONObject();
//				requestBody.put("tracking", aftershipRecord);
//
//				request = new HttpEntity<String>(requestBody.toString(), afterShipHeaders);
//
//				String response = restTemplate.postForObject(aftership_url_base + "/trackings", request, String.class);
//
//				// response body https://developers.aftership.com/reference/body-envelope
//				aftershipResponse = new JSONObject(response);
//
//				//If an exception occurs let's allow this to be skipped. Every exception 
//				// I've seen is due to "Tracking already exists" 400 error. That means 
//				// the values being saved here were saved in the first POST.
//				persistSaleAndInventory(orderNumber);
//			}
//		} catch (Exception e) {
//			String msg = e.getMessage();
//			//This is not an error condition
//			if (!msg.contains("Tracking already exists")) {
//				log.error("Error in CommunicatorServiceImpl::processShipStationBatch");
//				log.debug("processBatch: " + shipstationBatch.toString());
//				log.debug("total: " + total);
//				log.debug("page: " + page);
//				log.debug("pages: " + pages);
//				log.debug("shipstationOrder: " + shipstationOrder.toString());
//				log.debug("aftershipRecord: " + aftershipRecord.toString());
//				log.debug("request: " + request.toString());
//				log.debug("aftershipResponse: " + aftershipResponse.toString());
//				throw e;
//			} else {
//				log.debug("AfterShip returned %s", msg);
//			}
//		}
//	}

	/*****************************************************************
	 * Private Methods
	 **/

//	private JSONObject buildAddress(CustomerOrder customerOrder) {
//		JSONObject address = new JSONObject();
//		address.put("name", customerOrder.getFullName());
//		address.put("company", customerOrder.getCompanyName());
//		address.put("street1", customerOrder.getAddressLine1());
//		address.put("street2", customerOrder.getAddressLine2());
//		address.put("street3", customerOrder.getAddressLine3());
//		address.put("city", customerOrder.getCity());
//		address.put("state", customerOrder.getState());
//		address.put("postalCode", customerOrder.getPostalCode());
//		address.put("country", "US");
//		address.put("phone", customerOrder.getPhoneNumber());
//		return address;
//	}

//	private JSONObject createShipStationOrderBody(CustomerOrder customerOrder) {
//		JSONObject requestBody = new JSONObject();
//
//		requestBody.put("orderNumber", customerOrder.getScInvoiceNumber());
//		requestBody.put("orderDate", customerOrder.getScOrderDate());
//		requestBody.put("orderStatus", "awaiting_shipment");
//
//		requestBody.put("billTo", buildAddress(customerOrder));
//		requestBody.put("shipTo", buildAddress(customerOrder));
//
//		requestBody.put("amountPaid", customerOrder.getScInvoiceTotal());
//		requestBody.put("shippingPaid", customerOrder.getShippingTotal());
//		requestBody.put("customerEmail", customerOrder.getEmailAddress());
//		requestBody.put("tagid", 123);
//		requestBody.put("pmtStatus", "NOT PAID");
//		requestBody.put("customField1", "NOT PAID in custom field");
//		
//		/*
//		 * Extract items from the customer order and add the relevant fields to the ship
//		 * station order
//		 */
//		JSONArray items = customerOrder.getItems();
//		ArrayList<JSONObject> shipItems = new ArrayList<>();
//
//		for (int i = 0; i < items.length(); i++) {
//
//			JSONObject orderItem = items.getJSONObject(i);
//
//			JSONObject shipItem = new JSONObject();
//			if (!orderItem.getString("name").equals("Recurring plan")) {
//				shipItem.put("unitPrice", orderItem.getInt("price"));
//				shipItem.put("quantity", orderItem.getInt("quantity"));
//				shipItem.put("name", orderItem.getString("name"));
//				shipItems.add(shipItem);
//			}
//		}
//		requestBody.put("items", shipItems);
//		log.info("requestBody: " + requestBody);
//		return requestBody;
//	}
//
	/*
	 * Functional but not complete pending a decision on how to handle emails and
	 * phone numbers to avoid sharing with ShipStation.
	 */

//	private JSONObject createAfterShipTrackingRecord(JSONObject shipstationOrder) {
//		JSONObject trackingRecord = new JSONObject();
//
//		// BigInteger shipmentNbr = shipstationOrder.getBigInteger("shipmentId");
//		String orderNbr = shipstationOrder.getString("orderNumber");
//		String trackingNbr = shipstationOrder.getString("trackingNumber");
//		JSONObject shipTo = shipstationOrder.getJSONObject("shipTo");
//		String customerName = shipTo.getString("name");
//		// String shipDate = shipstationOrder.getString("shipDate"); //Actually the date
//		// ShipStation printed the shipping labels
//
//		// TODO Is there a difference for AfterShip between USPS Priority Mail and USPS
//		// regular main?
//		// I'm guessing not since it doesn't seem to be offered as a carrier option in
//		// AfterShip, but I should check.
//
//		// slug
//		// trackingRecord.put("slug", "usps");
//
//		// tracking number
//		trackingRecord.put("tracking_number", trackingNbr);
//
//		// title
//		String timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
//		System.out.println(timeStamp);
//		String title = "OrderTrackingAutomation-" + timeStamp;
//		trackingRecord.put("title", title);
//
//		// emails
//		JSONArray emails = new JSONArray();
//		String email = shipstationOrder.getString("customerEmail");
//		log.debug(email);
//		emails.put(email);
//		trackingRecord.put("emails", emails);
//
//		trackingRecord.put("order_id", orderNbr);
//
//		trackingRecord.put("order_number", orderNbr);
//
//		trackingRecord.put("language", "en");
//
//		trackingRecord.put("customerName", customerName);
//
//		return trackingRecord;
//	}

//	/**
//	 * Write a record of each items sold for the order then decrement the number
//	 * sold from the inventory table.
//	 * 
//	 * @param orderNumber
//	 */
//	private void persistSaleAndInventory(String orderNumber) {
//
//		JSONArray orderItems = null;
//		String sku = null;
//		String productName = null;
//		BigDecimal unitPrice = null;
//		BigDecimal totalPrice = null;
//		Integer quantitySold = null;
//		Inventory inventory = null;
//
//		try {
//
//			OrderItems orderItemsObject = orderItemsPersistenceService.read(orderNumber);
//
//			orderItems = new JSONArray(orderItemsObject.getOrderItems());
//
//			Iterator<Object> it = orderItems.iterator();
//			while (it.hasNext()) {
//				JSONObject item = (JSONObject) it.next();
//				sku = item.getString("id");
//				productName = item.getString("name");
//				unitPrice = item.getBigDecimal("unitPrice");
//				totalPrice = item.getBigDecimal("totalPrice");
//				quantitySold = item.getInt("quantity");
//
//				// record the details of the item sold
//				salesPersistenceService.write(orderNumber, sku, productName, quantitySold, unitPrice, totalPrice);
//
//				// update the inventory stock on hand
//				inventory = inventoryPersistenceService.read(sku);
//				inventory.setOnHand(inventory.getOnHand() - quantitySold);
//				inventoryPersistenceService.save(inventory);
//			}
//		} catch (Exception e) {
//			log.debug("persistSaleAndInventory exception for order: " + orderNumber);
//			log.debug("orderItems: " + orderItems);
//			log.debug(String.format(
//					"orderNumber: %s\n sku: %s\n productName: %s\n quantitySold: %d\n unitPrice: %d\n, totalPrice: %d\n",
//					orderNumber, sku, productName, quantitySold, unitPrice, totalPrice));
//			log.debug("inventory: " + inventory);
//			throw e;
//		}
//	}
}
