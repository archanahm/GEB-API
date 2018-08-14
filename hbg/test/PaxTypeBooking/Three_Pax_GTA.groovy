package com.hbg.test.PaxTypeBooking

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll


abstract class Three_Pax_GTA extends XSellBaseTest{
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("paxexcelSheet"))
	static final String SHEET_NAME_SEARCH="3Pax_GTA"

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
	def itemDetails_HBG = [:]
	@Shared
	int item =0
	def flag = false
	Map itemMap=null
	Map fields = null
	static final int ITEM_COUNT= Integer.parseInt(ConfigProperties.getValue("HBG_City_Count").toString().trim())

	static final String CITY_GTA= ConfigProperties.getValue("City_List")
	def cities_GTA = CITY_GTA.split(",")
	BookingStatus bookingStatus = null
	@Shared
	List<HotelDTO> hotelListHBG = new ArrayList<HotelDTO>()
	@Shared
	List<HotelDTO> hotelListHBG1 = new ArrayList<HotelDTO>()


	@Shared
	List<HotelDTO> hotelListGTA = new ArrayList<HotelDTO>()

	@Shared
	HotelDTO successHotelDTO = null

	@Shared
	HotelDTO gtaSuccessHotelDTO = null

	AS400DatabaseUtil dbconn= new AS400DatabaseUtil()
	/**
	 * SearchHotelPricePaxRequest
	 * search for the HBG item untill found any avilable item
	 */
	@Unroll
	def "SearchHotelPricePax: #1 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
		List cities=numOfCitiesList(4)
		def itemCodelist=[]
		for(k in 0.. cities.size()-1 ) {
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug("Search Response Xml..\n${responseBody_search}")

			//softAssert.assertTrue(!node_search.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")
			if (node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.size() > 0) {
				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
				if (hList.size() > 0) {
					hotelListHBG.addAll(hList)
				}
			}
		}

		println "total hotels count for all the cities : "+hotelListHBG.size()
		softAssert.assertTrue(hotelListHBG.size() > 0,"Available hotel not found for Cities " +cities)
		softAssert.assertAll()
		where:
		testDesc | searchRowNum
		"search for avilable HBG hotel and save the item details" | 1
	}

	/**
	 * search for the GTA item untill found any avilable item
	 */
	@Unroll
	def "SearchHotelPricePax: #2 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
		def itemCodelist=[]
		for(k in 0.. cities_GTA.size()-1 ) {
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities_GTA[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
			responseBody_search = XmlUtil.serialize(node_search)
			logDebug("Search Response Xml..\n${responseBody_search}")
			if (node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.size() > 0) {
				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
				if (hList.size() > 0) {
					hotelListGTA.addAll(hList)
				}
			}
		}

		println "total hotels count for all the cities : "+hotelListGTA.size()
		softAssert.assertTrue(hotelListGTA.size() > 0,"Available GTA hotel not found for cities " + cities_GTA )
		softAssert.assertAll()
		where:
		testDesc | searchRowNum
		"search for avilable GTA hotel and save the item details" | 1
	}
