package io.witcradg.shipstationrelayapi.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.witcradg.shipstationrelayapi.entity.CustomerOrder;

import lombok.extern.log4j.Log4j2;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

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
	
	@Value("${shipstation.api.key}")
	private String shipstationApiKey;
	
	@Value("${shipstation.api.secret}")
	private String shipstationApiSecret;
		
	private RestTemplate restTemplate = new RestTemplate();
	private HttpHeaders headers = new HttpHeaders();
	private HttpHeaders shipHeaders = new HttpHeaders();


	@PostConstruct
	private void loadHeaders() throws Exception  {
		headers.add("Square-Version", "2021-04-21");		
		headers.add("Authorization", "Bearer "+ auth);
		headers.setContentType(MediaType.APPLICATION_JSON);

		String data= shipstationApiKey + ":" + shipstationApiSecret;
	    String encodedStr = Base64.getEncoder()
	            .encodeToString(data.getBytes(StandardCharsets.UTF_8.name()));
    
		shipHeaders.add("Authorization", "Basic " + encodedStr );
		shipHeaders.setContentType(MediaType.APPLICATION_JSON);

	}
	
	@Override
	public void createCustomer(CustomerOrder customerOrder) throws Exception {
		
		log.debug("createCustomer: " + customerOrder.toString());

		JSONObject addressObject = new JSONObject();
		addressObject.put("address_line_1", customerOrder.getAddressLine1());
		addressObject.put("address_line_2", customerOrder.getAddressLine2());
		addressObject.put("address_line_3", customerOrder.getAddressLine3());
		addressObject.put("administrative_district_level_1", customerOrder.getCity());
		addressObject.put("administrative_district_level_2", customerOrder.getState());
		// addressObject.put("administrative_district_level_3", "level3");
		addressObject.put("country", "US");
		addressObject.put("postal_code", customerOrder.getPostalCode());

		JSONObject requestBody = new JSONObject();
		//requestBody.put("company_name", customerOrder.getCompanyName());
		requestBody.put("email_address", customerOrder.getEmailAddress());
		requestBody.put("family_name", customerOrder.getFamilyName());
		requestBody.put("given_name", customerOrder.getGivenName());
		//requestBody.put("nickname", customerOrder.getNickname());
		requestBody.put("idempotency_key", UUID.randomUUID().toString());
		requestBody.put("phone_number", customerOrder.getPhoneNumber());
		requestBody.put("address", addressObject);

		HttpEntity<String> request = new HttpEntity<String>(requestBody.toString(), headers);
		log.debug("request: " + request.getBody());

		String response = restTemplate.postForObject(url_base+"/customers", request, String.class);
		log.debug("response: " + response);

		JSONObject responseCustomer = new JSONObject(response);
		String id = responseCustomer.getJSONObject("customer").getString("id");
		log.debug("responseCustomer id: " + id);
		customerOrder.setSqCustomerId(id);
	}

	@Override
	public void createOrder(CustomerOrder customerOrder) throws Exception {
		log.debug("createOrder: " + customerOrder.toString());

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

		HttpEntity<String> request = new HttpEntity<String>(requestBody.toString(), headers);
		log.debug("request: " + request.getBody());
		
		String response = restTemplate.postForObject(url_base+"/orders", request, String.class);

		JSONObject responseOrder = new JSONObject(response);
		String id = responseOrder.getJSONObject("order").getString("id");
		log.debug(" responseOrder id: " + id);
		customerOrder.setSqOrderId(id);
	}

	@Override
	public void createInvoice(CustomerOrder customerOrder) {
		log.debug("createInvoice: " + customerOrder.toString());
		
		JSONObject primaryRecipient = new JSONObject();
		primaryRecipient.put("customer_id", customerOrder.getSqCustomerId());
		
		JSONObject acceptedPaymentMethods = new JSONObject();
		acceptedPaymentMethods.put("bank_account", false); //TODO string or boolean?
		acceptedPaymentMethods.put("card", true); //TODO string or boolean?
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

		
		//Date operations
		
		Instant scheduledInstant = Instant.now().plus(1, ChronoUnit.MINUTES);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone( ZoneId.of("UTC"));
		String scheduledAt = formatter.format(scheduledInstant);

		log.debug("scheduled_at: " + scheduledAt);
		invoiceObject.put("scheduled_at", scheduledAt);
		
		String dueDate = scheduledInstant.toString().substring(0,10);
		log.debug("due_date: " + dueDate);
		paymentRequest.put("due_date", dueDate);
		
		invoiceObject.put("primary_recipient",primaryRecipient);
		
		JSONObject requestBody = new JSONObject();
		requestBody.put("idempotency_key", UUID.randomUUID().toString());
		requestBody.put("invoice", invoiceObject);
	
		HttpEntity<String> request = new HttpEntity<String>(requestBody.toString(), headers);
		log.debug("request: " + request.getBody());
		
		String response = restTemplate.postForObject(url_base+"/invoices", request, String.class);
		log.debug("response: \n" + response);

		JSONObject responseInvoice = new JSONObject(response);
		JSONObject invoice = responseInvoice.getJSONObject("invoice");
		String id = invoice.getString("id");
		int version = invoice.getInt("version");
		log.debug("invoice id: " + id);
		log.debug("invoice version: " + version);
		customerOrder.setSqInvoiceId(id);
		customerOrder.setSqInvoiceVersion(version);
	}
	
	@Override
	public void publishInvoice(CustomerOrder customerOrder) {
		log.debug("running publishInvoice: " + customerOrder);
		
		JSONObject requestBody = new JSONObject();
		requestBody.put("idempotency_key", UUID.randomUUID().toString());
		requestBody.put("version", customerOrder.getSqInvoiceVersion().intValue());
		
		HttpEntity<String> request = new HttpEntity<String>(requestBody.toString(), headers);
		log.debug("request: " + request.getBody());
		String publishURL = String.format(url_base+"/invoices/%s/publish", customerOrder.getSqInvoiceId());
		String response = restTemplate.postForObject(publishURL, request, String.class);
		log.debug("response publish invoice: " + response);
		
		// convert the response String to a json object
		JSONObject responseInvoice = new JSONObject(response);
		log.debug("responseInvoice: " + responseInvoice);
		
		// from the response object get the invoice
		JSONObject invoiceObject = responseInvoice.getJSONObject("invoice");
		log.debug("invoiceObject: " + invoiceObject);
		
//		String publicURL = invoiceObject.getString("public_url");
//		log.debug("publicURL: " + publicURL);

		//work-around
		String publicURL = "https://squareup.com/pay-invoice/" + invoiceObject.getString("id");
		log.debug("publicURL for setPaymentURL: " + publicURL);
		
		customerOrder.setPaymentURL(publicURL);
	}

	@Override
	public void sendSms(CustomerOrder customerOrder) throws InvalidPhoneNumberException {
		log.debug("running sendSms: " + customerOrder);

		String str = customerOrder.getPhoneNumber();

		StringBuilder stringBuilder = new StringBuilder();

		for (char dig : str.toCharArray()) {
		    if (Character.isDigit(dig)) 
		    {
		    	stringBuilder.append(dig);
		    }
		}
		
		String tmp = stringBuilder.toString();
		
		if (tmp.length() == 10 ) {
			stringBuilder.insert(0, "+1");
		} else if (tmp.length() == 11 && tmp.startsWith("1")) {
			stringBuilder.insert(0, '+');
		} else if (tmp.length() != 12 || !tmp.startsWith("+1")) {
			log.info("invalid phone number " + customerOrder.getPhoneNumber() );
			throw new InvalidPhoneNumberException(customerOrder.getPhoneNumber());
		}
		
		String sendTo = stringBuilder.toString();
		log.info("sendTo: " + sendTo);
				
		String messageContent = String.format(
				"Thank You for your Order on Delta8gummies.com. Use this link to Complete Your Purchase: %s " +
				"**Be Advised It takes up to 2 minutes before you can Complete Your Payment**" , 
				customerOrder.getPaymentURL());
		
		Twilio.init(twilioSid, twilioAuthToken);
		
		Message message = Message.creator(
				new PhoneNumber(sendTo), 
				new PhoneNumber(twilioPhoneNumber), 
				messageContent)
				.create();
		
		log.info("twilio message sid: " + message.getSid() );
	}
	
	/****************************************************************** 
	 * SHIP STATION METHODS
	 *******************************************************************/
	
	@Override
	public void postShipStationOrder(CustomerOrder customerOrder) {
		log.debug("shipHeaders: " + shipHeaders.toString());

		String publishURL = String.format("https://ssapi.shipstation.com/orders/createorder");
		log.debug("publishURL: " + publishURL);
		
		JSONObject body = createShipStationOrderBody(customerOrder);
		log.debug("request body: " + body);

		HttpEntity<String> requestEntity = new HttpEntity<String>(body.toString(), shipHeaders);
		log.debug("requestEntity: " + requestEntity);
		
		String response = restTemplate.postForObject(publishURL, requestEntity, String.class);
		
		//String response = restTemplate.postForObject(publishURL, request, String.class);
		log.debug("response get: " + response);
		
		// convert the response String to a JSON object
		JSONObject responseShipStation = new JSONObject(response);
		log.debug("responseShipStation: " + responseShipStation);
	}
	
	@Override
	public void getShipStationOrder(String orderNumber) {

		// example
		//String publishURL = String.format("https://ssapi.shipstation.com/orders?orderNumber=D8G-1198");
		String publishURL = String.format("https://ssapi.shipstation.com/orders?orderNumber="+orderNumber);
		//String publishURL = String.format("https://ssapi.shipstation.com/orders");
		log.debug("publishURL: " + publishURL);

		HttpEntity<Void> requestEntity = new HttpEntity<>(shipHeaders);

		ResponseEntity<String> response = restTemplate.exchange(
				publishURL, HttpMethod.GET, requestEntity, String.class);
		//log.debug("response get: " + response);
		
		// convert the response String to a JSON object
		JSONObject responseShipStation = new JSONObject(response);
		log.debug("responseShipStation: " + responseShipStation);
		log.debug("response body: " + responseShipStation.get("body"));
		
	}
	
	@Override
	public JSONObject getShipStationBatch(String resource_url) {
		log.debug("resource_url: " + resource_url);

		HttpEntity<Void> requestEntity = new HttpEntity<>(shipHeaders);

		ResponseEntity<String> response = restTemplate.exchange(
				resource_url, HttpMethod.GET, requestEntity, String.class);
		// log.debug("response get: " + response);
		  
		// convert the response String to a JSON object 
		JSONObject responseShipStation = new JSONObject(response); 
		log.debug("responseShipStation: " + responseShipStation); 
		log.debug("tpAa response body: " + responseShipStation.get("body"));
				
//		String body = "{\"status\":\"success\",\"data\":{\"id\":1,\"employee_name\":\"Tiger Nixon\",\"employee_salary\":320800,\"employee_age\":61,\"profile_image\":\"\"},\"message\":\"Successfully! Record has been fetched.\"}";

		//JSONObject jsonObjectBody = responseShipStation.getString("body"));
		String bdy = responseShipStation.getString("body");
		JSONObject jsonObjectBody = new JSONObject(bdy);
		JSONObject data = jsonObjectBody.getJSONObject("data");
		log.debug("data: " + data);

		return data;
	}
	
	public void processBatch(Object object) {
		log.debug("entering processBatch function", object.toString());
	
		
		/* ******************** AfterShip fields
		 * 	courier,
		 * 	tracking_number,
		 *  email,
		 *  sms,
		 *  order_id,
		 *  title,
		 *  order_path,
		 *  customer_name,
		 *  origin_country,
		 *  destination_country,
		 *  custom_1,
		 *  custom_2,
		 *  tracking_postal_code,
		 *  tracking_ship_date,
		 *  tracking_account_number,
		 *  tracking_key,
		 *  tracking_destination_country 
		 */
		

		
		// example
		//String publishURL = String.format("https://ssapi.shipstation.com/fulfillments");
		//String publishURL = String.format("https://ssapi.shipstation.com/orders?orderNumber=D8G-1198");
		//String publishURL = String.format("https://ssapi.shipstation.com/orders?orderNumber="+orderNumber);
//		log.debug("publishURL: " + publishURL);

//		HttpEntity<Void> requestEntity = new HttpEntity<>(shipHeaders);

//		ResponseEntity<String> response = restTemplate.exchange(
//				publishURL, HttpMethod.GET, requestEntity, String.class);
		//log.debug("response get: " + response);
		
		// convert the response String to a JSON object
//		JSONObject responseShipStation = new JSONObject(response);
//		log.debug("responseShipStation: " + responseShipStation);
	}		

	/***************************************************************** 
	 * Private Methods 
	 **/
	
	private JSONObject buildAddress(CustomerOrder customerOrder) {
		JSONObject address = new JSONObject();
		address.put("name", customerOrder.getFullName());
		address.put("company", customerOrder.getCompanyName());
		address.put("street1", customerOrder.getAddressLine1());
		address.put("street2", customerOrder.getAddressLine2());
		address.put("street3", customerOrder.getAddressLine3());
		address.put("city",  customerOrder.getCity());
		address.put("state", customerOrder.getState());
		address.put("postalCode", customerOrder.getPostalCode());
		address.put("country", "US");  
		address.put("phone",  customerOrder.getPhoneNumber());
		return address;
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
//		requestBody.put("taxAmount", 
		
		/* Extract items from the customer order and add the relevant fields to 
		 * the ship station order 
		 */
		JSONArray items = customerOrder.getItems();
		ArrayList<JSONObject> shipItems = new ArrayList<>();
		
		for (int i = 0; i < items.length(); i++) {
			
			JSONObject orderItem = items.getJSONObject(i);
			
			JSONObject shipItem =  new JSONObject();
//TODO confirm customer order price is actually unit price			
			if (!orderItem.getString("name").equals("Recurring plan")) {
				shipItem.put("unitPrice", orderItem.getInt("price"));			
				shipItem.put("quantity", orderItem.getInt("quantity"));
				shipItem.put("name", orderItem.getString("name"));
				shipItems.add(shipItem);
			}	

		}
		requestBody.put("items", shipItems); 
		log.debug("requestBody original:" + requestBody);
		return requestBody;
	}
	
}
