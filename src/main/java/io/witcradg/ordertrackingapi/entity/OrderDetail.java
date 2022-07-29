package io.witcradg.ordertrackingapi.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.json.JSONObject;

@Entity
@Table(name = "order_detail")
public class OrderDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@CreationTimestamp
	@Column(name = "create_date")
	private Date createDate = new Date();

	@Column(name = "order_number")
	private String orderNumber;

	@Column(name = "items_total")
	private BigDecimal itemsTotal;
	
	@Column(name = "saved_amount")
	private BigDecimal savedAmount;
	
	@Column(name = "subtotal")
	private BigDecimal subtotal;
	
	@Column(name = "shipping_fees")
	private BigDecimal shippingFees;
	
	@Column(name = "taxes_total")
	private BigDecimal taxesTotal;
	
	@Column(name = "grand_total")
	private BigDecimal grandTotal;
	
	@Column(name = "base_total")
	private BigDecimal baseTotal;

	public OrderDetail() {}
	
	public OrderDetail(JSONObject content) {
		this.orderNumber = content.getString("invoiceNumber");
		this.itemsTotal = content.getBigDecimal("itemsTotal");
		this.savedAmount = content.getBigDecimal("savedAmount");
		this.subtotal = content.getBigDecimal("subtotal");
		this.shippingFees = content.getBigDecimal("shippingFees");
		this.taxesTotal = content.getBigDecimal("taxesTotal");
		this.grandTotal = content.getBigDecimal("grandTotal");
		this.baseTotal = content.getBigDecimal("baseTotal");
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public BigDecimal getItemsTotal() {
		return itemsTotal;
	}

	public void setItemsTotal(BigDecimal itemsTotal) {
		this.itemsTotal = itemsTotal;
	}

	public BigDecimal getSavedAmount() {
		return savedAmount;
	}

	public void setSavedAmount(BigDecimal savedAmount) {
		this.savedAmount = savedAmount;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public void setSubtotal(BigDecimal subtotal) {
		this.subtotal = subtotal;
	}

	public BigDecimal getShippingFees() {
		return shippingFees;
	}

	public void setShippingFees(BigDecimal shippingFees) {
		this.shippingFees = shippingFees;
	}

	public BigDecimal getTaxesTotal() {
		return taxesTotal;
	}

	public void setTaxesTotal(BigDecimal taxesTotal) {
		this.taxesTotal = taxesTotal;
	}

	public BigDecimal getGrandTotal() {
		return grandTotal;
	}

	public void setGrandTotal(BigDecimal grandTotal) {
		this.grandTotal = grandTotal;
	}

	public BigDecimal getBaseTotal() {
		return baseTotal;
	}

	public void setBaseTotal(BigDecimal baseTotal) {
		this.baseTotal = baseTotal;
	}

}
