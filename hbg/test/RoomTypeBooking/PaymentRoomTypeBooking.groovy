package com.hbg.test.RoomTypeBooking

import groovy.xml.XmlUtil

import org.testng.asserts.SoftAssert

import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll

import com.hbg.dto.BookingStatus
import com.hbg.dto.PaymentStatus;
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPriceHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.qa.automation.util.ClientUnlockUtil;

abstract class PaymentRoomTypeBooking extends XSellBaseTest {
	
		
	////static final String SHEET_NAME_SEARCH="SHPPR_Xsell"
	
	static final String SHEET_NAME="Payment"
	static final String SHEET_NAME_ADDBOOKINGITEMREQ="xmle2e_AddBookingItemRequest"
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("roomTypeBookingSheet"))
	static final String URL= ConfigProperties.getValue("AuthenticationURL")
	static final String SHEET_NAME_SEARCH="Payment"	
	static final String SITEID= ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey= ConfigProperties.getValue("FITSiteIdHeaderKey")
	SearchHotelPriceHelper helper = new SearchHotelPriceHelper(EXCEL_SHEET,getClientId())	
	SearchHotelPricePaxHelper helper1 = new SearchHotelPricePaxHelper(EXCEL_SHEET,getClientId())			
		
	
	
	int searchRowNum=4
	String PaxId
	private static def xmlParser = new XmlParser()
	SoftAssert softAssert = new SoftAssert()
	Node node_search
	String responseBody_search
	int rowNum
	
	AS400DatabaseUtil dbconn= new AS400DatabaseUtil()
	BookingStatus bookingStatus = null
	PaymentStatus paymentStatus = null
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
	
	
	@Shared
	List<HotelDTO> hotelList = new ArrayList<HotelDTO>()

	@Shared
	List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()

	@Shared
	HotelDTO successHotelDTO = null
	
	@Shared
	List cities
		
	@Unroll
	def "1.RoomTypeBooking - Payment - SHPR"(){
	
		println "RoomTypeBooking - Payment - SHPPR"
		ClientUnlockUtil c = new ClientUnlockUtil()
		c.unlockClient("994")
		cities=numOfCitiesList(3)
		for(k in 0.. cities.size()-1 ){
			
			def checkInDate = helper.getCheckInDate()
			def fileContents_Search = helper1.getSHPRPayment("Payment",searchRowNum,cities[k], checkInDate, "DB", "2")					
//			println "SHPR Search Request Xml..Payment RoomType Booking \n${fileContents_Search}"					
			node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)					
//			logDebug("Search Response Xml..\n${responseBody_search}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)					
	
//			println cities[k]+" city hotels count : "+hList.size()
			if (hList.size() > 0){
				hotelList.addAll(hList)
			}
			
			println "\n\n"
		}
			
		println "total hotels count for all the cities : "+hotelList.size()
		softAssert.assertTrue(hotelList.size() > 0,"HBG Hotels not found for Payment for RoomType Booking")
		softAssert.assertAll()
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - SHPR "  | 1
		
	}
	
	@Unroll
	def "2.RoomTypeBooking - Payment - SingleItemWithPay"(){
		
		println "RoomTypeBooking - Payment - SingleItemWithPay"
		Node node
		String responseBody
		
		def expectedValue
		boolean flag = false
		
		if (hotelList.size()!=0){
			
			for (int i=0;i<hotelList.size();i++){
				
				HotelDTO hDTO = hotelList.get(i)
				def fileContents_Search = helper.getSingleItemPaymentwithPay(SHEET_NAME_SEARCH,searchRowNum,helper.getBookingReference(),hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
													
				println "SSearch Request Xml.. \n${fileContents_Search}"					
				node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)					
				logDebug("Search Response Xml..\n${responseBody_search}")
				
				if (responseBody_search.contains("<ErrorId>")){
					softAssert.assertTrue(false, "Failed : Received Error response")
				}else{
					def respBookingRef = node_search.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					if (respBookingRef!=null && respBookingRef.size() > 0){
						def respBookingRefsize = respBookingRef.size()
						def result = false
						for (k in 0.. respBookingRefsize)
						{
							 if(respBookingRef[k].@ReferenceSource.equals("api"))
							 {
								 flag=true
								 itemDetails.put('bookingId',respBookingRef[k].text())
								 print itemDetails.get("bookingId")
								 successHotelDTO = hDTO
								 break
							 }
						}
					}
					if (flag){
						break
					}
				}		
			}
			
			if (itemDetails.get("bookingId")!=null){
				paymentStatus=dbconn.getPaymentStatusDetails(itemDetails.get("bookingId"),SITEID.toString())
				softAssert.assertTrue(paymentStatus != null && !paymentStatus.getPaymentStatus().equalsIgnoreCase("ER"), "PAYMENT STATUS IS ERROR")
			}else{
				softAssert.assertTrue(itemDetails.get("bookingId")!=null,"No Booking Id exist")
				println "\n\n"
			}
		}else{
			softAssert.assertTrue(hotelList.size()!=0,"No HBG Hotels Exist")
			println "\n\n"
		}
		
		softAssert.assertAll()
		
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - SingleItemWithPay "  | 3
		
	}
	
	@Unroll
	def "2.1.RoomTypeBooking - Payment with MasterCard - SingleItemWithPay"(){
		
		println "RoomTypeBooking - Payment - SingleItemWithPay"
		Node node
		String responseBody
		
		def expectedValue
		boolean flag = false
		def bookingId
		if (hotelList.size()!=0){
			
			for (int i=0;i<hotelList.size();i++){
				
				HotelDTO hDTO = hotelList.get(i)
				def fileContents_Search = helper.getSingleItemPaymentwithMasterCard(SHEET_NAME_SEARCH,searchRowNum,helper.getBookingReference(),hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), "5454545454545454")
													
				println "SSearch Request Xml.. \n${fileContents_Search}"
				node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				
				if (responseBody_search.contains("<ErrorId>")){
					softAssert.assertTrue(false, "Failed : Received Error response")
				}else{
					def respBookingRef = node_search.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					if (respBookingRef!=null && respBookingRef.size() > 0){
						def respBookingRefsize = respBookingRef.size()
						def result = false
						for (k in 0.. respBookingRefsize)
						{
							 if(respBookingRef[k].@ReferenceSource.equals("api"))
							 {
								 flag=true
								 bookingId = respBookingRef[k].text()
								 print bookingId
								 break
							 }
						}
					}
					if (flag){
						break
					}
				}
			}
			
			if (bookingId!=null){
				paymentStatus=dbconn.getPaymentStatusDetails(bookingId,SITEID.toString())
				softAssert.assertTrue(paymentStatus != null && !paymentStatus.getPaymentStatus().equalsIgnoreCase("ER"), "PAYMENT STATUS IS ERROR")
			}else{
				softAssert.assertTrue(bookingId!=null,"No Booking Id exist")
				println "\n\n"
			}
		}else{
			softAssert.assertTrue(hotelList.size()!=0,"No HBG Hotels Exist")
			println "\n\n"
		}
		
		softAssert.assertAll()
		
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - SingleItemWithPay "  | 3
		
	}
	
	@Unroll
	def "3.RoomTypeBooking - Payment - SHPR2"(){
	
		println "RoomTypeBooking - Payment - SHPR2"
		cities=numOfCitiesList(4)
		for(k in 0.. cities.size()-1 ){
			
			if (cities[k] == successHotelDTO.getItemCity().toString()){ 
				continue 
			}
			
			hotelList = new ArrayList<HotelDTO>()
			
			def checkInDate = helper.getCheckInDatePlus()
			def fileContents_Search = helper1.getSHPRPayment("Payment",searchRowNum,cities[k], checkInDate, "SB", "1")
			println "SHPR Search Request Xml..Payment RoomType Booking \n${fileContents_Search}"
			node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)
			logDebug("Search Response Xml..\n${responseBody_search}")
			
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
	
			println cities[k]+" city hotels count : "+hList.size()
			if (hList.size() > 0){
				hotelList.addAll(hList)
			}
			
			println "\n\n"
		}
		
		println "total hotels count for all the cities : "+hotelList.size()
		
		softAssert.assertTrue(hotelList.size() > 0,"HBG Hotels not found for Payment for RoomType Booking")
		softAssert.assertAll()
		
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - SHPR2 "  | 1
	}
	
	@Unroll
	def "4.RoomTypeBooking - Payment - AddBookingItem"(){		
	
		println "RoomTypeBooking - Payment - AddBookingItem"
		
		if (itemDetails.get("bookingId")!=null){
			def flag=false
			for (int i=0;i<hotelList.size();i++){
				HotelDTO hDTO = hotelList.get(i)
				def checkInDate = helper.getCheckInDatePlus()
				def fileContents_Search = helper.getAddBookingtItem(SHEET_NAME_SEARCH,searchRowNum,itemDetails.get("bookingId"),hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(),checkInDate)
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"					
				node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)					
				logDebug("Search Response Xml..\n${responseBody_search}")
				
				if (responseBody_search.contains("<ErrorId>")){
					softAssert.assertTrue(false, "Failed : Received Error response")
				}else{
					def respBookingRef = node_search.ResponseDetails.BookingResponse.BookingReferences.BookingReference 
					if (respBookingRef!=null && respBookingRef.size() > 0){
						def respBookingRefsize = respBookingRef.size()
						println "Booking references size:"+ respBookingRefsize
						def result = false
						for (int k=0;k<respBookingRefsize;k++){
							if(respBookingRef[k].@ReferenceSource.equals("api")){
								result=true
								softAssert.assertTrue((respBookingRef[k].text().equals(itemDetails.get("bookingId"))),"BookingID does not match")
								flag=true
								break
							}
						}
						if (flag)  {
							break
						}
					}
				}
			}
			softAssert.assertTrue(node_search.ResponseDetails.BookingResponse.BookingPaymentStatus!=0)
			softAssert.assertTrue(node_search.@ResponseReference.startsWith('XA'))
			softAssert.assertTrue((node_search.ResponseDetails.BookingResponse.BookingItems.BookingItem.ItemStatus.text().equals("Confirmed")),"Booked Item Status not confirmed")
		}else{
			softAssert.assertTrue(itemDetails.get("bookingId")!=null,"No Booking Id exist")
			println "\n\n"
		}
		
		softAssert.assertAll()
		
		where:				
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - AddBookingItem "  | 3
	}
		
	def "5.RoomTypeBooking - Payment - SearchBookingItemRequest - True"(){
		println "RoomTypeBooking - Payment - searchBookingItemRequest - True"

		if (itemDetails.get("bookingId")!=null){

			def fileContents_Search = helper1.getSearchBookingItemRequestPayment(SHEET_NAME_SEARCH,searchRowNum,itemDetails.get("bookingId"),true)
			println "SHPPR Search Request Xml.. \n${fileContents_Search}"					
			 node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			 responseBody_search = XmlUtil.serialize(node_search)					
			logDebug("Search Response Xml..\n${responseBody_search}")
			//itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)				
			
			if (responseBody_search.contains("<ErrorId>")){
				softAssert.assertTrue(false, "Failed : Received Error response")
			}else{
				def respBookingRef = node_search.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
				def respBookingRefsize = respBookingRef.size()
				println "Booking References Size:"+respBookingRef.size()
			
				def result = false
				for (i in 0.. respBookingRefsize){
					if(respBookingRef[i].@ReferenceSource.equals("api")){
						result=true
						itemDetails.put('bookingId',respBookingRef[i].text())
						print itemDetails.get("bookingId")
						softAssert.assertTrue((respBookingRef[i].text().equals(itemDetails.get("bookingId"))),"BookingID does not match")
						break
					}
				}
				//def bookingItems = node_search.ResponseDetails.SearchBookingItemResponse.BookingItems.BookingItem
				//softAssert.assertTrue(bookingItems.size()==2, "Failed : Expected 2 booking items in response but return "+bookingItems.size())
				softAssert.assertTrue((node_search.ResponseDetails.SearchBookingItemResponse.BookingPaymentStatus.CardType.text().equals("VS")),"CardType is not matching")				
			}
		}else{
			softAssert.assertTrue(itemDetails.get("bookingId")!=null,"No Booking Id exist")
			println "\n\n"
		}
		
		softAssert.assertAll()
		
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - SearchBookingItemRequest - True "  | 3
		
	}
	
	
	def "6.RoomTypeBooking - Payment - SearchBookingRequest - true"()
	{
		println "RoomTypeBooking - Payment - searchBookingRequest - True"
		
		if (itemDetails.get("bookingId")!=null){
			
			def fileContents_Search = helper1.getsearchbookingitemdetails(SHEET_NAME,rowNum,itemDetails.get("bookingId"),true)
			println "SHPPR Search Request Xml.. \n${fileContents_Search}"					
			node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)					
			logDebug("Search Response Xml..\n${responseBody_search}")
			
			if (responseBody_search.contains("<ErrorId>")){
				softAssert.assertTrue(false, "Failed : Received Error response")
			}else{
			
				def respBookingRef = node_search.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
				def respBookingRefsize = respBookingRef.size()
				def result = false
				for (i in 0.. respBookingRefsize-1){
					if(respBookingRef[i].@ReferenceSource.equals("api")){
						result=true
						itemDetails.put('bookingId',respBookingRef[i].text())
						print itemDetails.get("bookingId")
						softAssert.assertTrue((respBookingRef[i].text().equals(itemDetails.get("bookingId"))),"BookingID does not match")
						break
					}
				}
				softAssert.assertTrue((node_search.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingPaymentStatus.CardType.text().equals("VS")),"CardType is not matching")
				softAssert.assertTrue((node_search.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingStatus.text().equals("Confirmed")),"Booking Status not Confirmed")			
			}
		}else{
			softAssert.assertTrue(itemDetails.get("bookingId")!=null,"No Booking Id exist")
			println "\n\n"
		}
		
		softAssert.assertAll()
		
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - SearchBookingRequest - true "  | 1
	}
	
		
	def "7.RoomTypeBooking - Payment - SearchBookingItemRequest - False"(){
		println "RoomTypeBooking - Payment - searchBookingItemRequest - False"

		if (itemDetails.get("bookingId")!=null){

			def fileContents_Search = helper1.getSearchBookingItemRequestPayment(SHEET_NAME_SEARCH,searchRowNum,itemDetails.get("bookingId"),false)
			println "SHPPR Search Request Xml.. \n${fileContents_Search}"					
			 node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			 responseBody_search = XmlUtil.serialize(node_search)					
			logDebug("Search Response Xml..\n${responseBody_search}")
			//itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)				
			
			if (responseBody_search.contains("<ErrorId>")){
				softAssert.assertTrue(false, "Failed : Received Error response")
			}else{
				def respBookingRef = node_search.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
				def respBookingRefsize = respBookingRef.size()
				println "Booking References Size:"+respBookingRef.size()
			
				def result = false
				for (i in 0.. respBookingRefsize){
					if(respBookingRef[i].@ReferenceSource.equals("api")){
						result=true
						itemDetails.put('bookingId',respBookingRef[i].text())
						print itemDetails.get("bookingId")
						softAssert.assertTrue((respBookingRef[i].text().equals(itemDetails.get("bookingId"))),"BookingID does not match")
						break
					}
				}
				softAssert.assertTrue(node_search.ResponseDetails.SearchBookingItemResponse.BookingPaymentStatus.size() == 0, "Failed : Expected No Payment details but Response contains payment details")				
			}
		}else{
			softAssert.assertTrue(itemDetails.get("bookingId")!=null,"No Booking Id exist")
			println "\n\n"
		}
		
		softAssert.assertAll()
		
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - SearchBookingItemRequest - False "  | 3
		
	}
		
	def "8.RoomTypeBooking - Payment - SearchBookingRequest - false"()
	{
		println "RoomTypeBooking - Payment - searchBookingRequest - false"
		
		if (itemDetails.get("bookingId")!=null){
			
			def fileContents_Search = helper1.getsearchbookingitemdetails(SHEET_NAME,rowNum,itemDetails.get("bookingId"),false)
			println "SHPPR Search Request Xml.. \n${fileContents_Search}"					
			node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)					
			logDebug("Search Response Xml..\n${responseBody_search}")
			
			if (responseBody_search.contains("<ErrorId>")){
				softAssert.assertTrue(false, "Failed : Received Error response")
			}else{
			
				def respBookingRef = node_search.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
				def respBookingRefsize = respBookingRef.size()
				def result = false
				for (i in 0.. respBookingRefsize-1){
					if(respBookingRef[i].@ReferenceSource.equals("api")){
						result=true
						itemDetails.put('bookingId',respBookingRef[i].text())
						print itemDetails.get("bookingId")
						softAssert.assertTrue((respBookingRef[i].text().equals(itemDetails.get("bookingId"))),"BookingID does not match")
						break
					}
				}
				softAssert.assertTrue(node_search.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingPaymentStatus.size() == 0, "Failed : Expected No Payment details but Response contains payment details")
				softAssert.assertTrue((node_search.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingStatus.text().equals("Confirmed")),"Booking Status not Confirmed")			
			}
		}else{
			softAssert.assertTrue(itemDetails.get("bookingId")!=null,"No Booking Id exist")
			println "\n\n"
		}
		
		softAssert.assertAll()
		
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - SearchBookingRequest - false "  | 1
	}
		
		
	def "9.RoomTypeBooking - Payment - CancelBookingItemRequest"()
	{
		println "RoomTypeBooking - Payment - CancelBookingItemRequest"
		
		if (itemDetails.get("bookingId")!=null){
			
			def fileContents_Search = helper1.cancelBookingItemRequest(SHEET_NAME_SEARCH,searchRowNum,itemDetails.get("bookingId"), "1")
			println "SHPPR Search Request Xml.. \n${fileContents_Search}"					
			node_search = helper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)					
			logDebug("Search Response Xml..\n${responseBody_search}")
	
			if (responseBody_search.contains("<ErrorId>")){
				softAssert.assertTrue(false, "Failed : Received Error response")
			}else{
				def respBookingRef = node_search.ResponseDetails.BookingResponse.BookingReferences.BookingReference
				def respBookingRefsize = respBookingRef.size()
				def result = false
				for (i in 0.. respBookingRefsize-1){
					if(respBookingRef[i].@ReferenceSource.equals("api")){
						result=true
						itemDetails.put('bookingId',respBookingRef[i].text())
						print itemDetails.get("bookingId")
						softAssert.assertTrue((respBookingRef[i].text().equals(itemDetails.get("bookingId"))),"BookingID does not match")
						break
					}
				}
				
				softAssert.assertTrue((node_search.ResponseDetails.BookingResponse.BookingStatus.text().equals("Cancelled")),"Failed : Booking not cancelled")
			}
		}else{
			softAssert.assertTrue(itemDetails.get("bookingId")!=null,"No Booking Id exist")
			println "\n\n"
		}
		
		softAssert.assertAll()
		
		where:
		rowNum | testDesc | searchRowNum
		1|"RoomTypeBooking - Payment - CancelBookingItemRequest "  | 3
		
	}
	
}


class PaymentRoomTypeBooking_C1238 extends PaymentRoomTypeBooking {
	@Override
	String getClientId() {
		return "1238"
	}
}
@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class PaymentRoomTypeBookingSingleClient extends PaymentRoomTypeBooking {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}


