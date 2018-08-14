package com.hbg.test.RoomTypeBooking

import groovy.xml.XmlUtil

import org.testng.asserts.SoftAssert

import spock.lang.*

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.RoomTypeBookingHelper.RoomTypeBookingHelper_1DB_2Cot_HBG
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil

abstract class RoomTypeBookingTest_1DB_2Cot_HBG extends XSellBaseTest {

	static final String Sheet_NAME_1DB_2Cot_HBG = "1DB_2Cot_HBG"
	static final String EXCEL_SHEET = ConfigProperties.getValue("roomTypeBookingSheet")
	static final String SITEID = ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey = ConfigProperties.getValue("FITSiteIdHeaderKey")
	
	RoomTypeBookingHelper_1DB_2Cot_HBG helper = new RoomTypeBookingHelper_1DB_2Cot_HBG(EXCEL_SHEET, getClientId())

	int rowNum
	String sheet,testDesc,Data
	private static def xmlParser = new XmlParser()
	SoftAssert softAssert= new SoftAssert()
	XMLValidationUtil XmlValidate = new XMLValidationUtil()

	@Shared
	List<HotelDTO> hotelList = new ArrayList<HotelDTO>()

	@Shared
	List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()

	@Shared
	HotelDTO successHotelDTO = null
	
	@Shared
	boolean addBookingSuccess = false
	
	@Shared
	boolean addBookingMultipleItemSuccess = false

	@Shared
	AS400DatabaseUtil dbconn= new AS400DatabaseUtil()
	
	@Shared
	BookingStatus bookingStatus

	@Unroll
	def "Verify RoomTypeBooking test #1 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		def cities = numOfCitiesList(10)
		for(k in 0.. cities.size()-1){
			def fileContents_shpr = helper.searchHotelPriceRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, cities[k])
			println "Search Request Xml.. \n${fileContents_shpr}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_shpr,'SearchHotelPriceRequest',FITSiteIdHeaderKey,SITEID)
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			softAssert.assertTrue(node.ResponseDetails.SearchHotelPriceResponse != 0)
			def hotelNode = node.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			logDebug(cities[k]+" hbg city hotels count : "+hList.size())
			if (hList.size() > 0){
				hotelList.addAll(hList)
			}
			
