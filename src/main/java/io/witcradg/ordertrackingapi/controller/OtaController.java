package io.witcradg.ordertrackingapi.controller;

import java.util.Enumeration;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.witcradg.ordertrackingapi.entity.CustomerOrder;
//import io.witcradg.ordertrackingapi.persistence.IOrderDetailPersistenceService;
import io.witcradg.ordertrackingapi.persistence.IOrderHistoryPersistenceService;
import io.witcradg.ordertrackingapi.persistence.IOrderItemsPersistenceService;
import io.witcradg.ordertrackingapi.repository.OrderDetailRepository;
import io.witcradg.ordertrackingapi.service.ICommunicatorService;
import io.witcradg.ordertrackingapi.service.IEmailSenderService;
import io.witcradg.ordertrackingapi.service.IShipStationService;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
public class OtaController {

	// feature toggles
	@Value("${controller.useSquareApi}")
	private boolean useSquareApi;
	
	@Value("${controller.useShipStationApi}")
	private boolean useShipStationApi;

	@Autowired
	ICommunicatorService communicatorService;
	
	@Autowired
	IShipStationService shipStationService;

	@Autowired
	IEmailSenderService emailSenderService;

	@Autowired
	IOrderHistoryPersistenceService orderHistoryPersistence;

	@Autowired
	OrderDetailRepository orderDetailRepository;

	@Autowired
	IOrderItemsPersistenceService orderItemsPersistence;

	boolean validOrder = false;

	// log the most recent step on processing errors
	String stepMessage = "";
	String orderNumber = "";

	@PostMapping("/ssa")
	public ResponseEntity<HttpStatus> createNewOrder(@RequestBody String rawJson) {

		try {
			stepMessage = "Parsing raw json";
			JSONObject jsonObject = new JSONObject(rawJson);

			stepMessage = "Checking for order.completed";
			if ("order.completed".equals(jsonObject.getString("eventName"))) {
				validOrder = true;
				stepMessage = "Is valid order. Getting order number";

				JSONObject content = jsonObject.getJSONObject("content");
				stepMessage = content.getString("invoiceNumber");
				orderNumber = stepMessage;
				CustomerOrder customerOrder = new CustomerOrder(content);

				if (useSquareApi) {
					communicatorService.createCustomer(customerOrder);
					communicatorService.createOrder(customerOrder);
					communicatorService.createInvoice(customerOrder);
					communicatorService.publishInvoice(customerOrder);
					orderHistoryPersistence.write(orderNumber, "Square order created");
					communicatorService.sendSms(customerOrder);
					orderHistoryPersistence.write(orderNumber, "SMS sent");
				}
				if (useShipStationApi) {
					//shipStationService.runShipStationUtility();
					shipStationService.postShipStationOrder(customerOrder);
					orderHistoryPersistence.write(orderNumber, "ShipStation order posted");
				}

				orderDetailRepository.save(customerOrder.getOrderDetail());
				orderItemsPersistence.write(orderNumber, customerOrder.getItems().toString());

			} else if (jsonObject.has("eventName")) {
				log.info("A JSON object was recieved with an eventName of: " + jsonObject.getString("eventName"));
			}

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			log.error(e.getMessage());

			try {
				if (validOrder) {
					emailSenderService.sendEmail("witcradg@gmail.com", "OTA system error",
							String.format("Order Number: %s \n %s", orderNumber, rawJson));
					orderHistoryPersistence.write(orderNumber, "ERROR");
				}
			} catch (Exception ex) {
				log.error("Exception thrown in OTA Exception clause");
				log.error(ex.getMessage());
			}

			log.error("ERROR==================================\n");
			log.error(stepMessage);
			log.error(e.getMessage());
			log.error("rawJson: " + rawJson);
			log.error("---------------------------------------\n");
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/shipped")
	public ResponseEntity<HttpStatus> onShipped(@RequestBody String rawJson) {
		log.info("onShipped: " + rawJson);
		JSONObject jsonObject = new JSONObject(rawJson);
		if ("SHIP_NOTIFY".equals(jsonObject.getString("resource_type"))) {
			try {
				JSONObject shipstationBatch = shipStationService
						.getShipStationBatch(jsonObject.getString("resource_url"));
				shipStationService.processShipStationBatch(shipstationBatch);
			} catch (Exception e) {
				log.error(e.getMessage());
				log.error("rawJson: " + rawJson);
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "**")
	public ResponseEntity<HttpStatus> error(HttpServletRequest request) {
		dumpRequest(request);

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	private void dumpRequest(HttpServletRequest request) {
		Exception exception = (Exception) request.getAttribute("javax.servlet.error.exception");
		if (exception != null) {
			log.error("    exception: " + exception.getMessage());
		}
		log.error("    Request URL: " + request.getRequestURL());

		StringBuffer headers = new StringBuffer();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = (String) headerNames.nextElement();
			String value = request.getHeader(key);
			headers.append(key + ":" + value + ",");
		}
		log.error("headers" + headers);

		try {
			log.error(request.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
		} catch (Exception e) {
			log.error("Exception thrown in error method when trying to read request: " + e.getMessage());
		}
	}
}
