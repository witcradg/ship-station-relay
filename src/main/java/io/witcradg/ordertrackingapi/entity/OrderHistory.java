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
@Table(name = "order_history")
public class OrderHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
    @CreationTimestamp
    @Column (name = "create_date")
    private Date createDate = new Date();
	
	@Column(name = "order_number")
	private String orderNumber;

	@Column(name = "order_status")
	private String orderStatus;
	
	public OrderHistory() {
		super();
	}
	
	public OrderHistory(String orderNumber, String orderStatus) {
		super();
		this.orderNumber = orderNumber;
		this.orderStatus = orderStatus;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public String getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}
}
