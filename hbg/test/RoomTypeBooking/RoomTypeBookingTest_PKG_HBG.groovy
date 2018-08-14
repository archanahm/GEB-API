package com.hbg.test.RoomTypeBooking

import groovy.xml.XmlUtil

import org.testng.asserts.SoftAssert

import spock.lang.*

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.RoomTypeBookingHelper.RoomTypeBookingHelper_PKG_HBG
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil

abstract class RoomTypeBookingTest_PKG_HBG extends XSellBaseTest  {

	static final String Sheet_NAME_PKG_HBG = "PKG_HBG"
	static final String EXCEL_SHEET = ConfigProperties.getValue("roomTypeBookingSheet")
	static final String SITEID = ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey = ConfigProperties.getValue("FITSiteIdHeaderKey")
	RoomTypeBookingHelper_PKG_HBG helper = new RoomTypeBookingHelper_PKG_HBG(EXCEL_SHEET, getClientId())

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
	AS400DatabaseUtil dbconn= new AS400DatabaseUtil()
	
	@Shared
	BookingStatus bookingStatus
	
	@Shared
	boolean addBookingSuccess = false
	
	@Shared
	boolean addBookingItemSuccess = false
	
	@Unroll
	def "Verify RoomTypeBooking test #1 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		def cities = numOfCitiesList(10)
		for(k in 0.. cities.size()-1){
			def fileContents_shpr = helper.searchHotelPriceRequestFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, cities[k])
			println "Search Request Xml.. \n${fileContents_shpr}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_shpr,'SearchHotelPriceRequest',FITSiteIdHeaderKey,SITEID)
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			softAssert.assertTrue(node.ResponseDetails.SearchHotelPriceResponse != 0)
			def hotelNode = node.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
			List<HotelDTO> hList = RequestNewHelper.getSearchAmandableHBGHotels(hotelNode, 0)
			println cities[k]+" city hotels count : "+hList.size()
			if (hList.size() > 0){
				hotelList.addAll(hList)
			}
			