			List<HotelDTO> gtaHList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
			logDebug(cities[k]+" gta city hotels count : "+gtaHList.size())
			if (gtaHList.size() > 0){
				gtaHotelList.addAll(gtaHList)
			}
		}
		
		println "total hbg hotels count for all the cities : "+hotelList.size()
		println "total hbg hotels count for all the cities : "+gtaHotelList.size()
		softAssert.assertTrue(hotelList.size() > 0,"Available hotel not found")
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - SearchHotelPriceRequest"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 2 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		
		def expectedValue
		boolean flag = false
		println "hotel size : "+hotelList.size()
		for (int i=0;i<hotelList.size();i++){
			HotelDTO hDTO = hotelList.get(i)
			
			def fileContents = helper.hotelPriceBreakdownRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'HotelPriceBreakdownRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
			if (responseBody.contains("ErrorId") || !responseBody.contains("<PriceRange>")){
				continue
			}else{
				//expectedValue = hDTO.getItemCity().toString() +"|"+ hDTO.getItemCode().toString()+"|"+hDTO.getCategoryId().toString()
				expectedValue = hDTO.getItemCity().toString() +"|"+ hDTO.getItemCode().toString()
				flag = true
				break
			}
		}
		
		println "flag : "+flag
		
		if (flag){
			
			def hotelPriceBreakdownResponse = node.ResponseDetails.HotelPriceBreakdownResponse
			String itemCityCode = hotelPriceBreakdownResponse.ItemCity[0].@Code.toString()
			String itemCode = hotelPriceBreakdownResponse.Item[0].@Code.toString()
	
			def hotelRoom = hotelPriceBreakdownResponse.HotelItem.HotelRooms.HotelRoom
			for (int i=0;i<hotelRoom.size();i++){
				String roomCategoryId = hotelRoom[i].@Id.toString()
				String hotelCode = hotelRoom[i].@Code.toString()
				//def actualValue = itemCityCode+"|"+itemCode+"|"+roomCategoryId
				def actualValue = itemCityCode+"|"+itemCode
				softAssert.assertTrue(expectedValue==actualValue, "Item City/Code : actual value ("+actualValue+") not matching with expected value ("+expectedValue+") for the hotelCode : "+ hotelCode)
			}
	
	
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			logDebug("\n	Valid response ... \nResponseReference : ${node.@ResponseReference}")
			softAssert.assertTrue(node.ResponseDetails.HotelPriceBreakdownResponse != 0)
		}else{
			softAssert.assertTrue(false, "HotelPriceBreakdownRequest failed to get valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - HotelPriceBreakdownRequest"
	}
	
	
	@Unroll
	def "Verify RoomTypeBooking test 3 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		def bookingReferenceNum = null
		def expectedValue
		boolean flag = false
		println "hotel size : "+hotelList.size()
		for (int i=0;i<hotelList.size();i++){
			HotelDTO hDTO = hotelList.get(i)
			bookingReferenceNum = helper.getBookingReference()
			def fileContents = helper.addBookingRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'AddBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
			if ((responseBody.contains("ErrorId") && (responseBody.contains("Hotel is invalid")))|| !responseBody.contains("<BookingReference ReferenceSource=")){
				continue
			}else{
				
				def respBookingItem = node.ResponseDetails.BookingResponse.BookingItems.BookingItem
				if (respBookingItem != null && respBookingItem.size() > 0){
					def itemStatus = respBookingItem[0].ItemStatus
					String itemStatusCode = itemStatus[0].@Code
					if (itemStatusCode != null && itemStatusCode == "RJ"){
						continue
					}
					expectedValue = hDTO.getItemCity().toString() +"|"+ hDTO.getItemCode().toString()+"|Confirmed"
					flag = true
					successHotelDTO = hDTO
					successHotelDTO.setBookingReference(bookingReferenceNum)
					
					break
				}else{
					continue
				}
			}
		}
		println "flag : "+flag
		if (flag){
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize){
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("client")){
					result = true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("api")){
					result = true
					successHotelDTO.setBookingId(respBookingRef[2].text())
					println "BookingId : "+successHotelDTO.getBookingId()
					break
				}
			}
			
			if(result){
				addBookingSuccess = true
			}
			
			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 3 retries")

			
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
	
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
		}else{
			softAssert.assertTrue(false, "AddBookingRequest failed to get valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - AddBookingRequest"
	}
	
	
	@Unroll
	def "Verify RoomTypeBooking test 4 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingSuccess){
			Node node
			String responseBody
			List<String> bookingRefList = new ArrayList<String>()
			List<String> bookingIdList = new ArrayList<String>()
	
			def fileContents = helper.searchBookingRequest1For_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum)
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
	
			def respBookingRef = node.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
	
			for (i in 0.. respBookingRefsize-1) {
	
				if (respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("client")) {
					bookingRefList.add(respBookingRef[i].text())
				}
	
				if (respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("api")) {
					bookingIdList.add(respBookingRef[i].text())
				}
	
				if (i == respBookingRefsize-1){break}
			}
	
			softAssert.assertTrue(bookingRefList.contains(successHotelDTO.getBookingReference().toString()), "Booking reference number does not exist")
			softAssert.assertTrue(bookingIdList.contains(successHotelDTO.getBookingId().toString()), "Booking Id does not exist")
	
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			softAssert.assertTrue(node.ResponseDetails.SearchBookingResponse != 0)
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - SearchBookingRequest With Date Range"
	}

	@Unroll
	def "Verify RoomTypeBooking test 5 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.searchBookingRequest2For_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
	
			def respBookingRef = node.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("client")) {
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("api")) {
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
					break
				}
			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
	
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			softAssert.assertTrue(node.ResponseDetails.SearchBookingResponse != 0)
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}	

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - SBR with BookingId"
	}
	@Unroll
	def "Verify RoomTypeBooking test 6 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingSuccess){
			HotelDTO anotherSuccessHotelDTO = new HotelDTO()
			Node node1
			String responseBody1
			def bookingReferenceNum1 = null
			boolean flag1 = false
			List<HotelDTO> list = new ArrayList<HotelDTO>()
			for (int i=0;i<hotelList.size();i++){
				HotelDTO hDTO = hotelList.get(i)
				if (hDTO.getCategoryId() == successHotelDTO.getCategoryId()){
					continue
				}
				
				bookingReferenceNum1 = helper.getBookingReference()
				def fileContents = helper.addBookingRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, bookingReferenceNum1, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
				node1 = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'AddBookingRequest',FITSiteIdHeaderKey,SITEID)
				responseBody1 = XmlUtil.serialize(node1)
				
				if ((responseBody1.contains("ErrorId") && (responseBody1.contains("<ItemStatus Code=\"RJ\">Rejected</ItemStatus>")) && (responseBody1.contains("Hotel is invalid")))|| !responseBody1.contains("<BookingReference ReferenceSource=")){
					continue
				}else{
					def respBookingItem = node1.ResponseDetails.BookingResponse.BookingItems.BookingItem
					if (respBookingItem != null && respBookingItem.size() > 0){
						def itemStatus = respBookingItem[0].ItemStatus
						String itemStatusCode = itemStatus[0].@Code
						if (itemStatusCode != null && itemStatusCode == "RJ"){
//							flag1 = true
							anotherSuccessHotelDTO = hDTO
							anotherSuccessHotelDTO.setBookingReference(bookingReferenceNum1)
							list.add(anotherSuccessHotelDTO)
							anotherSuccessHotelDTO = new HotelDTO()
							if (list.size() == 2){
								flag1 = true
								break
							}
						}
					}
				}
			}
			
			if (flag1){
				Node node
				String responseBody
		
//				def fileContents = helper.addMultipleBookingItemRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId(), 
//																							successHotelDTO.getItemCity(), successHotelDTO.getItemCode(), successHotelDTO.getCategoryId(),
//																							anotherSuccessHotelDTO.getItemCity(), anotherSuccessHotelDTO.getItemCode(), anotherSuccessHotelDTO.getCategoryId())
				
				def fileContents = helper.addMultipleBookingItemRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId(), 
																							list.get(0).getItemCity(), list.get(0).getItemCode(), list.get(0).getCategoryId(),
																							list.get(1).getItemCity(), list.get(1).getItemCode(), list.get(1).getCategoryId())
				println "Search Request Xml.. \n${fileContents}"
				node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'AddBookingItemRequest',FITSiteIdHeaderKey,SITEID)
				responseBody = XmlUtil.serialize(node)
				logDebug("Search Response Xml..\n${responseBody}")
		
				if (responseBody.contains("<ErrorId>")){
					softAssert.assertTrue(false,"Received a error response for multiple item addbooking request")
				}else{
					def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0.. respBookingRefsize)
					{
						if (respBookingRef[i] == null) { continue }
						if(respBookingRef[i].@ReferenceSource.equals("client")) {
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
						}
			
						if(respBookingRef[i].@ReferenceSource.equals("api")) {
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
							break
						}
					}
					
					if(result){
						addBookingMultipleItemSuccess = true
					}
					softAssert.assertTrue(result==true, "ReferenceSource is not client")
			
					def roomCategoryId  = node.ResponseDetails.BookingResponse.BookingItems.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id.toString()
					softAssert.assertTrue(roomCategoryId.contains(successHotelDTO.getCategoryId().toString()), "Room Category Id does not match")
				}
			}else{
				softAssert.assertTrue(false,"Can't find the valid hotel to add as a second item to the request")
			}
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "ABIR_MulitpItem_SingleRoom_TB_1Extra_1Cot_HBG - AddBookingItemRequest"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 7 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingMultipleItemSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.bookingItemPriceBreakdownFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'BookingItemPriceBreakdown',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
	
			def expectedValue = successHotelDTO.getItemCity().toString() +"|"+ successHotelDTO.getItemCode().toString()
	
			def respBookingRef = node.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("client")) {
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("api")) {
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
					break
				}
			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
	
			def roomCategoryId  = node.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id.toString()