/**
 * AddBookingRequest
 * Creat booking an item with HBG item  inculding 1 child
 * @return
 */
	@Unroll
	def "SearchHotelPricePax:  #3 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		def result = false
		def bookingStatusCode
		itemDetails.put('randombookingRefNum',helper.getBookingReference())
		def bookingReferenceNum = null
		def expectedValue
		String responseBody_booking
		println "Hotel size : " + hotelListHBG.size()
		if(hotelListHBG.size() != 0) {
		for (k in  0 .. hotelListHBG.size()-1) {
			HotelDTO hDTO = hotelListHBG.get(k)
			bookingReferenceNum = helper.getBookingReference()
			def fileContents_booking = helper.getBookThreePaxXml(SHEET_NAME_SEARCH, rowNum,bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
			println "Request Xml...\n${fileContents_booking}"
			 node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
			 responseBody_booking = XmlUtil.serialize(node_booking)
			logDebug ("Search Response Xml..\n${responseBody_booking}")

			if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
				successHotelDTO = hDTO
				successHotelDTO.setBookingReference(bookingReferenceNum)
				expectedValue = hDTO.getItemCity().toString() +"|" +hDTO.getItemCode().toString()+"|1,2"
				def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
				def respBookingRefsize = respBookingRef.size()

				for (i in 0..respBookingRefsize-1) {
					if (respBookingRef[i].@ReferenceSource.equals("client")) {
						result = true
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
					}

					if (respBookingRef[i].@ReferenceSource.equals("api")) {
						result = true
						successHotelDTO.setBookingId(respBookingRef[i].text())

					}

				}
				softAssert.assertTrue(result == true, "ReferenceSource is not client")
				bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
				softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")

				break
			} else if (helper.getExpectedResult().equals("Failure")) {
				helper.validateFailureResponse(responseBody_booking)
			} else if (helper.getExpectedResult().equals("Empty")) {
				helper.validateXmlWithEmptyResponse(responseBody_booking)
			}else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
				def errorStr = node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
				println "\n\n"
				errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
				println "ERROR IN RESPONSE " + errorStr
				println "\n\n"
				softAssert.assertFalse(node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0, " Error Returned in Booking Response" + errorStr)
				println "\n\n"
			}
		}
			if(	result == true){
				helper.validateSuccessResponse(responseBody_booking, expectedValue)
				def numberofcots = node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem.HotelItem.HotelPaxRoom.@Cots
				softAssert.assertTrue(numberofcots[0].equals("1"), "numberofcots is not as per the request")
			}
			/*else if (node_booking.ResponseDetails.Errors.Error.size() > 0 ){
				def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
				println "\n\n"
				println "ERROR IN RESPONSE " + Errostring
				softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)

			}*/

		}
		else{
			softAssert.assertTrue(hotelListHBG.size() != 0, "NO HBG Hotels found for cities " )
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		2|"create new booking for HBG item : AddBookingRequest "
	}

	/**
	 * SearchHotelPricePaxRequest
	 * search for the HBG item untill found any avilable item
	 */
	@Unroll
	def "SearchHotelPricePax #4 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
		List cities=numOfCitiesList(8)
		def itemCodelist=[]
		for(k in 0.. cities.size()-1 ) {
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug("Search Response Xml..\n${responseBody_search}")

			if (node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.size() > 0) {
				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
				if (hList.size() > 0) {
					hotelListHBG1.addAll(hList)
				}
			}
		}

		println "total hotels count for all the cities : "+hotelListHBG1.size()
		softAssert.assertTrue(hotelListHBG1.size() > 0,"Available hotel not found in " + cities)
		softAssert.assertAll()
		where:
		testDesc | searchRowNum
		"search for avilable HBG hotel with 1 Child" | 12
	}


	/**
	 * AddBookingItemRequest
	 * Add new item to already created booking with 3pAx HBG
	 * @return
	 */
	@Unroll
	def "SearchHotelPricePax  #5 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		def bookingStatusCode
		itemDetails.put('randombookingRefNum',helper.getBookingReference())
		def bookingReferenceNum = null
		def expectedValue
		def result = false
		println "Hotel size : " + hotelListHBG1.size()
		if(successHotelDTO != null && hotelListHBG1.size() != 0 ) {
			for (k in  0 .. hotelListHBG1.size()-2) {
				HotelDTO hDTO = hotelListHBG1.get(k)

			//add new item to already created boooking
			def fileContents_booking = helper.getAddBookingItemRequestThreePax(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(),hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
			println "Request Xml...\n${fileContents_booking}"
			 node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
			String responseBody_booking = XmlUtil.serialize(node_booking)
			logDebug ("Search Response Xml..\n${responseBody_booking}")

			// Verify the response "response code" and "confirmation status" number of pax
			 expectedValue = hDTO.getItemCity().toString() +"|" +hDTO.getItemCode().toString()+"|1,2,3"
			if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
				def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
				def respBookingRefsize = respBookingRef.size()

				for (i in 0..respBookingRefsize) {
					if(respBookingRef[i].@ReferenceSource.equals("client"))
					{
						result=true
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
					}

					if(respBookingRef[i].@ReferenceSource.equals("api"))
					{
						result=true
						softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking reference ID does not match")
						break
					}

				}
				softAssert.assertTrue(result == true, "ReferenceSource is not client")
				bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
				softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
				helper.validateSuccessResponse(responseBody_booking, expectedValue)
				break
			} else if (helper.getExpectedResult().equals("Failure")) {
				helper.validateFailureResponse(responseBody_booking)
			} else if (helper.getExpectedResult().equals("Empty")) {
				helper.validateXmlWithEmptyResponse(responseBody_booking)
			} else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
				def errorStr = node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
				println "ERROR IN RESPONSE " + errorStr
				println "\n\n"
				errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
				println "ERROR IN RESPONSE " + errorStr
				softAssert.assertFalse(node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
			} else if (node_booking.ResponseDetails.Errors.Error.size() > 0) {
				def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
				print "ERROR IN RESPONSE " + Errostring
				println "\n\n"
				softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
			}
				if(result == true ){
					helper.validateSuccessResponse(responseBody_booking, expectedValue)
					break
				} else if(k == 50) {
					break
				}
		}
		def respBookingItems = node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem
		def respBookingItemssize = respBookingItems.size()
		//softAssert.assertTrue(respBookingItemssize == 2 , "2 Booking items are not returned in response")


		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		3|"Add new item to already created booking with 3PAX HBG  "
	}

	/**
	 * AddBookingItemRequest
	 * Add new item to already created booking with 2pAx GTA
	 * @return
	 */
	@Unroll
	def "SearchHotelPricePax  #6 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		def bookingStatusCode
		def result = false
		itemDetails.put('randombookingRefNum',helper.getBookingReference())
		def bookingReferenceNum = null
		def expectedValue
		println "Hotel size : " + hotelListGTA.size()
		if(successHotelDTO != null && hotelListGTA.size() != 0 ) {
			for (k in  0 .. hotelListGTA.size()-2) {
				HotelDTO hDTO = hotelListGTA.get(k)

				//add new item to already created boooking
				def fileContents_booking = helper.getAddBookingItemRequestTwoPax(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(),hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
				println "Request Xml...\n${fileContents_booking}"
				 node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
				String responseBody_booking = XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody_booking}")

				// Verify the response "response code" and "confirmation status" number of pax
				expectedValue = hDTO.getItemCity().toString() +"|" +hDTO.getItemCode().toString()+"|2,3"
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
					def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()

					for (i in 0..respBookingRefsize) {
						if(respBookingRef[i].@ReferenceSource.equals("client"))
						{
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
						}

						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking reference ID does not match")
							break
						}

					}
					softAssert.assertTrue(result == true, "ReferenceSource is not client")
					bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
				//	helper.validateSuccessResponse(responseBody_booking, expectedValue)
					break
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody_booking)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody_booking)
				} else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
					def errorStr = node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
					println "ERROR IN RESPONSE " + errorStr
					println "\n\n"
					errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
					println "ERROR IN RESPONSE " + errorStr
					softAssert.assertFalse(node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
				} else if (node_booking.ResponseDetails.Errors.Error.size() > 0) {
					def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
					print "ERROR IN RESPONSE " + Errostring
					println "\n\n"
					softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
				}
				if(result == true ){
					helper.validateSuccessResponse(responseBody_booking, expectedValue)
					break
				} else if(k == 50) {
					break
				}
			}
			def respBookingItems = node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem
			def respBookingItemssize = respBookingItems.size()
			//softAssert.assertTrue(respBookingItemssize == 2 , "2 Booking items are not returned in response")
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		13|"Add new item to already created booking with 2 PAX GTA  "
	}

	/**SearchBooking
	 *search for creted booking
	 * @return
	 */
	@Unroll
	def "SearchHotelPricePax : #7|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getBookingRequest(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")
				//def expectedValue = "Confirmed"
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.size() > 0){
			def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
			//helper.validateSuccessResponse(responseBody_booking, expectedValue)
		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
			def errorStr = node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
			println "ERROR IN RESPONSE " + errorStr
			println "\n\n"
			errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
			println "ERROR IN RESPONSE " + errorStr
			softAssert.assertFalse(node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
		}
		else if(node_booking.ResponseDetails.Errors.Error.size()> 0){
			def Errostring=node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
			print "ERROR IN RESPONSE " + Errostring
			println "\n\n"
			softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size()> 0, "Error Returned in Booking Response " + Errostring)
		}
	}
	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"
		where:
		testDesc | searchRowNum
		" Search for created booking and status " | 4
	}

	/**
	 * BookingItemPriceBreakdown
	 * verify BookingItemPriceBreakdownResponse returns with ExtraBed="true" NumberOfCots="1"
	 * @return
	 */
	@Unroll
	def "SearchHotelPricePax: #8 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getBookingItemPriceBreakdownRequest(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		// Verify the resonse "response code" and "confirmation status"
		def expectedValue = successHotelDTO.getItemCity()+"|"+ successHotelDTO.getItemCode()+"|"+"1"
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.size() > 0){
			def respBookingRef = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i].@ReferenceSource.equals("client"))
				{
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}

				if(respBookingRef[i].@ReferenceSource.equals("api"))
				{
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking reference ID does not match")

					break
				}

			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
			softAssert.assertTrue(node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.size() == 1, "Response contains more than 1 or less  booking items")
			softAssert.assertTrue(node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.PriceRanges.size() == 1, "Response contains more than 1 or less booking items PriceRanges")

		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}
		else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
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
		helper.validateSuccessResponse(responseBody_booking,expectedValue)
		def numberofcots = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.@NumberOfCots
		//softAssert.assertTrue(numberofcots[0].equals("1"), "numberofcots")
	}