			List<HotelDTO> gtaHList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
			println cities[k]+" city hotels count : "+gtaHList.size()
			if (gtaHList.size() > 0){
				gtaHotelList.addAll(gtaHList)
			}
		}
		
		println "total hotels count for all the cities : "+hotelList.size()
		softAssert.assertTrue(hotelList.size() > 0,"Available hotel not found")
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - SearchHotelPriceRequest"
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
			
			def fileContents = helper.hotelPriceBreakdownRequestFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'HotelPriceBreakdownRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
			if (responseBody.contains("ErrorId") || !responseBody.contains("<PriceRange>")){
				continue
			}else{
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
			String roomCategoryId = hotelRoom[0].@Id.toString()
			def actualValue = itemCityCode+"|"+itemCode
	
			softAssert.assertTrue(node.ResponseDetails.HotelPriceBreakdownResponse != 0)
			softAssert.assertTrue(expectedValue==actualValue, "CityCode|ItemCode : actual value not matching with expected value")
		}else{
			softAssert.assertTrue(false, "HotelPriceBreakdownRequest failed to get valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - HotelPriceBreakdownRequest"
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
			def fileContents = helper.addBookingRequestFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'AddBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
			if ((responseBody.contains("ErrorId") && (responseBody.contains("Hotel is invalid")))|| !responseBody.contains("<BookingReference ReferenceSource=")){
				continue
			}else{
				expectedValue = hDTO.getItemCity().toString() +"|"+ hDTO.getItemCode().toString()+"|Confirmed"
				flag = true
				successHotelDTO = hDTO
				successHotelDTO.setBookingReference(bookingReferenceNum)
				break
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
					break
				}
			}
			
			if(result){
				addBookingSuccess = true
			}
			
			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 3 retries")
	
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
		}else{
			softAssert.assertTrue(false, "AddBookingRequest failed to get valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - AddBookingRequest"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 4 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.searchBookingRequest1For_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId())
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
	
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			softAssert.assertTrue(node.ResponseDetails.SearchBookingResponse != 0)
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}

		softAssert.assertAll()
		println "\n\n" 

		where:
		rowNum | testDesc
		1 | "PKG_HBG - SBR with BookingId"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 5 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.bookingItemPriceBreakdownFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId())
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
		
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - BookingItemPriceBreakdown"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 6 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingSuccess){
			Node node
			String responseBody
			HotelDTO hDTO = null
			for (int i=0;i<hotelList.size();i++){
				hDTO = hotelList.get(i)
				if (hDTO.getCategoryId() != successHotelDTO.getCategoryId()){
					break
				}
			}
			def fileContents = helper.addBookingItemRequestFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'AddBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
	
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
			
			if (!result){
				softAssert.assertTrue(false, "Failed : Either ReferenceSource is not client or received an Error response")
			}else{
				def roomCategoryId  = node.ResponseDetails.BookingResponse.BookingItems.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id.toString()
				softAssert.assertTrue(roomCategoryId.contains(successHotelDTO.getCategoryId().toString()), "Room Category Id does not match")
				if (roomCategoryId.contains(successHotelDTO.getCategoryId().toString())){
					addBookingItemSuccess = true
				}
			}
			
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "ABIR_PKG_HBG - AddBookingItemRequest"
	}
	

	@Unroll
	def "Verify RoomTypeBooking test 8 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingItemSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.searchBookingItemFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId().toString())
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
			
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
		
		}else{
			softAssert.assertTrue(false, "Can't execute this test as addBookingItemSuccess is failied to return valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - SearchBookingItem"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 10 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.chargeConditionBookingFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId())
			println "ChargeConditionBookingRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'ChargeConditions_Booking',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("ChargeConditionBookingRequest Response Xml..\n${responseBody}")
	
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			def chargeConditions = node.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition
			softAssert.assertTrue(chargeConditions != null, "ChargeCondition in response is not returned")
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - ChargeCondition_Booking"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 11 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.modifyBookingRequestFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId())
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
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("client")) {
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}
	
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("agent")) {
					softAssert.assertTrue(respBookingRef[i].text().equals("JMB"), "Booking agent ref does not match")
				}
	
				if(respBookingRef[i] != null && respBookingRef[i].@ReferenceSource.equals("api")) {
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
					break
				}
			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
	
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
		
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - MBR (change AgentRef)"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 12 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.modifyBookingItemRequestFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId(), successHotelDTO.getCategoryId())
			println "ModifyBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'ModifyBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("ModifyBookingItemRequest Response Xml..\n${responseBody}")
	
			softAssert.assertTrue((responseBody.contains("Cannot modify Booking Item") || responseBody.contains("Item is not allowed to be amended")), "Response xml is not matched with the expected xml")
		
		}else{
			softAssert.assertTrue(false, "Can't execute this test as AddBookingRequest is failied to return valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - MBIR"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 13 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingItemSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.cancelBookingItemRequestFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId())
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
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
	
			def respBookingItem = node.ResponseDetails.BookingResponse.BookingItems.BookingItem
			def itemReference = respBookingItem[0].ItemReference[0].text()
			def itemStatus = respBookingItem[0].ItemStatus[0].text()
			
			softAssert.assertTrue(itemStatus=="Cancelled", "Response xml ItemStatus is not matched with expected value")
			softAssert.assertTrue(itemReference=="8", "Response xml ItemReference not matched with the expected value")
		
		}else{
			softAssert.assertTrue(false, "Can't execute this test as addBookingItemSuccess is failied to return valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - CancelBookingItemRequest"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 14 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingItemSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.searchBookingItemFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId().toString())
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
			
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
		
		}else{
			softAssert.assertTrue(false, "Can't execute this test as addBookingItemSuccess is failied to return valid response")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "PKG_HBG - SearchBookingItem  (after 1 item cancelled)"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 15 || #testDesc"(){
		println("Test Running ...$testDesc")
		if (addBookingSuccess){
			Node node
			String responseBody
	
			def fileContents = helper.cancelBookingRequestFor_PKG_HBG(Sheet_NAME_PKG_HBG, rowNum, successHotelDTO.getBookingId())
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
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
	
			def respBookingItem = node.ResponseDetails.BookingResponse.BookingItems.BookingItem
			
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
		1 | "PKG_HBG - CancelBookingRequest"
	}
}

class RoomTypeBookingTest_PKG_HBG_C1237 extends RoomTypeBookingTest_PKG_HBG {
	
	@Override
	String getClientId() {
		return "1237"
	}
}

class RoomTypeBookingTest_PKG_HBG_C1238 extends RoomTypeBookingTest_PKG_HBG {
	@Override
	String getClientId() {
		return "1238"
	}
}

class RoomTypeBookingTest_PKG_HBG_C1239 extends RoomTypeBookingTest_PKG_HBG {
	@Override
	String getClientId() {
		return "1239"
	}
}

class RoomTypeBookingTest_PKG_HBG_C1244 extends RoomTypeBookingTest_PKG_HBG {
	@Override
	String getClientId() {
		return "1244"
	}
}
@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class RoomTypeBookingTest_PKG_HBGSingleClient extends RoomTypeBookingTest_PKG_HBG {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
