package io.witcradg.shipstationrelayapi.service;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
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
	
	//TODO ****************************************************************** needs to be added to the application.properties
	private String aftershipApiKey = "foo";
	private String aftershipApiSecret = "bar";
	
		
	private RestTemplate restTemplate = new RestTemplate();
	private HttpHeaders headers = new HttpHeaders();
	private HttpHeaders shipHeaders = new HttpHeaders();
	private HttpHeaders afterShipHeaders = new HttpHeaders();



	@PostConstruct
	private void loadHeaders() throws Exception  {
		headers.add("Square-Version", "2021-04-21");		
		headers.add("Authorization", "Bearer "+ auth);
		headers.setContentType(MediaType.APPLICATION_JSON);

		String shipStationData= shipstationApiKey + ":" + shipstationApiSecret;
	    String shipStationDataEncodedStr = Base64.getEncoder()
	            .encodeToString(shipStationData.getBytes(StandardCharsets.UTF_8.name()));

	    shipHeaders.add("Authorization", "Basic " + shipStationDataEncodedStr );
		shipHeaders.setContentType(MediaType.APPLICATION_JSON);
	    
		String aftershipData= aftershipApiKey + ":" + aftershipApiSecret;
	    String aftershipDataEncodedStr = Base64.getEncoder()
	            .encodeToString(aftershipData.getBytes(StandardCharsets.UTF_8.name()));
	    
	    afterShipHeaders.add("Authorization", "Basic " + aftershipDataEncodedStr );
	    afterShipHeaders.setContentType(MediaType.APPLICATION_JSON);
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
	
	
	/*
	 * https://developers.aftership.com/reference/quick-start
	 * https://developers.aftership.com/reference/post-trackings
	 * NOTE: THE ABOVE CONTAINS DETAILED BREAKDOWN 
	 * https://developers.aftership.com/reference/object-tracking
	 * 		Common Scenarios: Scenario 1.
	 * 
	 * curl --location --request POST 'https://api.aftership.com/v4/trackings' \
		--header 'aftership-api-key: your aftership api key' \
		--header 'Content-Type: application/json' \
		--data-raw '{
	    	"tracking": {
	        	"tracking_number": "9405511202575421535949",
	        	"slug": "usps"
	    	}
		}'
		
	 * use "usps" for carrier slug
	 */
	public void processBatch(JSONObject object) {
		log.debug("entering processBatch function" + object.toString());
	
		/*
		 * Assumptions 
		 * 	I'll need to extract and iterate over multiple records
		 *  Create a new empty AfterShip bundle (FORMAT TBD: JsonArray, CSV, etc.)
		 *  For Each Ship Station Record
		 * 		MAY need a lookup TBD
		 *    	Create a new AfterShip Record
		 *  	Map fields from the ShipStation record to a new AfterShip record
		 *  	insert AfterShip record into the AfterShip bundle
		 *  Create new POST (confirm a POST is used)
		 *  Call the POST
		 *  Handle the response
		 */
		
		// START LOOP
		// 		Do some magic here to do iteration over the "object" variable
		// 		and create one ore more JSONObjects (shipstationRecord) for each record in the ShipStation dataset
		
			JSONObject shipstationRecord = new JSONObject(); //TODO MOCK for raw JSON to be extracted from the dataset
//			log.debug("shipstationRecord: " + shipstationRecord.toString());

		
			JSONObject aftershipRecord = createAfterShipTrackingRecord(shipstationRecord);
			log.debug("aftershipRecord: " + aftershipRecord.toString());
			
			
			JSONObject requestBody = new JSONObject();
//			requestBody.put("idempotency_key", UUID.randomUUID().toString());
			requestBody.put("tracking", aftershipRecord);

			HttpEntity<String> request = new HttpEntity<String>(requestBody.toString(), headers);
			log.debug("request: " + request.getBody());
			
			String response = restTemplate.postForObject(url_base+"/orders", request, String.class);


		
		// END LOOP
		
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

private JSONObject createAfterShipTrackingRecord(JSONObject record) {
	JSONObject trackingPost = new JSONObject();
	
	/* FOR ShipStation batched records received "FROM"
	{
	shipments: [
		{
			shipmentId: 27394493,
			orderId: 77777972,
			orderKey: 'eb2ca4e04766406c9693c5292f3d319c',
			userId: '63ec9bfd-987e-477c-ba5d-72988adb5bcc',
			customerEmail: 'monteagle@hotmail.com',
			orderNumber: 'D8G-2482',
			createDate: '2022-04-05T14:17:54.6800000',
			shipDate: '2022-04-05',
			shipmentCost: 11.35,
			insuranceCost: 0.0,
			trackingNumber: '9410811202508168181851',
			isReturnLabel: false,
			batchNumber: null,
			carrierCode: 'stamps_com',
			serviceCode: 'usps_priority_mail',
			packageCode: 'flat_rate_padded_envelope',
			confirmation: 'signature',
			warehouseId: 39270,
			voided: false,
			voidDate: null,
			marketplaceNotified: false,
			notifyErrorMessage: null,
			shipTo: {
				name: 'Angela Sampley ',
				company: null,
				street1: '343 ARMORY RD',
				street2: '',
				street3: null,
				city: 'MONTEAGLE',
				state: 'TN',
				postalCode: '37356-7606',
				country: 'US',
				phone: '',
				residential: null,
				addressVerified: null
			},
			weight: { value: 4.0, units: 'ounces', WeightUnits: 1 },
			dimensions: null,
			insuranceOptions: { provider: null, insureShipment: false, insuredValue: 0.0 },
			advancedOptions: {
				billToParty: '4',
				billToAccount: null,
				billToPostalCode: null,
				billToCountryCode: null,
				storeId: 54658
			},
			shipmentItems: null,
			labelData: null,
			formData: null
		}
	],
	total: 1,
	page: 1,
	pages: 1
	}
	 */
	
	/* for AfterShip records "TO" (Note: there may be more fields wanted/needed. There are numerous other optional fields
	 * including "Custom".
	{
		"tracking": {
    		"slug": "dhl",
		    "tracking_number": "6123456789",
		    "title": "Title Name",
		    "smses": [
		        "+18555072509",
		        "+18555072501"
		    ],
		    "emails": [
		        "email@yourdomain.com",
		        "another_email@yourdomain.com"
		    ],
		    "order_id": "ID 1234",		order number? The order_id_path uses a url
		    "order_number": "1234",		
		    "order_id_path": "http://www.aftership.com/order_id=1234",
		    "custom_fields": {
		        "product_name": "iPhone Case",
		        "product_price": "USD19.99"
		    },
		    "language": "en",
		    "order_promised_delivery_date": "2019-05-20",
		    "delivery_type": "pickup_at_store",
		    "pickup_location": "Flagship Store",
		    "pickup_note": "Reach out to our staffs when you arrive our stores for shipment pickup"
		}
	}	
	 */
	

	JSONObject shipStationData = new JSONObject("{\"shipments\":[{\"shipmentId\":27394493,\"orderId\":77777972,\"orderKey\":\"eb2ca4e04766406c9693c5292f3d319c\",\"userId\":\"63ec9bfd-987e-477c-ba5d-72988adb5bcc\",\"customerEmail\":\"monteagle@hotmail.com\",\"orderNumber\":\"D8G-2482\",\"createDate\":\"2022-04-05T14:17:54.6800000\",\"shipDate\":\"2022-04-05\",\"shipmentCost\":11.35,\"insuranceCost\":0.00,\"trackingNumber\":\"9410811202508168181851\",\"isReturnLabel\":false,\"batchNumber\":null,\"carrierCode\":\"stamps_com\",\"serviceCode\":\"usps_priority_mail\",\"packageCode\":\"flat_rate_padded_envelope\",\"confirmation\":\"signature\",\"warehouseId\":39270,\"voided\":false,\"voidDate\":null,\"marketplaceNotified\":false,\"notifyErrorMessage\":null,\"shipTo\":{\"name\":\"Angela Sampley \",\"company\":null,\"street1\":\"343 ARMORY RD\",\"street2\":\"\",\"street3\":null,\"city\":\"MONTEAGLE\",\"state\":\"TN\",\"postalCode\":\"37356-7606\",\"country\":\"US\",\"phone\":\"\",\"residential\":null,\"addressVerified\":null},\"weight\":{\"value\":4.00,\"units\":\"ounces\",\"WeightUnits\":1},\"dimensions\":null,\"insuranceOptions\":{\"provider\":null,\"insureShipment\":false,\"insuredValue\":0.0},\"advancedOptions\":{\"billToParty\":\"4\",\"billToAccount\":null,\"billToPostalCode\":null,\"billToCountryCode\":null,\"storeId\":54658},\"shipmentItems\":null,\"labelData\":null,\"formData\":null}],\"total\":1,\"page\":1,\"pages\":1}\n");
	
	String total =  shipStationData.getString("total");
	String page =  shipStationData.getString("pages");
	String pages =  shipStationData.getString("pages");
	
	log.debug("total: " + total);
	log.debug("page: " + page);
	log.debug("pages: " + pages);

	JSONArray shipStationOrders = shipStationData.getJSONArray("shipments");
	
	JSONObject order = shipStationOrders.getJSONObject(0);
		
	String shipmentNbr = order.getString("shipmentId");
	String orderNbr = order.getString("orderNumber");
	String trackingNbr = order.getString("tracking_number");
	String recipient = order.getString("recipient");
	String shipDate = order.getString("ship_date");
	String packingSlipPrinted = "";		order.getString("packing_slip_printed");
	String labelCreated = "Created";			order.getString("label_created");
	String labelPrinted = "";			order.getString("label_printed");
	String marketplaceNotified = "Not Notified";	order.getString("marketplace_notified");
	String shipmentNotification = "Not Sent";	order.getString("shipment_notification");
	String deliveryNotification = "Failed";	order.getString("delivery_notification");

	//TODO NOTE: order_date, SMS and email values will require a lookup for each record.
	
	//TODO Is there a difference for AfterShip between USPS Priority Mail and USPS regular main? 
	//I'm guessing not since it doesn't seem to be offered as a carrier option in AfterShip, but I should check.
	
	trackingPost.put("slug", "usps"); // hard-coded for now since no other carriers are supported by delta8gummies.
	trackingPost.put("tracking_number", trackingNbr);

	String timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
	System.out.println(timeStamp);
	String title = "ShipStationRelay" + timeStamp;
	trackingPost.put("title", title);

	JSONArray smses = new JSONArray();
	trackingPost.put("smses", smses);
	
	JSONArray emails = new JSONArray();
	trackingPost.put("emails", emails);

	trackingPost.put("order_id", shipmentNbr);
	trackingPost.put("order_number", orderNbr);
//	trackingPost.put("order_id_path", ????); //This might be something that is only populated on a GET from AfterShip and not something we should populate 
// custom fields pending
	trackingPost.put("language","en");

	return trackingPost;
}
}
