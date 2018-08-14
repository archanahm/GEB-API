package com.hbg.util


import java.sql.*

import com.hbg.dto.PaymentStatus
import com.hbg.dto.BookingStatus
import com.hbg.dto.PaymentStatusDetails

public class AS400DatabaseUtil {

	
	
	private static Connection connection = null;
	private static final int retryIntervalSec = 10 // seconds
	private static final int retryCount = 20 // seconds


	def static main(def args)
	{
		PaymentStatus status= getPaymentStatusDetails("925780","028")
		println "status"+status.getPaymentStatus()
	}
	private GetConnection() {
	}
	static Connection getAs400Connection() {
		try {
			if (connection == null) {
				Class.forName("com.ibm.as400.access.AS400JDBCDriver");
				connection = DriverManager.getConnection("jdbc:as400://londev;naming=sql;errors=full;prompt=false;libraries=RT012DTA;dateformat=iso;time format=iso", "webrev", "webrev");
			
				//Class.forName("com.ibm.as400.access.AS400JDBCDriver");
				//connection = DriverManager.getConnection("jdbc:as400://londev;naming=sql;errors=full;prompt=false;libraries=RT021DTA;dateformat=iso;time format=iso", "webrev", "webrev");
				
				
				}
			return connection;
		}catch(Exception exception) {
			exception.printStackTrace()
		}
		return connection
	}

	def PaymentStatus  getPaymentStatusDetails(String bookingReferenceId, String siteId) {
		Connection connection = AS400DatabaseUtil.getAs400Connection();
		ResultSet resultSet
		PaymentStatus payStatus = null
		PaymentStatusDetails payDetails
		def active = "y"
		Statement stmt
		int count = 0
		int sumAmount = 0
		//		bookingReferenceId = 7877
		sleep(100000)
		try {
			stmt = connection.createStatement();
			println "Query:" + " SELECT PYLG_STS,PYLG_BKPVL FROM RT" + siteId + "DTA.RTPYLGP WHERE PYLG_STS = 'ER' AND PYLG_BKUI = " + bookingReferenceId + ""
			resultSet = stmt.executeQuery("SELECT PYLG_STS,PYLG_BKPVL FROM RT" + siteId + "DTA.RTPYLGP WHERE  PYLG_BKUI = " + bookingReferenceId + "");
			//resultSet = stmt.executeQuery("SELECT * FROM RT" + siteId + "DTA.RTBKPYP WHERE BKPY_BKUI = " + bookingReferenceId + " AND BKPY_ACTIV = 'Y' AND BKPY_BKPUI = 1");


			while (resultSet.next()) {
				count = resultSet.row
				if (resultSet.row == 1) {
					String amountStr = resultSet.getString("PYLG_BKPVL")
					Integer amount = Integer.valueOf(amountStr)
					String paymentStatus = resultSet.getString("PYLG_STS")
					sumAmount += amount
					payStatus = new PaymentStatus()
					payStatus.setPaymentStatus(resultSet.getString("PYLG_STS").trim())
					//payStatus.setTotalAmount(resultSet.getString("PYLG_BKPVL").toString().trim())


					break
				}
			}
		}

		catch (Exception exception) {

			exception.printStackTrace()
		}
		println "PaymentStatus : "+payStatus
		return payStatus

	}

	def BookingStatus getBookingStatusDetails(String bookingReferenceId, String siteId , String expbookingStatus, int retry) {
		Connection connection = AS400DatabaseUtil.getAs400Connection();
		print "AS400 connection " + connection
		ResultSet resultSet
		BookingStatus bookingStatus = null
		int count = 0
		int noOfTries = 1
		sleep(10_000)
		for (k in 0..retryCount) {
			sleep(retryIntervalSec*1000)
			try {
				Statement stmt = connection.createStatement();
				bookingStatus = new BookingStatus()
				//print "SELECT BKMS_STS, BKMS_GRSVL,BKMS_NETVL FROM  RT" + siteId + "DTA.RTBKMSP WHERE BKMS_BKUI = " + bookingReferenceId + ""
				resultSet = stmt.executeQuery("SELECT BKMS_STS, BKMS_GRSVL,BKMS_NETVL FROM  RT" + siteId + "DTA.RTBKMSP WHERE BKMS_BKUI = " + bookingReferenceId + "");

				while (resultSet.next()) {
					count = resultSet.row
					//println "number of rows DB: $count"
					//println " booking status in query DB : " + resultSet.getString("BKMS_STS")
					if (resultSet.row == 1) {
						bookingStatus.setBookingstatus(resultSet.getString("BKMS_STS").trim())
						bookingStatus.setGrossamount(resultSet.getString("BKMS_GRSVL").toString().trim())
						bookingStatus.setNetamount(resultSet.getString("BKMS_NETVL").toString().trim())
						break
					}
				}


			} catch (Exception exception) {

				exception.printStackTrace()
			}

			if(bookingStatus.getBookingstatus().equalsIgnoreCase(expbookingStatus)){
				break
			}

		}
		bookingStatus.setRowcount(count)
		return bookingStatus
	}

	public void finallize() {
		if(connection!= null) {
			connection.close()
		}
		
	}
	}

