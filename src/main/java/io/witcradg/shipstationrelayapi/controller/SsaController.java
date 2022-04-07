package io.witcradg.shipstationrelayapi.controller;

import java.util.Enumeration;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.witcradg.shipstationrelayapi.entity.CustomerOrder;
import io.witcradg.shipstationrelayapi.service.ICommunicatorService;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
public class SsaController {

	boolean useSquareApi = false; // disable unused code without deleting it
	boolean useShipStationApi = true;

	@Autowired
	ICommunicatorService communicatorService;

	@PostMapping("/ssa")
	public ResponseEntity<HttpStatus> createNewOrder(@RequestBody String rawJson) {

		try {
			JSONObject jsonObject = new JSONObject(rawJson);
			if ("order.completed".equals(jsonObject.getString("eventName"))) {
				JSONObject content = jsonObject.getJSONObject("content");
				CustomerOrder customerOrder = new CustomerOrder(content);
				if (useSquareApi) {
					communicatorService.createCustomer(customerOrder);
					communicatorService.createOrder(customerOrder);
					communicatorService.createInvoice(customerOrder);
					communicatorService.publishInvoice(customerOrder);
					communicatorService.sendSms(customerOrder);
				}
				if (useShipStationApi) {
					communicatorService.postShipStationOrder(customerOrder);
					// communicatorService.getShipStationFulfillment("D8G-1198");
					//communicatorService.getShipStationOrder("TEST-0001");
				}
			}
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			log.error(e.getMessage());
			log.error("rawJson: " + rawJson);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/shipped")
	public ResponseEntity<HttpStatus> onShipped(@RequestBody String rawJson) {  
		log.info("testpoint 1", rawJson);

		JSONObject jsonObject = new JSONObject(rawJson);
		if ("SHIP_NOTIFY".equals(jsonObject.getString("resource_type"))) {
			try {
				
				//JSONObject shipstationData = communicatorService.getShipStationBatch(jsonObject.getString("resource_url"));
				//TODO Remove MOCK 
				JSONObject shipstationBatch = new JSONObject("{\"shipments\":[{\"shipmentId\":27394493,\"orderId\":77777972,\"orderKey\":\"eb2ca4e04766406c9693c5292f3d319c\",\"userId\":\"63ec9bfd-987e-477c-ba5d-72988adb5bcc\",\"customerEmail\":\"monteagle@hotmail.com\",\"orderNumber\":\"D8G-2482\",\"createDate\":\"2022-04-05T14:17:54.6800000\",\"shipDate\":\"2022-04-05\",\"shipmentCost\":11.35,\"insuranceCost\":0.00,\"trackingNumber\":\"9410811202508168181851\",\"isReturnLabel\":false,\"batchNumber\":null,\"carrierCode\":\"stamps_com\",\"serviceCode\":\"usps_priority_mail\",\"packageCode\":\"flat_rate_padded_envelope\",\"confirmation\":\"signature\",\"warehouseId\":39270,\"voided\":false,\"voidDate\":null,\"marketplaceNotified\":false,\"notifyErrorMessage\":null,\"shipTo\":{\"name\":\"Angela Sampley \",\"company\":null,\"street1\":\"343 ARMORY RD\",\"street2\":\"\",\"street3\":null,\"city\":\"MONTEAGLE\",\"state\":\"TN\",\"postalCode\":\"37356-7606\",\"country\":\"US\",\"phone\":\"\",\"residential\":null,\"addressVerified\":null},\"weight\":{\"value\":4.00,\"units\":\"ounces\",\"WeightUnits\":1},\"dimensions\":null,\"insuranceOptions\":{\"provider\":null,\"insureShipment\":false,\"insuredValue\":0.0},\"advancedOptions\":{\"billToParty\":\"4\",\"billToAccount\":null,\"billToPostalCode\":null,\"billToCountryCode\":null,\"storeId\":54658},\"shipmentItems\":null,\"labelData\":null,\"formData\":null}],\"total\":1,\"page\":1,\"pages\":1}\n");

				communicatorService.processShipStationBatch(shipstationBatch);
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