//			softAssert.assertTrue(roomCategoryId.contains(successHotelDTO.getCategoryId().toString()), "Room Category Id does not match")
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingItemRequest for multiple items is failied to return valid response")
		}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - BookingItemPriceBreakdown"
	}

	@Unroll
	def "Verify RoomTypeBooking test 8 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingMultipleItemSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.searchBookingItemFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId().toString())
			println "SearchBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("SearchBookingItemRequest Response Xml..\n${responseBody}")
	
			def expectedValue = "Confirmed|1,2,3"
	
			def respBookingRef = node.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("client")) {
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("api")) {
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
					break
				}
			}
			
			def bookingItemResponse = node.ResponseDetails.SearchBookingItemResponse.BookingItems.BookingItem
			
	//		softAssert.assertTrue(bookingItemResponse.size()==3, "Booking item count not matched")
			
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingItemRequest for multiple items is failied to return valid response")
		}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - SearchBookingItem"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 9 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if(addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.chargeConditionBookingFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "ChargeConditionBookingRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'ChargeConditions_Booking',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("ChargeConditionBookingRequest Response Xml..\n${responseBody}")
	
	
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			
			def chargeConditions = node.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition
			softAssert.assertTrue(chargeConditions != null, "ChargeCondition in response is not returned")
			softAssert.assertTrue(chargeConditions.size() == 2, "Response not contains 2 charge conditions for the given booking item")
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - ChargeCondition_Booking"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 10 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if(addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.modifyBookingRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "ModifyBookingRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'ModifyBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("ModifyBookingRequest Response Xml..\n${responseBody}")
	
			def expectedValue = ""
	
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if (respBookingRef[i] == null) { continue }
				if(respBookingRef[i].@ReferenceSource.equals("client")) {
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i].@ReferenceSource.equals("agent")) {
					softAssert.assertTrue(respBookingRef[i].text().equals("JMB"), "Booking agent ref does not match")
				}
	
				if(respBookingRef[i].@ReferenceSource.equals("api")) {
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
					break
				}
			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
	
			def pax1 = node.ResponseDetails.BookingResponse.PaxNames.PaxName[0].text()
			softAssert.assertTrue(pax1.equals("JESS TEST"), "Pax 1 first and surname not changed")
	
			def pax2 = node.ResponseDetails.BookingResponse.PaxNames.PaxName[1].text()
			softAssert.assertTrue(pax2.equals("TETS 2"), "Pax 2 first and surname not changed")
	
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"

		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - MBR (change AgentRef)"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 11 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingMultipleItemSuccess){
			
			Node node
			String responseBody
	
			def fileContents = helper.modifyBookingItemRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId(), successHotelDTO.getCategoryId())
			println "ModifyBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'ModifyBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("ModifyBookingItemRequest Response Xml..\n${responseBody}")
	
			softAssert.assertTrue((responseBody.contains("Cannot modify Booking Item") || responseBody.contains("Item is not allowed to be amended")), "Response xml is not matched with the expected xml")
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingItemRequest for multiple items is failied to return valid response")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - MBIR"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 12 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingSuccess){
			
			Node node
			String responseBody
	
			def fileContents = helper.cancelBookingItemRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "CancelBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'CancelBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("CancelBookingItemRequest Response Xml..\n${responseBody}")
	
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("client")) {
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("api")) {
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
					break
				}
			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
	
			def respBookingItem = node.ResponseDetails.BookingResponse.BookingItems.BookingItem
			def itemReference = respBookingItem[0].ItemReference[0].text()
			def itemStatus = respBookingItem[0].ItemStatus[0].text()
			
			softAssert.assertTrue(itemStatus=="Cancelled", "Response xml ItemStatus is not matched with expected value")
			softAssert.assertTrue(itemReference=="1", "Response xml ItemReference not matched with the expected value")
		
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingItemRequest for multiple items is failied to return valid response")
		}	
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - CancelBookingItemRequest"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 13 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingMultipleItemSuccess){
			
			Node node
			String responseBody
	
			def fileContents = helper.searchBookingItemFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId().toString())
			println "SearchBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("SearchBookingItemRequest Response Xml..\n${responseBody}")
	
			def expectedValue = "Confirmed|1,2,3"
	
			def respBookingRef = node.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("client")) {
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("api")) {
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
					break
				}
			}
			
			def bookingItemResponse = node.ResponseDetails.SearchBookingItemResponse.BookingItems.BookingItem
			
			softAssert.assertTrue(bookingItemResponse.size()==2, "Booking item count not matched")
			
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingItemRequest for multiple items is failied to return valid response")
		}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - SearchBookingItem  (after 1 item cancelled)"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 14 || #testDesc"(){
		println("Test Running ...$testDesc")
		
		if (addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.cancelBookingRequestFor_1DB_2Cot_HBG(Sheet_NAME_1DB_2Cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "CancelBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'CancelBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("CancelBookingItemRequest Response Xml..\n${responseBody}")
	
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if (respBookingRef[i] == null) { continue }
				if(respBookingRef[i].@ReferenceSource.equals("client")) {
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i].@ReferenceSource.equals("api")) {
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
					break
				}
			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
	
			def respBookingItem = node.ResponseDetails.BookingResponse.BookingItems.BookingItem
			
			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"X",3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"),"Booking status is not cancelled in CBS after 3 retries")
	
			for (int j=0;j<respBookingItem.size();j++){
				def itemReference = respBookingItem[j].ItemReference[0].text()
				def itemStatus = respBookingItem[j].ItemStatus[0].text()
				if (!(itemStatus == "Cancelled" || itemStatus == "Rejected")){
					softAssert.assertTrue(false, "Response xml ItemStatus is not matched with expected value")
				}
			}
		
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "1DB_2Cot_HBG - CancelBookingRequest"
	}
}


class RoomTypeBookingTest_1DB_2Cot_HBG_C1237 extends RoomTypeBookingTest_1DB_2Cot_HBG {
	
	@Override
	String getClientId() {
		return "1237"
	}
}

class RoomTypeBookingTest_1DB_2Cot_HBG_C1238 extends RoomTypeBookingTest_1DB_2Cot_HBG {
	@Override
	String getClientId() {
		return "1238"
	}
}

class RoomTypeBookingTest_1DB_2Cot_HBG_C1239 extends RoomTypeBookingTest_1DB_2Cot_HBG {
	@Override
	String getClientId() {
		return "1239"
	}
}

class RoomTypeBookingTest_1DB_2Cot_HBG_C1244 extends RoomTypeBookingTest_1DB_2Cot_HBG {
	@Override
	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class RoomTypeBookingTest_1DB_2Cot_HBGSingleClient extends RoomTypeBookingTest_1DB_2Cot_HBG {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
