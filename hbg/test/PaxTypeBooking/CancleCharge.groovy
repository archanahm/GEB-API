package com.hbg.test.PaxTypeBooking

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll

abstract class CancelCharge extends XSellBaseTest{


	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("paxexcelSheet"))
	static final String SHEET_NAME_SEARCH="CancelCharge"


	static final String SITEID= ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey= ConfigProperties.getValue("FITSiteIdHeaderKey")
	SearchHotelPricePaxHelper helper=new SearchHotelPricePaxHelper(EXCEL_SHEET,getClientId())

	int rowNum
	String sheet,testDesc,Data
	private static def xmlParser = new XmlParser()
	SoftAssert softAssert= new SoftAssert()
	XMLValidationUtil XmlValidate = new XMLValidationUtil()
	int searchRowNum
	def addRef = ""

	@Shared
	def itemDetails = [:]
	@Shared
	def itemDetailsGta = [:]
	int hotelCount =0
	def flag = false
	Map itemMap=null
	Map itemMapGta=null
	Map fields = null
	static final String CITY_GTA= ConfigProperties.getValue("City_List")
	def cities_GTA = CITY_GTA.split(",")
	BookingStatus bookingStatus = null
	@Shared
	List cities

	@Shared
	List<HotelDTO> hotelListHBG = new ArrayList<HotelDTO>()

	@Shared
	List<HotelDTO> hotelListGTA = new ArrayList<HotelDTO>()

	@Shared
	HotelDTO successHotelDTO = null

	@Shared
	HotelDTO gtaSuccessHotelDTO = null

	AS400DatabaseUtil dbconn= new AS400DatabaseUtil()

	@Shared
	Map hotelListHBGmap = [:]

	@Shared
	String fristBookingGross
	@Shared
	String secBookingGross
	@Shared
	String thirdBookingGross

	@Unroll

	/**
	 * search for the HBG item untill found any AVAILABLE item
	 */
	def "1. Verify Avilabel HBG Hotel #1 || #testDesc"(){
		println " serach for avilable HBG hotel "
		Node node_search
		String responseBody_search
		cities=numOfCitiesList(8)

		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchWithCancelChargeXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search Request for SearchChargeConditions.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug("Response Xml.for Search ChargeConditions\n${responseBody_search}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList
			if (hotelNode != 0) {
				hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
			}

			if (hList.size() > 0){
				for (HotelDTO dto : hList){
					def rNode = dto.getRoomCategoryNode()
					def chargeCnd = rNode.ChargeConditions.size()
					if (chargeCnd != 0 ){
						hotelListHBG.add(dto)
					}
				}
			}
		}

		softAssert.assertTrue(hotelListHBG.size() > 0,"HBG Hotels not found for Search Charge Conditions")

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc 				| searchRowNum
		1		|"serach for avilable HBG hotel with Charge Condation True  " 		| 1
	}

	/**
	 * search for the GTA item untill found any avilable item
	 */
	@Unroll
	def "Verify Search for avilable GTA hotel #2 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search

			for (k in 0..cities_GTA.size() - 1) {
				def fileContents_Search = helper.getPaxSearchWithCancelChargeXml(SHEET_NAME_SEARCH, searchRowNum, cities_GTA[k])
				println "Search REquest Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")


				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList
				if (hotelNode != 0) {
					hList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
				}

				if (hList.size() > 0){
					for (HotelDTO dto : hList){
						def rNode = dto.getRoomCategoryNode()
						def chargeCnd = rNode.ChargeConditions.size()
						if (chargeCnd != 0 ){
							hotelListGTA.add(dto)
						}
					}
				}
			}


			println "total hotels count for all the cities : " + hotelListGTA.size()
			softAssert.assertTrue(hotelListGTA.size() > 0, "Available hotel not found")

		softAssert.assertAll()

		where:
		searchRowNum | testDesc
		12|"search for avilable HBG hotel and save the item details"
	}

	@Unroll
	/**
	 * AddBookingRequest
	 * create booking for HBG item verify boooking status
	 * @return
	 */

	def "Verify create  booking request  test #3|| #testDesc"(){
		println("Test Running ...$testDesc")
		itemDetails.put('randombookingRefNum',helper.getBookingReference())
		def bookingReferenceNum = null
		def expectedValue
		def result = false
		Node node_booking
		String responseBody_booking
		if(hotelListHBG.size() != 0) {
		println "Hotel size : " + hotelListHBG.size()
		for (k in  0 .. hotelListHBG.size()-1) {
			HotelDTO hDTO = hotelListHBG.get(k)
			bookingReferenceNum = helper.getBookingReference()
			def fileContents_booking = helper.getBookPaxXml(SHEET_NAME_SEARCH, rowNum, bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
			println "Request Xml...\n${fileContents_booking}"
			 node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
			responseBody_booking = XmlUtil.serialize(node_booking)
			logDebug("Search Response Xml..\n${responseBody_booking}")

			// Verify the resonse "response code" and "confirmation status"
			expectedValue = "1,2"
			if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
				successHotelDTO = hDTO
				successHotelDTO.setBookingReference(bookingReferenceNum)
				def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
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
				softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
				break
			} else if (helper.getExpectedResult().equals("Failure")) {
				helper.validateFailureResponse(responseBody_booking)
			} else if (helper.getExpectedResult().equals("Empty")) {
				helper.validateXmlWithEmptyResponse(responseBody_booking)
			}
			 else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
			def errorStr = node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
			println "ERROR IN RESPONSE " + errorStr
			errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
			println "ERROR IN RESPONSE " + errorStr
			softAssert.assertFalse(node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
		} else if (node_booking.ResponseDetails.Errors.Error.size() > 0) {
			def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
			print "ERROR IN RESPONSE " + Errostring
			softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
		}
		}

		if(result == true){
			fristBookingGross=node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem.ItemPrice.@Gross[0]
			String roomId = node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem.HotelItem.HotelPaxRoom.@Id[0]
			softAssert.assertTrue(roomId.equals(successHotelDTO.getCategoryId().toString()), "Room ID  not matching after booking")
			helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}
		}
		else{
			softAssert.assertTrue(hotelListHBG.size()  != 0, "NO HBG Hotels found")
		}

		softAssert.assertAll()
		println "\n\n"


		where:
		rowNum | testDesc
		2| " Verify add booking request  - Able to book HBG hotel  "


	}
