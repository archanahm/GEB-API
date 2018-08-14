package com.hbg.test.RoomTypeBooking

import org.testng.asserts.SoftAssert

import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.RoomTypeBookingHelper.RoomTypeBookingOneExtraOneCotHelper
import com.hbg.test.XSellBaseTest
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil

import groovy.xml.XmlUtil
import spock.lang.*

abstract class RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG extends XSellBaseTest {

	static final String Sheet_NAME_1TB_1Extra_1cot_HBG = "1TB_1Extra_1Cot_HBG"
	static final String EXCEL_SHEET = ConfigProperties.getValue("roomTypeBookingSheet")
	static final String SITEID = ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey = ConfigProperties.getValue("FITSiteIdHeaderKey")
	RoomTypeBookingOneExtraOneCotHelper helper = new RoomTypeBookingOneExtraOneCotHelper(EXCEL_SHEET,getClientId())

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
	
	@Unroll
	def "Verify RoomTypeBooking test 1 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		def cities = numOfCitiesList(3)
		for(k in 0..2){
			def fileContents_shpr = helper.searchHotelPriceRequestFor1TB_1Extra1Bed_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, cities[k])
			println "Search Request Xml.. \n${fileContents_shpr}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_shpr,'SearchHotelPriceRequest',FITSiteIdHeaderKey,SITEID)
			// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			softAssert.assertTrue(node.ResponseDetails.SearchHotelPriceResponse != 0)
			def hotelNode = node.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			println cities[k]+" city HBG hotels count : "+hList.size()
			if (hList.size() > 0){
				hotelList.addAll(hList)
			}
			
