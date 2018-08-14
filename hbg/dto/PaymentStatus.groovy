package com.hbg.dto

import java.io.Serializable
import java.util.List

import com.hbg.dto.PaymentStatusDetails

class PaymentStatus implements Serializable{

	private String paymentStatus;
	private String grossamount;
	private String netamount;
	private int rowcount;
	public String getPaymentStatus() {
		return paymentStatus;
	}
	public void setPaymentStatus(paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
	public String getGrossamount() {
		return grossamount;
	}
	public void setGrossamount(String grossamount) {
		this.grossamount = grossamount;
	}
	public String getNetamount() {
		return netamount;
	}
	public void setNetamount(String netamount) {
		this.netamount = netamount;
	}
	public int getRowcount() {
		return rowcount;
	}
	public void setRowcount(int rowcount) {
		this.rowcount = rowcount;
	}
	@Override
	public String toString() {
		return "PaymentStatus [paymentStatus =" + paymentStatus + ", grossAmount=" + grossamount + ", netAmount=" + netamount+ "]";
	}
}

