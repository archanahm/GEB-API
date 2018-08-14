package com.hbg.dto

import java.io.Serializable


class PaymentStatusDetails implements Serializable{

	String paymentStatus
	String bookinStatus
	int amount

	/**
	 * @return the paymentStatus
	 */
	public String getPaymentStatus() {
		return paymentStatus
	}
	/**
	 *
	 * @return
	 */
	public String getBookingStatus(){
		return bookinStatus
	}
	/**
	 * @param paymentStatus the paymentStatus to set
	 */
	public void setPaymentStatus(String paymentStatus) {
		println paymentStatus
		this.paymentStatus = paymentStatus;
		println this.paymentStatus
	}
	/**
	 *
	 * @param bookingStatus to set the bookingStatus
	 */
	public void setBookingStatus(String bookingStatus){
		println bookingStatus
		this.bookingStatus = bookingStatus
		println this.bookingStatus

	}
	/**
	 * @return the amount
	 */
	public Integer getAmount() {
		return amount;
	}
	/**
	 * @param amount the amount to set
	 */
	public void setAmount(int amount) {
		this.amount = amount;
	}
	@Override
	public String toString() {
		return "PaymentStatusDetails [paymentStatus=" + paymentStatus + ", amount=" + amount + "]";
	}
}

