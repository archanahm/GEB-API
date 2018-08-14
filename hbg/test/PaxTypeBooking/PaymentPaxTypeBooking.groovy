package com.hbg.test.PaxTypeBooking

import com.hbg.dto.PaymentStatus
import com.hbg.dto.PaymentStatusDetails

import org.testng.asserts.SoftAssert;

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties


import groovy.xml.XmlUtil
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll
	

abstract class PaymentPaxTypeBooking extends XSellBaseTest{
	
	////static final String SHEET_NAME_SEARCH="SHPPR_Xsell"
	static final String SHEET_NAME="Payment"
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("paxexcelSheet"))
	static final String URL= ConfigProperties.getValue("AuthenticationURL")
	static final String SHEET_NAME_SEARCH="Payment"

	static final String SITEID= ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey= ConfigProperties.getValue("FITSiteIdHeaderKey")
	SearchHotelPricePaxHelper helper = new SearchHotelPricePaxHelper(EXCEL_SHEET,getClientId())
	int searchRowNum=4
	def itemRefrence
	def cardDetails
	def cardType
	String PaxId
	private static def xmlParser = new XmlParser()
	
	AS400DatabaseUtil dbconn= new AS400DatabaseUtil()
	BookingStatus bookingStatus = null
	PaymentStatus paymentStatus = null
	Node node_search
	String responseBody_search
	int rowNum
	
	int k
	@Shared
	def itemDetails = [:]
	@Shared
	def itemCode = []
	@Shared
	def roomCategory = []
	@Shared
	def itemCity = []
	@Shared
	def itemCityName = []
	@Shared
	def itemName = []
	int hotelCount =0
	def flag = false
	Map itemMapGTA=null
	Map itemMapHBG=null
	Map fields = null
	
	SoftAssert softAssert= new SoftAssert()
	
	@Shared
	List<HotelDTO> hotelList = new ArrayList<HotelDTO>()

	List<HotelDTO> hotelListLocal = new ArrayList<HotelDTO>()



	@Shared
	List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()

	@Shared
	HotelDTO successHotelDTO = null
	
	@Shared
	List cities
	@Unroll
	def "1.PaxTypeBooking - Payment - SHPPR1"()
	{
		println "PaxTypeBooking - Payment - SHPPR1"
		cities=numOfCitiesList(5)
		println "cities size"+ cities.size()
		for(k in 0.. cities.size()-1 )
			{
				def fileContents_Search = helper.getSHPPRPayment(SHEET_NAME_SEARCH,searchRowNum,cities[k])
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"				
				 node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)				
				logDebug("Search Response Xml..\n${responseBody_search}")
				//itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)

				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

				if (hList.size() > 0){
					hotelList.addAll(hList)
				}
				
				List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)

				if (gtaHList.size() > 0){
					gtaHotelList.addAll(gtaHList)
				}
			}
			println "total hotels count for all the cities : "+hotelList.size()
			softAssert.assertTrue(hotelList.size() > 0,"HBG Hotels not found")
			
			softAssert.assertAll()
			println "\n\n"					
			
			where:
			rowNum | testDesc | searchRowNum
			1|"City Search "  | 1
		
		
	}
	@Unroll
	def "2.PaxTypeBooking - SingleItemWithPay_BB #testDesc"()
	{
		println "PaxTypeBooking - Payment - SingleItemWithPay_BB"		
		def flag=false
		def bookingReferenceNum = null
		def expectedValue
		def result = false
		String responseBody_booking
		if(hotelList.size() != 0) {
			println "Hotel size : " + hotelList.size()
			for (k in  0 .. hotelList.size()-1) {
				HotelDTO hDTO = hotelList.get(k)
				bookingReferenceNum = helper.getBookingReference()

				def fileContents_Search = helper.getSingleItemPaymentwithPay(SHEET_NAME_SEARCH, searchRowNum, bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), cardDetails,cardType)

				println "SHPPR Search Request Xml.. \n${fileContents_Search}"
				node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey,SITEID)
			    responseBody_search = XmlUtil.serialize(node_search)			
				logDebug("Search Response Xml..\n${responseBody_search}")				
					//TO Capture the Booking ID
				if (node_search.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
					successHotelDTO = hDTO
					successHotelDTO.setBookingReference(bookingReferenceNum)
					def respBookingRef = node_search.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()

					for (i in 0..respBookingRefsize) {
						if (respBookingRef[i].@ReferenceSource.equals("client")) {
							result = true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
						}

						if (respBookingRef[i].@ReferenceSource.equals("api")) {
							result = true
							successHotelDTO.setBookingId(respBookingRef[i].text())
							successHotelDTO.setCategoryId(hDTO.getCategoryId())
							break
						}

					}
				softAssert.assertTrue(result == true, "ReferenceSource is not client")
					bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
					if(bookingStatus.getBookingstatus().equalsIgnoreCase("C") ){
						successHotelDTO.setBookingId(respBookingRef[respBookingRef.size()-1].text())
						successHotelDTO.setCategoryId(hDTO.getCategoryId())
						paymentStatus= dbconn.getPaymentStatusDetails(successHotelDTO.getBookingId(), SITEID.toString())
						softAssert.assertTrue(!paymentStatus.getPaymentStatus().equalsIgnoreCase("ER"), "PAYMENT STATUS IS ERROR")
					}else
					{
						softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries Hence the Payment status not checked for this booking")
					}

					break
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody_booking)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody_booking)
				}
				else if (node_search.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
					def errorStr = node_search.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
					println "ERROR IN RESPONSE " + errorStr
					errorStr = errorStr + ":" + node_search.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
					println "ERROR IN RESPONSE " + errorStr
					softAssert.assertFalse(node_search.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
				} else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
					def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_search.ResponseDetails.Errors.Error.ErrorText.text()
					print "ERROR IN RESPONSE " + Errostring
					softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
				}


			if(result == true)
				break
			}

		}
		else{
			softAssert.assertTrue(hotelList.size()  != 0, "NO HBG Hotels found")
		}

		softAssert.assertAll()
		println "\n\n"

			where:
			rowNum | testDesc | searchRowNum | cardDetails | cardType
			1|"Add booking with Payment MC card "  | 3 | 5454545454545454 | "MC"
			1|"Add booking with Payment VS card "  | 3 | 4111111111111111 | "VS"
	
		
	}
	
	def "3.PaxTypeBooking - Payment - SHPPR2"()
	{
		println "PaxTypeBooking - Payment - SHPPR2"		
		for(k in 0.. cities.size()-1 )
			{
				def fileContents_Search = helper.getSHPPRPayment(SHEET_NAME_SEARCH,searchRowNum,cities[k])
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"				
				node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)				
				logDebug("Search Response Xml..\n${responseBody_search}")
				//itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)
				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				
				if (hotelNode!=null)
				{		
					List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
					println cities[k]+" city hotels count : "+hList.size()
					
					if (hList.size() > 0){
						hotelList.addAll(hList)
					}
					
					List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
					println cities[k]+" city hotels count : "+gtaHList.size()
					if (gtaHList.size() > 0){
						gtaHotelList.addAll(gtaHList)
					}
			}
			}
			println "total hotels count for all the cities : "+hotelList.size()
			softAssert.assertTrue(hotelList.size() > 0,"HBG Hotels not found")
			softAssert.assertAll()
			println "\n\n"					
		
			where:
			rowNum | testDesc | searchRowNum
			1|"City Search "  | 1
		
	}
	
	def "4.PaxTypeBooking - Payment - AddBookingItem_BB #testDesc"()
	{
		println "PaxTypeBooking - Payment - AddBookingItem_BB"
		def flag=false
		def bookingReferenceNum = null
		def expectedValue
		cities=numOfCitiesList(5)
		cities.add("LON")
		def result = false

		if (successHotelDTO != null ) {
			for(k in 0.. cities.size()-1 )
			{
				def fileContents_Search = helper.getSHPPRPayment(SHEET_NAME_SEARCH,searchRowNum,cities[k])
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"
				node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				//itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)
				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel

				if (hotelNode!=null)
				{
					List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
					println cities[k]+" city hotels count : "+hList.size()

					if (hList.size() > 0){
						hotelListLocal.addAll(hList)
					}

				}
			}
			println "total hotels count for all the cities : "+hotelListLocal.size()
			softAssert.assertTrue(hotelListLocal.size() > 0,"HBG Hotels not found")
			softAssert.assertAll()
			println "\n\n"
			for (int k = 0; k < hotelListLocal.size(); k++) {
				HotelDTO hDTO = hotelListLocal.get(k)
				def fileContents_Search = helper.getAddBookingtwithPay(SHEET_NAME_SEARCH, searchRowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"
				node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")

				if (node_search.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {


					def respBookingRef = node_search.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()

					for (i in 0..respBookingRefsize) {
						if (respBookingRef[i].@ReferenceSource.equals("client")) {
							result = true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
						}

						if (respBookingRef[i].@ReferenceSource.equals("api")) {
							result = true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking reference ID does not match")
							break
						}

					}
					softAssert.assertTrue(result == true, "ReferenceSource is not client")
					bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
					if(bookingStatus.getBookingstatus().equalsIgnoreCase("C")){
						paymentStatus= dbconn.getPaymentStatusDetails(successHotelDTO.getBookingId(), SITEID.toString())
						softAssert.assertTrue(!paymentStatus.getPaymentStatus().equalsIgnoreCase("ER"), "PAYMENT STATUS IS ERROR")
					}

					break
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody_booking)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody_booking)
				} else if (node_search.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
					def errorStr = node_search.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
					println "ERROR IN RESPONSE " + errorStr
					errorStr = errorStr + ":" + node_search.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
					println "ERROR IN RESPONSE " + errorStr
					softAssert.assertFalse(node_search.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
				} else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
					def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_search.ResponseDetails.Errors.Error.ErrorText.text()
					print "ERROR IN RESPONSE " + Errostring
					softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
				}
			}

			if(result == true){

				//	helper.validateSuccessResponse(responseBody_booking,expectedValue)
				}
			} else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()
		where:
			rowNum | testDesc | searchRowNum
			1|"Addbooking item "  | 1
	}
	
	def "5.PaxTypeBooking - Payment - SearchBookingItemRequest - True"()
	{
		println "PaxTypeBooking - Payment - searchBookingItemRequest - True"

		if(successHotelDTO != null )
		{
				def fileContents_Search = helper.getSearchBookingItemRequestPayment(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId(),true)
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"				
				 node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)				
				logDebug("Search Response Xml..\n${responseBody_search}")
				//itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)			
				
				def respBookingRef = node_search.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
				if (respBookingRef!=null)
				{
					def respBookingRefsize = respBookingRef.size()
					println "Booking References Size:"+respBookingRef.size()
					
					def result = false
					for (i in 0.. respBookingRefsize)
					{
		
						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							itemDetails.put('bookingId',respBookingRef[i].text())
							print itemDetails.get("bookingId")
							softAssert.assertTrue((respBookingRef[i].text().equals(successHotelDTO.getBookingId())),"BookingID does not match")
							break
						}
		
					}
				
					//softAssert.assertFalse(itemMapGTA==null,"No GTA Hotels Found")				

					println "\n	Valid response ... \nResponseReference : ${node_search.@ResponseReference}"
					println "Booking Payment Status:"+node_search.ResponseDetails.SearchBookingItemResponse.BookingPaymentStatus.size()
					softAssert.assertTrue(node_search.ResponseDetails.SearchBookingItemResponse.BookingPaymentStatus!=0,"Booking Payment Status not returned")
					softAssert.assertTrue((node_search.ResponseDetails.SearchBookingItemResponse.BookingPaymentStatus.CardType.text().equals("MC")),"CardType is not matching")
	
			//}

				}
				else
					{
						softAssert.assertTrue(respBookingRef!=null,"Booking Reference not returned")

					}
		}else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()
		where:
			rowNum | testDesc | searchRowNum
			1|"City Search "  | 1
	}
	
	
	def "6.PaxTypeBooking - Payment - SearchBookingRequest - true"()
	{
		println "PaxTypeBooking - Payment - searchBookingRequest - True"

		if(successHotelDTO != null )
		{
				def fileContents_Search = helper.getsearchbookingitemdetails(SHEET_NAME,rowNum,successHotelDTO.getBookingId(),true)
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"				
				 node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)				
				logDebug("Search Response Xml..\n${responseBody_search}")	
				
				
				def respBookingRef = node_search.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
				
				if (respBookingRef!=null)
				{
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0.. respBookingRefsize-1)
					{
		
						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							itemDetails.put('bookingId',respBookingRef[i].text())
							print itemDetails.get("bookingId")
							softAssert.assertTrue((respBookingRef[i].text().equals(successHotelDTO.getBookingId())),"BookingID does not match")
							break
						}
		
					}					
			

					println "Booking Payment Status:"+node_search.ResponseDetails.SearchBookingResponse.BookingStatus.size()
					softAssert.assertTrue(node_search.ResponseDetails.SearchBookingResponse.BookingPaymentStatus!=0,"Boooking Payment Status not returned")
					bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
					//softAssert.assertTrue((node_search.ResponseDetails.SearchBookingResponse.BookingStatus.text().equals("Confirmed")),"Booking Status not Confirmed")

				}
				else
				{
					softAssert.assertTrue(respBookingRef!=null,"Booking Reference not returned")

				}
		}else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()
			where:
			rowNum | testDesc | searchRowNum
			1|"City Search "  | 1
	}
	
	
	def "7.PaxTypeBooking - Payment - SearchBookingItemRequest - False"()
	{
		println "PaxTypeBooking - Payment - searchBookingItemRequest - False"
		if(successHotelDTO != null )
		{
				def fileContents_Search = helper.getSearchBookingItemRequest(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId())
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"				
				 node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)				
				logDebug("Search Response Xml..\n${responseBody_search}")
				//itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)

				
				
				def respBookingRef = node_search.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
				if (respBookingRef!=null)
				{
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0.. respBookingRefsize-1)
					{
		
						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							itemDetails.put('bookingId',respBookingRef[i].text())
							print itemDetails.get("bookingId")
							softAssert.assertTrue((respBookingRef[i].text().equals(successHotelDTO.getBookingId())),"BookingID does not match")
							break
						}
		
					}

					softAssert.assertTrue(node_search.ResponseDetails.SearchBookingItemResponse.BookingPaymentStatus!=0)
					bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
				//	softAssert.assertTrue((node_search.ResponseDetails.SearchBookingItemResponse.BookingStatus.text().equals("Confirmed")),"Booking Status not Confirmed")

				}else
				{
					softAssert.assertTrue(respBookingRef!=null,"Booking Reference not returned")

				}
		}else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()
			where:
			rowNum | testDesc | searchRowNum
			1|"City Search "  | 1
	}
	
	
	def "8.PaxTypeBooking - Payment - SearchBookingRequest - False"()
	{
		println "PaxTypeBooking - Payment - searchBookingRequest - False"

		if(successHotelDTO != null ) {
				def fileContents_Search = helper.getsearchbookingitemdetails(SHEET_NAME,rowNum,successHotelDTO.getBookingId(),false)
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"				
				 node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)				
				logDebug("Search Response Xml..\n${responseBody_search}")
				
				
				def respBookingRef = node_search.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
				
				if (respBookingRef!=null)
				{
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0.. respBookingRefsize-1)
					{
		
						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							itemDetails.put('bookingId',respBookingRef[i].text())
							print itemDetails.get("bookingId")
							softAssert.assertTrue((respBookingRef[i].text().equals(successHotelDTO.getBookingId())),"BookingID does not match")
							break
						}
		
					}			
					
					softAssert.assertTrue(node_search.@ResponseReference.startsWith('XA'))				
					softAssert.assertTrue(node_search.ResponseDetails.SearchBookingResponse.BookingPaymentStatus!=0)
					bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
					softAssert.assertAll()
					println "\n\n"
				}else
					{
						softAssert.assertTrue(respBookingRef!=null,"Booking Reference not returned")
						softAssert.assertAll()
						println "\n\n"
					}
		}else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}

	softAssert.assertAll()
			where:
			rowNum | testDesc | searchRowNum
			1|"City Search "  | 1
	}
	
	
	def "9.PaxTypeBooking - Payment - CancelBookingItemRequest"()
	{
		println "PaxTypeBooking - Payment - CancelBookingItemRequest"

		if(successHotelDTO != null ) {
				def fileContents_Search = helper.cancelBookingItemRequest(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId(),itemRefrence)
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"				
				 node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)				
				logDebug("Search Response Xml..\n${responseBody_search}")
							

				def respBookingRef = node_search.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def expectedValue = "Cancelled"+"|Confirmed"
			if(node_search.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
				bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
			//	softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
				//helper.validateSuccessResponse(responseBody_search,expectedValue)

				softAssert.assertTrue(node_search.ResponseDetails.BookingResponse.BookingPaymentStatus!=0)
				softAssert.assertTrue((node_search.ResponseDetails.BookingResponse.BookingPaymentStatus.MaskedCardNumber.text()!=null),"Masked Card Number does notdisplay")
				softAssert.assertTrue((node_search.ResponseDetails.BookingResponse.BookingPaymentStatus.CardType.text().equals("MC")),"CardType is not matching")
				softAssert.assertTrue((node_search.ResponseDetails.BookingResponse.BookingPaymentStatus.BalanceAmount.text()!=0),"Balance Amount Displayed")
				//softAssert.assertTrue((node_search.ResponseDetails.BookingResponse.BookingItems.BookingItem.ItemReference.text()==1),"Item Reference is Matching")
				softAssert.assertTrue((node_search.ResponseDetails.BookingResponse.BookingItems.BookingItem.ItemStatus.text().equals("Cancelled")),"Item Reference is Matching")

			}else if(helper.getExpectedResult().equals("Failure")){
				helper.validateFailureResponse(responseBody_search)
			}else if(helper.getExpectedResult().equals("Empty")){
				helper.validateXmlWithEmptyResponse(responseBody_search)
			}else if (node_search.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
				def errorStr = node_search.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
				println "ERROR IN RESPONSE " + errorStr
				errorStr = errorStr + ":" + node_search.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
				println "ERROR IN RESPONSE " + errorStr
				softAssert.assertFalse(node_search.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
			}
			else if(node_search.ResponseDetails.Errors.Error.size()> 0){
				def Errostring=node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_search.ResponseDetails.Errors.Error.ErrorText.text()
				print "ERROR IN RESPONSE " + Errostring
				softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size()> 0, "Error Returned in Booking Response " + Errostring)
			}


	}else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()
			where:
			rowNum | testDesc | searchRowNum | itemRefrence
			7|"City Search "  | 1 | 1
	}}


	
	class PaymentPaxTypeBooking_C1238 extends PaymentPaxTypeBooking {
		@Override
		String getClientId() {
			return "1238"
		}
	}
	



@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class PaymentPaxTypeBookingSingleClient extends PaymentPaxTypeBooking {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}


