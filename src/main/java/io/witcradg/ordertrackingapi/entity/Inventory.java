package io.witcradg.ordertrackingapi.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "inventory")
public class Inventory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
    @CreationTimestamp
    @Column (name = "last_updated")
    private Date lastUpdated = new Date();
    
	@Column(name = "sku")
	private String sku;

	@Column(name = "product_name")
	private String productName;
	
	@Column(name = "on_hand")
	private Integer onHand;
	
	public Inventory() {
		super();
	}

	public Inventory(String sku, String productName, Integer onHand) {
		super();
		this.sku = sku;
		this.productName = productName;
		this.onHand = onHand;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
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

	public Integer getOnHand() {
		return onHand;
	}

	public void setOnHand(Integer onHand) {
		this.onHand = onHand;
	}

}