			List<HotelDTO> gtaHList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
			println cities[k]+" city GTA hotels count : "+gtaHList.size()
			if (gtaHList.size() > 0){
				gtaHotelList.addAll(gtaHList)
			}
		}
		println "total hotels count for all the cities : "+hotelList.size()
		softAssert.assertTrue(hotelList.size() > 0,"Available hotel not found")
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		1 		| "1TB_1ExtraB_1Cot_HBG - SearchHotelPriceRequest"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 2 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		
		def expectedValue
		boolean flag = false
		println "hotel size : "+hotelList.size()
		if(hotelList.size() > 0) {
			for (int i=0;i<hotelList.size();i++){
				HotelDTO hDTO = hotelList.get(i)
				
				def fileContents = helper.hotelPriceBreakdownRequestFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
				println "Search Request Xml.. \n${fileContents}"
				node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'HotelPriceBreakdownRequest',FITSiteIdHeaderKey,SITEID)
				responseBody = XmlUtil.serialize(node)
				logDebug("Search Response Xml..\n${responseBody}")
				if (responseBody.contains("ErrorId") || !responseBody.contains("<PriceRange>")){
					continue
				}else{
					expectedValue = hDTO.getItemCity().toString() +"|"+ hDTO.getItemCode().toString()+"|"+hDTO.getCategoryId().toString()
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
				def actualValue = itemCityCode+"|"+itemCode+"|"+roomCategoryId
		
		
				String childRoomCategoryId = hotelRoom[1].@Id.toString()
				def childActualValue = itemCityCode+"|"+itemCode+"|"+childRoomCategoryId
				String childHotelRoomCode = hotelRoom[1].@Code.toString()
		
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
				softAssert.assertTrue(node.ResponseDetails.HotelPriceBreakdownResponse != 0)
//				softAssert.assertTrue(expectedValue==actualValue, "RoomCategoryId : actual value not matching with expected value")
				softAssert.assertTrue(childHotelRoomCode=="CH", "No breakdown for child in the response")
//				softAssert.assertTrue(childActualValue==actualValue, "Child RoomCategoryId : actual value not matching with expected value")
			}else{
				softAssert.assertTrue(false, "HotelPriceBreakdownRequest failed to get valid response")
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		2 		| "1TB_1ExtraB_1Cot_HBG - HotelPriceBreakdownRequest"
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
		if(hotelList.size() > 0 ) {
			for (int i=0;i<hotelList.size();i++){
				HotelDTO hDTO = hotelList.get(i)
				bookingReferenceNum = helper.getBookingReference()
				def fileContents = helper.addBookingRequestFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
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
					if(respBookingRef[i].@ReferenceSource.equals("client")){
						result = true
						println "111:"+respBookingRef[i].text()
						println "222:"+successHotelDTO.getBookingReference()
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
					}
		
					if(respBookingRef[i].@ReferenceSource.equals("api")){
						result = true
						successHotelDTO.setBookingId(respBookingRef[2].text())
						break
					}
				}
				softAssert.assertTrue(result==true, "ReferenceSource is not client")
		
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			}else{
				softAssert.assertTrue(false, "AddBookingRequest failed to get valid response")
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		3 		| "1TB_1ExtraB_1Cot_HBG - AddBookingRequest"
	}
	
	/*
	@Unroll
	def "Verify RoomTypeBooking test 4 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		List<String> bookingRefList = new ArrayList<String>()
		List<String> bookingIdList = new ArrayList<String>()

		if(hotelList.size() > 0) {
			def fileContents = helper.searchBookingRequest1For_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum)
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
	
			def respBookingRef = node.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			boolean flag = false
		
		
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(flag, "SearchBookingRequest with Date Range returned errors in the response")
			}else {
				for (i in 0.. respBookingRefsize-1) {
					
					if (respBookingRef[i].@ReferenceSource.equals("client")) {
						bookingRefList.add(respBookingRef[i].text())
					}
		
					if (respBookingRef[i].@ReferenceSource.equals("api")) {
						bookingIdList.add(respBookingRef[i].text())
					}
		
					if (i == respBookingRefsize-1){break}
				}
		
				softAssert.assertTrue(bookingRefList.contains(successHotelDTO.getBookingReference().toString()), "Booking reference number does not exist")
				softAssert.assertTrue(bookingIdList.contains(successHotelDTO.getBookingId().toString()), "Booking Id does not exist")
		
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
				softAssert.assertTrue(node.ResponseDetails.SearchBookingResponse != 0)
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		4 		| "1TB_1ExtraB_1Cot_HBG - SearchBookingRequest With Date Range"
	}

	@Unroll
	def "Verify RoomTypeBooking test 5 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		if(hotelList.size() > 0) {
			def fileContents = helper.searchBookingRequest2For_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
	
			def respBookingRef = node.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(result, "SearchBookingRequest with Booking Id returned errors in the response")
			}else {
				for (i in 0.. respBookingRefsize)
				{
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
		
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
				softAssert.assertTrue(node.ResponseDetails.SearchBookingResponse != 0)
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		5 		| "1TB_1ExtraB_1Cot_HBG - SBR with BookingId"
	}
	

	
	@Unroll
	def "Verify RoomTypeBooking test 6 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		if(hotelList.size() > 0) {
			def fileContents = helper.addBookingItemRequestFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId(), successHotelDTO.getItemCity(), successHotelDTO.getItemCode(), successHotelDTO.getCategoryId())
			println "Search Request Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'AddBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			logDebug("Search Response Xml..\n${responseBody}")
	
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(result, "AddBookingItemRequest with 1 Cot returned errors in the response")
			}else {
				for (i in 0.. respBookingRefsize)
				{
					if(respBookingRef[i].@ReferenceSource.equals("client")) {
						result=true
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
					}
		
					if(respBookingRef[i].@ReferenceSource.equals("api")) {
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
						break
					}
				}
			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
	
			def roomCategoryId  = node.ResponseDetails.BookingResponse.BookingItems.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id.toString()
			softAssert.assertTrue(roomCategoryId.contains(successHotelDTO.getCategoryId().toString()), "Room Category Id does not match")
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		6 		| "ABIR_TB_1Extra_1Cot_HBG - AddBookingItemRequest with 1 Cot"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 7 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody

		if(hotelList.size() > 0) {
			def fileContents = helper.searchBookingItemFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId().toString())
			println "SearchBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			println "SearchBookingItemRequest Response Xml..\n${responseBody}"
	
			def expectedValue = "Confirmed|1,2,3"
	
			def respBookingRef = node.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(result, "SearchBookingItem returned errors in the response")
			}else {
				for (i in 0.. respBookingRefsize)
				{
					if(respBookingRef[i].@ReferenceSource.equals("client")) {
						result=true
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
					}
		
					if(respBookingRef[i].@ReferenceSource.equals("api")) {
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
						break
					}
				}
				def bookingItemResponse = node.ResponseDetails.SearchBookingItemResponse.BookingItems.BookingItem
				
				softAssert.assertTrue(bookingItemResponse.size()==2, "Booking item count not matched")
				
				softAssert.assertTrue(result==true, "ReferenceSource is not client")
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		7 		| "1TB_1ExtraB_1Cot_HBG - SearchBookingItem"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 8 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody

		if(hotelList.size() > 0) {
			def fileContents = helper.chargeConditionBookingFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "ChargeConditionBookingRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'ChargeConditions_Booking',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			println "ChargeConditionBookingRequest Response Xml..\n${responseBody}"
	
	
			// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			def result = false
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(result, "ChargeCondition_Booking returned errors in the response")
			}else {
				def chargeConditions = node.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition
				softAssert.assertTrue(chargeConditions != null, "ChargeCondition in response is not returned")
				softAssert.assertTrue(chargeConditions.size() == 2, "Response not contains 2 charge conditions for the given booking item")
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		8 		| "1TB_1ExtraB_1Cot_HBG - ChargeCondition_Booking"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 9 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		
		if(hotelList.size() > 0) {
			def fileContents = helper.modifyBookingRequestFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "ModifyBookingRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'ModifyBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			println "ModifyBookingRequest Response Xml..\n${responseBody}"
	
			def expectedValue = ""
	
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(result, "MBR (change AgentRef) returned errors in the response")
			}else {
				for (i in 0.. respBookingRefsize)
				{
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
		
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		9 		| "1TB_1ExtraB_1Cot_HBG - MBR (change AgentRef)"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 10 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody

		if(hotelList.size() > 0) {
			def fileContents = helper.modifyBookingItemRequestFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId(), successHotelDTO.getCategoryId())
			println "ModifyBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'ModifyBookingRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			println "ModifyBookingItemRequest Response Xml..\n${responseBody}"
			softAssert.assertTrue(responseBody.contains("<ErrorId>GRT1981</ErrorId>") || responseBody.contains("<ErrorId>ERR0155</ErrorId>"), "Failed : Expected response with ErrorId GRT1981/ERR0155 but response not contains any of these ErrorId")
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		10 		| "1TB_1ExtraB_1Cot_HBG - MBIR"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 11 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody

		if(hotelList.size() > 0) {
			def fileContents = helper.cancelBookingItemRequestFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "CancelBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'CancelBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			println "CancelBookingItemRequest Response Xml..\n${responseBody}"
	
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(result, "CancelBookingItemRequest returned errors in the response")
			}else {
				for (i in 0.. respBookingRefsize)
				{
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
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
		
				def respBookingItem = node.ResponseDetails.BookingResponse.BookingItems.BookingItem
				def itemReference = respBookingItem[0].ItemReference[0].text()
				def itemStatus = respBookingItem[0].ItemStatus[0].text()
				
				softAssert.assertTrue(itemStatus=="Cancelled", "Response xml ItemStatus is not matched with expected value")
				softAssert.assertTrue(itemReference=="1", "Response xml ItemReference not matched with the expected value")
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		11 		| "1TB_1ExtraB_1Cot_HBG - CancelBookingItemRequest"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 12 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody

		if(hotelList.size() > 0) {
			def fileContents = helper.searchBookingItemFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId().toString())
			println "SearchBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			println "SearchBookingItemRequest Response Xml..\n${responseBody}"
	
			def expectedValue = "Confirmed|1,2,3"
	
			def respBookingRef = node.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(result, "SearchBookingItem  (after 1 item cancelled) returned errors in the response")
			}else {
				for (i in 0.. respBookingRefsize)
				{
					if(respBookingRef[i].@ReferenceSource.equals("client")) {
						result=true
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
					}
		
					if(respBookingRef[i].@ReferenceSource.equals("api")) {
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")
						break
					}
				}
				
				def bookingItemResponse = node.ResponseDetails.SearchBookingItemResponse.BookingItems.BookingItem
				
				softAssert.assertTrue(bookingItemResponse.size()==1, "Booking item count not matched")
				
				softAssert.assertTrue(result==true, "ReferenceSource is not client")
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		12 		| "1TB_1ExtraB_1Cot_HBG - SearchBookingItem  (after 1 item cancelled)"
	}
	
	@Unroll
	def "Verify RoomTypeBooking test 13 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody

		if(hotelList.size() > 0) {
			def fileContents = helper.cancelBookingRequestFor_1TB_1ExtraBed_1Cot_HBG(Sheet_NAME_1TB_1Extra_1cot_HBG, rowNum, successHotelDTO.getBookingId())
			println "CancelBookingItemRequest Xml.. \n${fileContents}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'CancelBookingItemRequest',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node)
			println "CancelBookingItemRequest Response Xml..\n${responseBody}"
	
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			if (responseBody.contains("ErrorId")) {
				softAssert.assertTrue(result, "CancelBookingRequest returned errors in the response")
			}else {
				for (i in 0.. respBookingRefsize)
				{
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
				// softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
		
				def respBookingItem = node.ResponseDetails.BookingResponse.BookingItems.BookingItem
				
				for (int j=0;j<respBookingItem.size();j++){
					def itemReference = respBookingItem[j].ItemReference[0].text()
					def itemStatus = respBookingItem[j].ItemStatus[0].text()
					if (!(itemStatus == "Cancelled" || itemStatus == "Rejected")){
						softAssert.assertTrue(false, "Response xml ItemStatus is not matched with expected value")
					}
				}
			}
		}else {
			softAssert.assertTrue(hotelList.size() > 0, "Could not complete the request - No HBG hotels returned for SearchHotelPriceRequest ")
		}
		
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc
		13 		| "1TB_1ExtraB_1Cot_HBG - CancelBookingRequest"
	}*/
}

class RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG_C1237 extends RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG {
	
	@Override
	String getClientId() {
		return "1237"
	}
}

class RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG_C1238 extends RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG {
	
	@Override
	String getClientId() {
		return "1238"
	}
}

class RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG_C1239 extends RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG {
	
	@Override
	String getClientId() {
		return "1239"
	}
}

class RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG_C1244 extends RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG {
	
	@Override
	String getClientId() {
		return "1244"
	}
}
@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBGSingleClient extends RoomTypeBookingTest_1TB_1ExtraB_1Cot_HBG {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
