
package com.hbg.dto

/**
 * @author archana
 *
 */
class BookingStatus implements Serializable{
	
	private String bookingstatus;
	private String grossamount;
	private String netamount;
	private int rowcount;
	public String getBookingstatus() {
		return bookingstatus;
	}
	public void setBookingstatus(String bookingstatus) {
		this.bookingstatus = bookingstatus;
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
		return "BookingStatus [bookingstatus =" + bookingstatus + ", grossAmount=" + grossamount + ", netAmount=" + netamount+ "]";
	} 
}