/**
 * Search booking - search for any item after booking verify the status of the booking in CBS
 */
	@Unroll
	def "Verify Search for booking test #4|| #testDesc"(){
		println("Test Running ...$testDesc")
		def result = false
		if(successHotelDTO != null ) {

			def fileContents_bookingref = helper.getBookingRequest(SHEET_NAME_SEARCH, searchRowNum, successHotelDTO.getBookingId())
			println "Request Xml...\n${fileContents_bookingref}"
			Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
			String responseBody_booking = XmlUtil.serialize(node_booking)
			logDebug("Search Response Xml..\n${responseBody_booking}")
			def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
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
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()
		where:
	 	testDesc | searchRowNum
		" Search for booking- With HBG hotel " | 1
	}

/**
 * AddBookingItemRequest
 * Add new item to already created booking and verify the booking status
 * @return
 */
	@Unroll
	def "Verify Adding single item to booking test #5|| #testDesc"(){
		println("Test Running ...$testDesc")
		String responseBody_booking
		def expectedValue
		def result = false
		if(successHotelDTO != null ) {
			cities=numOfCitiesList(6)
			List<HotelDTO> hotelListHBGLocal = new ArrayList<HotelDTO>()
			for(k in 0.. cities.size()-1 ){
				def fileContents_Search = helper.getPaxSearchWithPkgRatesXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
				println "Search Request for SearchChargeConditions.. \n${fileContents_Search}"
				def node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				def responseBody_search = XmlUtil.serialize(node_search)

				logDebug("Response Xml.for Search ChargeConditions\n${responseBody_search}")
				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList

				if (hotelNode != 0) {
					hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
				}

				if (hList.size() > 0){
					for (HotelDTO dto : hList){
						def rNode = dto.getRoomCategoryNode()
						def pkgRate = rNode.ItemPrice.@PackageRate[0]
						if (pkgRate != null && pkgRate == "true"){
							hotelListHBGLocal.add(dto)
						}
					}
				}
			}

			if(hotelListHBGLocal.size() != 0) {

				for (k in  0 .. hotelListHBGLocal.size()-1) {
					HotelDTO hDTO = hotelListHBGLocal.get(k)
					def fileContents_bookingref = helper.getAddBookingItemRequest(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
					println "Request Xml...\n${fileContents_bookingref}"
					Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
					responseBody_booking = XmlUtil.serialize(node_booking)
					logDebug("Search Response Xml..\n${responseBody_booking}")

					// Verify the resonse "response code" and "confirmation status"
					expectedValue = hDTO.getItemCity().toString() + "|" + hDTO.getItemCode().toString()
					if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
						def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
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
						secBookingGross=node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem.ItemPrice.@Gross[0]
						bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
						softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
						break

					} else if (helper.getExpectedResult().equals("Failure")) {
						helper.validateFailureResponse(responseBody_booking)
					} else if (helper.getExpectedResult().equals("Empty")) {
						helper.validateXmlWithEmptyResponse(responseBody_booking)
					} else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
						def errorStr = node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
						println "ERROR IN RESPONSE " + errorStr
						errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
						println "ERROR IN RESPONSE " + errorStr
						softAssert.assertFalse(node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
					} else if (node_booking.ResponseDetails.Errors.Error.size() > 0) {
						def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
						print "ERROR IN RESPONSE " + Errostring
						softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
					}
				}

					if (result == true) {


						helper.validateSuccessResponse(responseBody_booking, expectedValue)
					}
				}
		else{
			softAssert.assertTrue(hotelListHBGLocal.size()  != 0, "NO HBG Hotels found")
		}

		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}


		softAssert.assertAll();
		println "\n\n"


		where:

		rowNum|testDesc | searchRowNum
		3|" Add single item item to already created booking  -   HBG hotel  " | 12
	}



/**
 * AddBookingItemRequest
 * Add new GTA item to already created booking with 2pax
 * @return
 */
	@Unroll
	def "Verify add booking request with GTA items  #6 || #testDesc"(){
		println("Test Running ...$testDesc")

		def result = false
		def expectedValue
		String responseBody_booking
		if(successHotelDTO != null){
	for (k in  0 .. hotelListGTA.size()-1) {
		HotelDTO hDTO = hotelListGTA.get(k)
		def fileContents_booking = helper.getAddBookingItemRequest(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
		println "Request Xml...\n${fileContents_booking}"
		Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		 responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug("Search Response Xml..\n${responseBody_booking}")

		// Verify the response "response code" and "confirmation status" number of pax
		 expectedValue = hDTO.getItemCity().toString() + "|" + hDTO.getItemCode().toString()
		if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
			def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
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
			thirdBookingGross=node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem.ItemPrice.@Gross[0]
			bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
			break
		} else if (helper.getExpectedResult().equals("Failure")) {
			helper.validateFailureResponse(responseBody_booking)
		} else if (helper.getExpectedResult().equals("Empty")) {
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		} else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
			def errorStr = node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
			println "ERROR IN RESPONSE " + errorStr
			errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
			println "ERROR IN RESPONSE " + errorStr
			softAssert.assertFalse(node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
		} else if (node_booking.ResponseDetails.Errors.Error.size() > 0) {
			def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
			print "ERROR IN RESPONSE " + Errostring
			softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
		}

	}
	if(result == true){

		helper.validateSuccessResponse(responseBody_booking,expectedValue)

	}


		}
	else{
	softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc | searchRowNum
		4|"Add new GTA item to already created booking  : GTA item   " | 11
	}


	@Unroll
	def "Verify  booking item price break down test #7 || #testDesc"() {
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
		def fileContents_bookingref = helper.getBookingItemPriceBreakdownRequest(SHEET_NAME_SEARCH, searchRowNum, successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug("Search Response Xml..\n${responseBody_booking}")

		// Verify the resonse "response code" and "confirmation status"
		def expectedValue = successHotelDTO.getItemCity().toString() + "|" + successHotelDTO.getItemCode().toString()
		if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.BookingReference.size() > 0) {
			String grossAmnt=node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.ItemPrice.@Gross[0]
			softAssert.assertTrue(fristBookingGross.equals(grossAmnt), "Gross amount doesnt match in booking item price breakdown response")
			def respBookingRef = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
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


		} else if (helper.getExpectedResult().equals("Failure")) {
			helper.validateFailureResponse(responseBody_booking)
		} else if (helper.getExpectedResult().equals("Empty")) {
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}
		//validate room ID
		String roomId = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id[0].toString()
		//softAssert.assertTrue(roomId.equals(successHotelDTO.getCategoryId().toString()), "Booking reference ID does not match")

		helper.validateSuccessResponse(responseBody_booking, expectedValue)
	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll();
		println "\n\n"

		where:

		testDesc | searchRowNum
		" Search for booking item price break down pax  -   HBG and GTA items  " | 5
	}
	@Unroll
	def "Verify Charge Conditions of bookied items test #8|| #testDesc"(){

		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {

		def fileContents_bookingref=helper.getSearchChargeConditionsRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_SearchCondation =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_SearchCondation = XmlUtil.serialize(node_SearchCondation)
		logDebug("Search Response Xml..\n${responseBody_SearchCondation}")

		// Verify the resonse "search condation response" expacted value feccthed from excel

		if(helper.getExpectedResult().equals("Success") && node_SearchCondation.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.size() > 0){

			helper.validateSuccessResponse(responseBody_SearchCondation)

		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_SearchCondation)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_SearchCondation)
		}
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()
		println "\n\n"
		where:
		rowNum|testDesc | searchRowNum
		6| " Search for charge condition for booked item   -   HBG and GTA hotel booking id  " | 50

	}

	@Unroll
	def "Verify modify batch item in a booking #9|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.getModifyBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node responseBody = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref,'ModifyBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_Modify = XmlUtil.serialize(responseBody)
		logDebug("Search Response Xml..\n${responseBody_Modify}")
				if(helper.getExpectedResult().equals("Success")){

					def respBookingRef = responseBody.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0.. respBookingRefsize-1) {
						if(respBookingRef[i].@ReferenceSource.equals("agent")) {
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals("ChgAgentREF"), "agent details not updated")
							break
						}
					}
					softAssert.assertTrue(result==true, "ReferenceSource is not api")
				}
				else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("nova")){
					helper.validateFailureResponse(responseBody_Modify)
				}
				else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("legacy")){

					helper.validateFailureResponse(responseBody_Modify,"ErrorId","GRT1981")
				}
				else if(helper.getExpectedResult().equals("Empty")){
					helper.validateXmlWithEmptyResponse(responseBody_Modify)
				}
			}
			else{
				softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
			}

		softAssert.assertAll()
		println "\n\n"

		where:

		rowNum|testDesc | searchRowNum
		7|" Modify agent details In booking -   HBG item  " | 50
	}

	@Unroll
	def "Verify modify batch Request in a booking #10|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.getModifyBatchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node responseBody = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref,'ModifyBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_Modify = XmlUtil.serialize(responseBody)
		logDebug("Search Response Xml..\n${responseBody_Modify}")
				if(helper.getExpectedResult().equals("Success") && responseBody.ResponseDetails.BookingResponse.BookingReferences.size() >0 ){

					def respBookingRef = responseBody.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0.. respBookingRefsize-1) {
						if(respBookingRef[i].@ReferenceSource.equals("agent")) {
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals("ChgAgentREF"), "agent details not updated")
							break
						}
					}
					softAssert.assertTrue(result==true, "ReferenceSource is not api")

				}else if(helper.getExpectedResult().equals("Failure")){
					helper.validateFailureResponse(responseBody_Modify)
				}else if(helper.getExpectedResult().equals("Empty")){
					helper.validateXmlWithEmptyResponse(responseBody_Modify)
				}
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}



		softAssert.assertAll()
		println "\n\n"

		where:

		rowNum|testDesc | searchRowNum
		8|" Modify batch booking details In booking -   HBG item  " | 50
	}


	@Unroll
	def "Verify Cancel item in a booking #11|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.getCancelBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug("Search Response Xml..\n${responseBody_booking}")

		def expectedValue = "Cancelled"+"|Confirmed"
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
			String gorssAmtAfterCancel=node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem.ItemPrice.@Gross[0]
			softAssert.assertTrue(fristBookingGross.equals(gorssAmtAfterCancel), "Gross amount doesnt match after cancel")
			bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
			helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
			def errorStr = node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
			println "ERROR IN RESPONSE " + errorStr
			errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
			println "ERROR IN RESPONSE " + errorStr
			softAssert.assertFalse(node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
		}
		else if(node_booking.ResponseDetails.Errors.Error.size()> 0){
			def Errostring=node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
			print "ERROR IN RESPONSE " + Errostring
			softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size()> 0, "Error Returned in Booking Response " + Errostring)
		}
	}
	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}


		softAssert.assertAll()
		println "\n\n"
		where:

		rowNum|testDesc | searchRowNum
		9|" cancel an Item in booking verify Item status to be cancelled and booking status is confirmed  -   HBG item " | 50
	}

	@Unroll
	def "verify Search for the item after cancel #12|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.	getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug("Search Response Xml..\n${responseBody_booking}")

		//validate response
		//validate SearchBookingItemResponse returned
		if(helper.getExpectedResult().equals("Success")){
			def respBookingRef = node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
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
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",5)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
			}
		else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}

		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()
		where:

		rowNum|testDesc
		10|" Search for an item after cancel of any item  -   HBG item "

	}

	@Unroll
	def "Verify Cancel  entire booking #13|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
			def fileContents_bookingref=helper.getCancelBookingRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug("Search Response Xml..\n${responseBody_booking}")

		//	def expectedValue = "Cancelled"
			if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
			def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
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
				bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"X",3)
				softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"),"Booking status is not cancelled in CBS after 5 retries")

				softAssert.assertTrue(result==true, "ReferenceSource is not client")
				}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}


	//	helper.validateSuccessResponse(responseBody_booking,expectedValue)
	}
	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}

	softAssert.assertAll();4
		println "\n\n"
		where:

		rowNum|testDesc | searchRowNum
		11|" cancel the booking after completing the booking -   HBG hotel booking id  " | 50
	}

	@Unroll
	def "verify Search Booking after Cancel #14|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		println "Response Xml...\n${responseBody_booking}"

		//validate response
		//validate SearchBookingItemResponse returned
		String SearchBookingItemResponse=node_booking.ResponseDetails.SearchBookingItemResponse
		softAssert.assertTrue(SearchBookingItemResponse.size() != 0, "SearchBookingItemResponse does not return")

		def expectedValue = "Cancelled"
		if(helper.getExpectedResult().equals("Success")){
			def respBookingRef = node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
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
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"X",3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"),"Booking status is not cancelled in CBS after 5 retries")

		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}


//		helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll()


		println "\n\n"
		where:

		rowNum|testDesc | searchRowNum
		11|" search for the booking after doing cancel -   HBG hotel booking id  " | 50
	}
}

class CancelCharge_C1237 extends CancelCharge {
	@Override
	String getClientId() {
		return "1237"
	}
}

class CancelCharge_C1238 extends CancelCharge {
	@Override
	String getClientId() {
		return "1238"
	}
}

class CancelCharge_C1239 extends CancelCharge {
	@Override
	String getClientId() {
		return "1239"
	}
}

class CancelCharge_C1244 extends CancelCharge {
	@Override
	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class CancelChargeSingleClient extends CancelCharge {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