else{
	softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
}
		softAssert.assertAll()
		println "\n\n"

		where:

		testDesc | searchRowNum
		" Search for booking item price break down pax  -   HBG hotel booking id  " | 5
	}


	/**
	 * SCCR_Booking SearchChargeConditionsRequest
	 * BookingItem 2 charge conditions returns
	 * @return
	 */
	@Unroll
	def "SearchHotelPricePax: #9|| #testDesc"(){

		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {

		def fileContents_bookingref=helper.getSearchChargeConditionsRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_SearchCondation = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_SearchCondation}")

		// Verify the resonse "search condation response" expacted value feccthed from excel
		def expectedValue ="cancellation,amendment"

		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.size() > 0){
			helper.validateSuccessResponse(responseBody_SearchCondation,expectedValue)

		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_SearchCondation)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_SearchCondation)
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



		println "\n\n"
		where:
		rowNum|testDesc
		6| " Search for charge condition for booked item   -   HBG hotel booking id  "

	}

/**
 * MBR ModifyBookingRequest
 *�?Booking response returns with Agent Ref changed
 * @return
 */
	@Unroll
	def "SearchHotelPricePax: #10|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getModifyBatchBookingRequestAsync(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId(),xmlName)
		println "Request Xml...\n${fileContents_bookingref}"
		String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_bookingref, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 0) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")
				def expectedValue =successHotelDTO.getItemCity().toString() + "|" + successHotelDTO.getItemCode().toString()
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.ModifyBookingRequest.size() > 0) {

					def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0..respBookingRefsize - 1) {
						if (respBookingRef[i].@ReferenceSource.equals("agent")) {
							result = true
							softAssert.assertTrue(respBookingRef[i].text().equals("ChgAgentREF"), "agent details not updated")
							break
						}
					}
					softAssert.assertTrue(result == true, "ReferenceSource is not api")

				} else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("nova")){
					helper.validateFailureResponse(responseBody)
				}
				else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("legacy")){

					helper.validateFailureResponse(responseBody,"ErrorId","GRT1981")
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody)
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
				//helper.validateSuccessResponse(responseBody_booking,expectedValue)
			}
		}

	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		println "\n\n"
		softAssert.assertAll()
		println "\n\n"


		where:

		rowNum|testDesc
		7|" Modify agentref name  -   HBG hotel booking id  "
	}
