package io.witcradg.shipstationrelayapi.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Setter
@Getter
@ToString
public class CustomerOrder {

	public CustomerOrder(JSONObject content) throws Exception {

		log.debug("Customer constructor:" + content.getJSONObject("user"));

		this.setEmailAddress(content.getJSONObject("user").getString("email"));
		this.setScInvoiceNumber(content.getString("invoiceNumber"));
		this.setScInvoiceTotal((int) (content.getDouble("finalGrandTotal") * 100));
		this.setScOrderWeight(Integer.toString(content.getInt("totalWeight")));
		this.setScOrderDate(content.getString("completionDate"));

		JSONObject address = content.getJSONObject("shippingAddress");

		this.setItems(content.getJSONArray("items"));

		// this.setCompanyName(address.getString("company"));
		this.setFullName(address.getString("fullName"));
		this.setFamilyName(address.getString("name"));
		this.setGivenName(address.isNull("firstName") ? "" : address.getString("firstName"));
		this.setAddressLine1(address.getString("address1"));
		this.setAddressLine2(address.getString("address2"));
		this.setCity(address.getString("city"));
		this.setState(address.getString("province"));
		this.setPostalCode(address.getString("postalCode"));
		// phone number field moved from shipping address to customFields
		JSONObject phoneNumberField = content.getJSONArray("customFields").getJSONObject(0);
		this.setPhoneNumber(phoneNumberField.getString("value"));

		this.setShippingTotal(content.getInt("shippingFees"));
	}

	private String companyName;
	private String emailAddress;
	private String fullName;
	private String familyName;
	private String givenName;
	private String nickname;
	private String phoneNumber;
	private String addressLine1;
	private String addressLine2;
	private String addressLine3;
	private String city;
	private String state;
	private String postalCode;
	private Integer shippingTotal;

	private JSONArray items;

	private String scInvoiceNumber;// sc - snip cart
	private Integer scInvoiceTotal;

	private String scOrderWeight;
	private String scOrderDate;

	private String sqCustomerId;// sq - square
	private String sqOrderId;
	private String sqInvoiceId;
	private Integer sqInvoiceVersion;
	private String paymentURL;
}
