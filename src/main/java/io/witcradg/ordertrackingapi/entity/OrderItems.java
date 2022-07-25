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
@Table(name = "order_items")
public class OrderItems {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
    @CreationTimestamp
    @Column (name = "create_date")
    private Date createDate = new Date();
	
	@Column(name = "order_number")
	private String orderNumber;
	
	@Column(name = "order_items")
	private String orderItems;
	
	public OrderItems() {}

	public OrderItems(long id, Date createDate, String orderNumber, String orderItems) {
		super();
		this.id = id;
		this.createDate = createDate;
		this.orderNumber = orderNumber;
		this.orderItems = orderItems;
	}

	public OrderItems(String orderNumber, String orderItems) {
		super();
		this.orderNumber = orderNumber;
		this.orderItems = orderItems;
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

	public String getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(String orderItems) {
		this.orderItems = orderItems;
	}
}
