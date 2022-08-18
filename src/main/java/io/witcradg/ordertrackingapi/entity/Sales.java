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

@Entity
@Table(name = "sales")
public class Sales {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
    @CreationTimestamp
    @Column (name = "date_sold")
    private Date dateSold = new Date();
    
    @Column(name = "order_number")
    private String orderNumber;
	
	@Column(name = "sku")
	private String sku;

	@Column(name = "product_name")
	private String productName;
	
	@Column(name = "unit_price")
	private BigDecimal unitPrice;	
	
	@Column(name = "total_price")
	private BigDecimal totalPrice;		
	
	@Column(name = "quantity_sold")
	private Integer quantitySold;
	
	public Sales() {
		super();
	}

	public Sales(String orderNumber, String sku, String productName, Integer quantitySold, BigDecimal unitPrice, BigDecimal totalPrice) {
		super();
		this.orderNumber = orderNumber;
		this.sku = sku;
		this.productName = productName;
		this.quantitySold = quantitySold;
		this.unitPrice = unitPrice;
		this.totalPrice = totalPrice;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getDateSold() {
		return dateSold;
	}

	public void setDateSold(Date dateSold) {
		this.dateSold = dateSold;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Integer getQuantitySold() {
		return quantitySold;
	}

	public void setQuantitySold(Integer quantitySold) {
		this.quantitySold = quantitySold;
	}
}