/**
 * MBR ModifyBookingitemRequest
 *�?check for error in response
 * @return
 */
	@Unroll
	def "SearchHotelPricePax: #11 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getModifyBatchBookingRequestAsync(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId(),xmlName)
		println "Request Xml...\n${fileContents_bookingref}"
		String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_bookingref, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 2) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")
				def expectedValue = successHotelDTO.getItemCity().toString() + "|" + successHotelDTO.getItemCode().toString()
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.ModifyBookingRequest.size() > 0) {

					def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0..respBookingRefsize - 1) {
						if (respBookingRef[i].@ReferenceSource.equals("agent")) {
							result = true
							softAssert.assertTrue(respBookingRef[i].text().equals("ChgAgentREF"), "agent details not updated")
							break
						}
					}
					softAssert.assertTrue(result == true, "ReferenceSource is not api")
				} else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("nova")){
					helper.validateFailureResponse(responseBody)
				}
				else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("legacy")){

					helper.validateFailureResponse(responseBody,"ErrorId","GRT1981")
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody)
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
				//helper.validateSuccessResponse(responseBody_booking,expectedValue)
			}
		}
	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"



		where:

		rowNum|testDesc
		8|" Modify booking item validate response  -   HBG hotel booking id  "
	}
/**
 * CBIR_item1  CancelBookingItemRequest
 * �?Item 1 cancelled
 * �?Booking remains confirmed
 * @return
 */
	@Unroll
	def "SearchHotelPricePax: #12 || #testDesc"(){
		println("Test Running ...$testDesc")
		def expectedValue
		String responseBody_booking
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getCancelBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		 responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		 expectedValue ="Cancelled"+"|Confirmed"
		if(helper.getExpectedResult().equals("Success")){
			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")

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

		softAssert.assertAll();
		helper.validateSuccessResponse(responseBody_booking,expectedValue)
		println "\n\n"
		where:

		rowNum|testDesc
		9|" cancel an Item in booking verify Item status to be cancelled and booking status is confirmed  -   HBG hotel booking id  "
	}

/**SearthBookingItem
 * Search for an item after cancel
 * @return
 */

	@Unroll
	def "SearchHotelPricePax: #13 || #testDesc"(){
		println("Test Running ...$testDesc")
		String responseBody_booking
		Node node_booking

		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.	getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		 node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		 responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		//validate response
		//validate SearchBookingItemResponse returned
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.size() > 0){
			def respBookingRef = node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i].@ReferenceSource.equals("client"))
				{
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}

				if(respBookingRef[i].@ReferenceSource.equals("api"))
				{
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID  does not match")
					break
				}

			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")

			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")

		}
		else if(helper.getExpectedResult().equals("Failure")){
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

		sleep(5*1000)
	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()

		where:

		rowNum|testDesc
		10|" Search for an item after cancel of any item  -   HBG hotel booking id  "

	}
	/**CancelBookingRequest
	 * cancel entier booking
	 * �?All Items status = Cancelled
	 * �?The Gross value = "0.00"   Nett="0.00"
	 * �?Booking cancelled in CBS and GCRES
	 * @return
	 */
	@Unroll
	def "SearchHotelPricePax: #14 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getCancelBookingRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		//def expectedValue = "Cancelled"
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference.size() >0){
			def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i].@ReferenceSource.equals("client"))
				{
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}

				if(respBookingRef[i].@ReferenceSource.equals("api"))
				{
					result=true
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


	//	helper.validateSuccessResponse(responseBody_booking,expectedValue)
	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:

		rowNum|testDesc
		11|" cancel the booking and validate status"
	}


}
class Three_Pax_GTA_C1237 extends Three_Pax_GTA {
	@Override
	String getClientId() {
		return "1237"
	}
}

class Three_Pax_GTA_C1238 extends Three_Pax_GTA {
	@Override
	String getClientId() {
		return "1238"
	}
}

class Three_Pax_GTA_C1239 extends Three_Pax_GTA {
	@Override
	String getClientId() {
		return "1239"
	}
}

class Three_Pax_GTA_C1244 extends Three_Pax_GTA {
	@Override

	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class Three_Pax_GTASingleClient extends Three_Pax_GTA {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
