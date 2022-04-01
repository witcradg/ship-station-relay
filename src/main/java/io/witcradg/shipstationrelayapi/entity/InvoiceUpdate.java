package io.witcradg.shipstationrelayapi.entity;

import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Setter
@Getter
@ToString
public class InvoiceUpdate {

	public InvoiceUpdate(JSONObject invoice) throws Exception {

		log.debug("InvoiceUpdate constructor:", invoice.toString());

		this.setId(invoice.getString("id"));
		this.setVersion(invoice.getInt("version"));
		this.setLocationId(invoice.getString("location_id"));
		this.setOrderId(invoice.getString("order_id"));
		this.setInvoiceNumber(invoice.getString("invoice_number"));
		this.setScheduledAt(invoice.getString("scheduled_at"));
		this.setStatus(invoice.getString("status"));
		if (invoice.has("public_url")) {
			log.info("************************************************** " + invoice.get("public_url"));
		}
		this.setCreatedAt(invoice.getString("created_at"));
		this.setUpdatedAt(invoice.getString("updated_at"));
		this.setDeliveryMethod(invoice.getString("delivery_method"));

		JSONObject recipient = invoice.getJSONObject("primary_recipient");

		this.setCustomerId(recipient.getString("customer_id"));
		this.setGivenName(recipient.getString("given_name"));
		this.setFamilyName(recipient.getString("family_name"));
		this.setEmailAddress(recipient.getString("email_address"));
		this.setPhoneNumber(recipient.getString("phone_number"));
	}

	private String id;
	private Integer version;
	private String locationId;
	private String orderId;
	private String invoiceNumber;
	private String scheduledAt;
	private String status;
	private String createdAt;
	private String updatedAt;
	private String primaryRecipient;
	private String customerId;
	private String givenName;
	private String familyName;
	private String emailAddress;
	private String phoneNumber;
	private String deliveryMethod;

}
