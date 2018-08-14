package com.hbg.test.PaxTypeBooking

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.kuoni.qa.automation.page.GCReservationHome
import com.kuoni.qa.automation.page.GCReservationLoginPage
import com.kuoni.qa.automation.page.GCReservationSearchPage
import geb.spock.GebSpec
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll


abstract class Two_Pax_HBG extends XSellBaseTest{

	static final String SHEET_NAME="xmle2e_AddBookingPax_New"
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("paxexcelSheet"))
	static final String SHEET_NAME_SEARCH="2Pax_HBG"


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

	@Unroll

	/**
	 * search for the HBG item untill found any avilable item
	 */
	def "Verify Search for HBG items test #1 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
	List cities=numOfCitiesList(4)
		//List cities = ["LON","TPE","BKK"]
		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug ("Search Response Xml..\n${responseBody_search}")
			print "Search Response Xml..\n${responseBody_search}"

			//softAssert.assertTrue(!node_search.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")

			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList
			if(hotelNode != 0){
				 hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
			}
			if (hList.size() > 0) {

				hotelListHBG.addAll(hList)
/*				def city = cities[k]
				hotelListHBGmap.city = hotelListHBG*/

			}
		}

		println "total hotels count for all the cities : "+hotelListHBG.size()
		softAssert.assertTrue(hotelListHBG.size() > 0,"Available hotel not found")
		softAssert.assertAll()

		where:
		searchRowNum | testDesc
		1|"search for avilable HBG hotel and save the item details"
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
				def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities_GTA[k])
				println "Search REquest Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				print "Search Response Xml..\n${responseBody_search}"

				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList
				if (hotelNode != 0) {
					hList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
				}

				if (hList.size() > 0) {
					hotelListGTA.addAll(hList)
				}
			}


			println "total hotels count for all the cities : " + hotelListGTA.size()
			softAssert.assertTrue(hotelListGTA.size() > 0, "Available hotel not found")

		softAssert.assertAll()

		where:
		searchRowNum | testDesc
		14|"search for avilable HBG hotel and save the item details"
	}

	@Unroll
	/**
	 * AddBookingRequest
	 * Creat booking an item with HBG item  inculding 1 child
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
			node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
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
				String roomId = node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem.HotelItem.HotelPaxRoom.@Id[0]
				softAssert.assertTrue(roomId.equals(successHotelDTO.getCategoryId().toString()), "Room ID  not matching after booking")
				break
			}

		}
			if (helper.getExpectedResult().equals("Failure")) {
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

		if(result == true){
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

	@Unroll
	def "Verify Search for booking test #4|| #testDesc"(){
		println("Test Running ...$testDesc")
		def result = false
		if(successHotelDTO != null ) {

			def fileContents_bookingref = helper.getBookingRequest(SHEET_NAME_SEARCH, searchRowNum, successHotelDTO.getBookingId())
			println "Request Xml...\n${fileContents_bookingref}"
			Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
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
 * Add new item to already created booking
 * @return
 */
	@Unroll
	def "Verify Adding single item to booking test #5|| #testDesc"(){
		println("Test Running ...$testDesc")
		String responseBody_booking
		def expectedValue
		def result = false
		Node node_booking
		List<HotelDTO> hotelListHBGLocal = new ArrayList<HotelDTO>()
		if(successHotelDTO != null ) {
			String responseBody_search
			List cities=numOfCitiesList(4)
			for(k in 0.. cities.size()-1 ){
				def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
				println "Search REquest Xml.. \n${fileContents_Search}"
				Node node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)

				logDebug ("Search Response Xml..\n${responseBody_search}")
				print "Search Response Xml..\n${responseBody_search}"

				//softAssert.assertTrue(!node_search.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")

				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList

				if(hotelNode != 0){
					hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
				}
				if (hList.size() > 0) {
					hotelListHBGLocal.addAll(hList)

				}
			}

			println "total hotels count for all the cities : "+hotelListHBGLocal.size()
			softAssert.assertTrue(hotelListHBGLocal.size() > 0,"Available hotel not found for ABIR")
			softAssert.assertAll()

			for (k in  0 .. hotelListHBGLocal.size()-1) {
				HotelDTO hDTO = hotelListHBGLocal.get(k)
				def fileContents_bookingref = helper.getAddBookingItemRequest(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
				println "Request Xml...\n${fileContents_bookingref}"
				 node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
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
					bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
					break
				}
			}
			if (helper.getExpectedResult().equals("Failure")) {
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

		if(result == true){
			helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}
			}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll();
		println "\n\n"


		where:

		rowNum|testDesc | searchRowNum
		3|" Add single item item to already created booking  -   HBG hotel  " | 14
	}

/**o check the rate key
 * Repeat t
 * search for the HBG item untill found any avilable item
 */
	@Unroll
	def "Verify Search for HBG item test #6 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
		List cities = numOfCitiesList(4)
		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug ("Search Response Xml..\n${responseBody_search}")
		//	softAssert.assertTrue(node_search.@ResponseReference.startsWith('XA'))
			//softAssert.assertTrue(!node_search.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")

			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList
			if(hotelNode != 0){
				hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
			}
			if (hList.size() > 0) {
				hotelListHBG.addAll(hList)
			}
		}

		println "total hotels count for all the cities : "+hotelListHBG.size()
		softAssert.assertTrue(hotelListHBG.size() > 0,"Available hotel not found")
		softAssert.assertAll()
		where:
		rowNum | testDesc | searchRowNum
		1|"search for avilable HBG hotel and save the item details" | 1
	}


/**
 * AddBookingItemRequest
 * Add new item to already created booking with multi items
 * @return
 */
	@Unroll
	def "Verify Add multiple item test #7|| #testDesc"(){
		println("Test Running ...$testDesc")
		List<HotelDTO> hotelListHBGLocal = new ArrayList<HotelDTO>()
		Node node_booking
		String responseBody_booking

		if(successHotelDTO != null ) {
			String responseBody_search
			List cities=numOfCitiesList(4)
			for(k in 0.. cities.size()-1 ){
				def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
				println "Search REquest Xml.. \n${fileContents_Search}"
				Node node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)

				logDebug ("Search Response Xml..\n${responseBody_search}")
				print "Search Response Xml..\n${responseBody_search}"

				//softAssert.assertTrue(!node_search.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")

				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList

				if(hotelNode != 0){
					hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
				}
				if (hList.size() > 0) {
					hotelListHBGLocal.addAll(hList)

				}
			}

			println "total hotels count for all the cities : "+hotelListHBGLocal.size()
			softAssert.assertTrue(hotelListHBGLocal.size() > 0,"Available hotel not found for ABIR")
			softAssert.assertAll()
			for (k in  0 .. hotelListHBGLocal.size()-1) {
				HotelDTO hDTO = hotelListHBGLocal.get(k)
				HotelDTO hDTO1 = hotelListHBGLocal.get(k)
			def fileContents_bookingref = helper.getAddBookingMultiItemRequest(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), hDTO1.getItemCity(), hDTO1.getItemCode(), hDTO1.getCategoryId())
			println "Request Xml...\n${fileContents_bookingref}"
			node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
			 responseBody_booking = XmlUtil.serialize(node_booking)
			logDebug("Search Response Xml..\n${responseBody_booking}")

			if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
				def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
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
				bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
				softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
				break

			}
				if(hotelListHBGLocal.size() == 50){
					break
				}
		}
			if (helper.getExpectedResult().equals("Failure")) {
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

			// there should be 2 bookings in response
			def respBookingItems = node_booking.ResponseDetails.BookingResponse.BookingItems.BookingItem
			def respBookingItemssize = respBookingItems.size()
			softAssert.assertTrue(respBookingItemssize == 2 , "2 Booking items are not returned in response")
	}
	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}


		softAssert.assertAll()
		println "\n\n"

		where:

		rowNum|testDesc | searchRowNum
		4 |"Adding multiple item and booking -   HBG and GTA item " | 14
	}
/**
 * AddBookingItemRequest
 * Add new GTA item to already created booking with 2pax
 * @return
 */
	@Unroll
	def "Verify add booking request with GTA items  #8 || #testDesc"(){
		println("Test Running ...$testDesc")
		HotelDTO hDTO = hotelListGTA.get(1)
		def result = false
	if(successHotelDTO != null){
		def fileContents_booking=helper.getAddBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),hDTO.getItemCity(),hDTO.getItemCode(),hDTO.getCategoryId())
		println "Request Xml...\n${fileContents_booking}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug("Search Response Xml..\n${responseBody_booking}")

		// Verify the response "response code" and "confirmation status" number of pax
		def expectedValue = hDTO.getItemCity().toString() +"|"+ hDTO.getItemCode().toString()
		if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
			def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()

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
			bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
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
	} else if (node_booking.ResponseDetails.Errors.Error.size() > 0) {
			def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
			print "ERROR IN RESPONSE " + Errostring
			softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
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
		5|"Add new GTA item to already created booking  : GTA item   " | 11
	}

	/**
	 * search for the HBG item untill found any avilable item
	 */
	@Unroll
	def " Verify Search for HBG items test #9 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
		List cities = numOfCitiesList(4)
		if(successHotelDTO != null){
		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug ("Search Response Xml..\n${responseBody_search}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
			if (hList.size() > 0) {
				hotelListHBG.addAll(hList)
			}
		}

		println "total hotels count for all the cities : "+hotelListHBG.size()
		softAssert.assertTrue(hotelListHBG.size() > 0,"Available hotel not found")
	}
	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
		softAssert.assertAll()
		println "\n\n"
		where:
		rowNum | testDesc | searchRowNum
		1|"search for avilable HBG hotel and save the item details" | 1
	}

	/**
	 * AddBookingItemRequest
	 * Add new GTA item to already created booking with combination of HBG and GTA item
	 * @return
	 */
	@Unroll
	def "Verify add booking request with GTA and HBG Multiple items  #10 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		String responseBody_booking
		def result
		List<HotelDTO> hotelListHBGLocal = new ArrayList<HotelDTO>()
		if(successHotelDTO != null ) {
			String responseBody_search
			List cities=numOfCitiesList(4)
			for(k in 0.. cities.size()-1 ){
				def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
				println "Search REquest Xml.. \n${fileContents_Search}"
				Node node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)

				logDebug ("Search Response Xml..\n${responseBody_search}")
				print "Search Response Xml..\n${responseBody_search}"

				//softAssert.assertTrue(!node_search.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")

				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList

				if(hotelNode != 0){
					hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
				}
				if (hList.size() > 0) {
					hotelListHBGLocal.addAll(hList)

				}
			}

			println "total hotels count for all the cities : "+hotelListHBGLocal.size()
			softAssert.assertTrue(hotelListHBGLocal.size() > 0,"Available hotel not found for ABIR multi items")
			softAssert.assertAll()
			def expectedValue
			for (k in 0..hotelListHBGLocal.size() - 1) {
				HotelDTO hDTO = hotelListHBGLocal.get(k)
				HotelDTO gDTO = hotelListGTA.get(k)
				//add new item to already created boooking
				def fileContents_booking = helper.getAddBookingMultiItemRequest(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), gDTO.getItemCity(), gDTO.getItemCode(), gDTO.getCategoryId())
				println "Request Xml...\n${fileContents_booking}"
				 node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
				 responseBody_booking = XmlUtil.serialize(node_booking)
				logDebug("Search Response Xml..\n${responseBody_booking}")

				// Verify the response "response code" and "confirmation status" number of pax
				 expectedValue = hDTO.getItemCity().toString() + "|" + hDTO.getItemCode().toString() + "|5,6"
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
					def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					 result = false
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
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")

					break


				}
				if(k == 25 ){
					break
				}

			}
			if (helper.getExpectedResult().equals("Failure")) {
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
			if(result == true){
				helper.validateSuccessResponse(responseBody_booking, expectedValue)
			}

		}
			else{
				softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
			}


			softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc | searchRowNum
		6|"Add new GTA and HBG item to already created booking  : GTA and HBG  item booking  " | 14
	}



	@Unroll

	def "Verify  booking item price break down test #11 || #testDesc"() {
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
		def fileContents_bookingref = helper.getBookingItemPriceBreakdownRequest(SHEET_NAME_SEARCH, searchRowNum, successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug("Search Response Xml..\n${responseBody_booking}")

		// Verify the resonse "response code" and "confirmation status"
		def expectedValue = successHotelDTO.getItemCity().toString() + "|" + successHotelDTO.getItemCode().toString()
		if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.BookingReference.size() > 0) {
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
			//validate room ID
			String roomId = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id[0]
			softAssert.assertTrue(roomId.equals(successHotelDTO.getCategoryId().toString()), "Booking reference ID does not match")
			helper.validateSuccessResponse(responseBody_booking, expectedValue)

		} else if (helper.getExpectedResult().equals("Failure")) {
			helper.validateFailureResponse(responseBody_booking)
		} else if (helper.getExpectedResult().equals("Empty")) {
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}



	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll();
		println "\n\n"

		where:

		testDesc | searchRowNum
		" Search for booking item price break down pax  -   HBG and GTA items  " | 7
	}
	@Unroll
	def "Verify Charge Conditions of bookied items test #12|| #testDesc"(){

		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {

		def fileContents_bookingref=helper.getSearchChargeConditionsRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_SearchCondation =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
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
		8| " Search for charge condition for booked item   -   HBG and GTA hotel booking id  " | 50

	}

	@Unroll
	def "Verify modify batch item in a booking #13|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.getModifyBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node responseBody = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref,'ModifyBooking', FITSiteIdHeaderKey, SITEID)
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
		9|" Modify agent details In booking -   HBG item  " | 50
	}

	@Unroll
	def "Verify modify batch Request in a booking #14|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.getModifyBatchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node responseBody = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref,'ModifyBooking', FITSiteIdHeaderKey, SITEID)
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
		10|" Modify batch booking details In booking -   HBG item  " | 50
	}


	@Unroll
	def "Verify Cancel item in a booking #15|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.getCancelBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug("Search Response Xml..\n${responseBody_booking}")

		def expectedValue = "Cancelled"+"|Confirmed"
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
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
		11|" cancel an Item in booking verify Item status to be cancelled and booking status is confirmed  -   HBG item " | 50
	}

	@Unroll
	def "verify Search for the item after cancel #16|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.	getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
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
		12|" Search for an item after cancel of any item  -   HBG item "

	}

	@Unroll
	def "Verify Cancel  entire booking #17|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
			def fileContents_bookingref=helper.getCancelBookingRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
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
		13|" cancel the booking after completing the booking -   HBG hotel booking id  " | 50
	}

	@Unroll
	def "verify Search Booking after Cancel #18|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null) {
		def fileContents_bookingref=helper.getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
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
		13|" search for the booking after doing cancel -   HBG hotel booking id  " | 50
	}
}
class Two_Pax_HBG_C1237 extends Two_Pax_HBG {
	@Override
	String getClientId() {
		return "1237"
	}
}

class Two_Pax_HBG_C1238 extends Two_Pax_HBG {
	@Override
	String getClientId() {
		return "1238"
	}
}

class Two_Pax_HBG_C1239 extends Two_Pax_HBG {
	@Override
	String getClientId() {
		return "1239"
	}
}

class Two_Pax_HBG_C1244 extends Two_Pax_HBG {
	@Override
	String getClientId() {
		return "1244"
	}
}
@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class Two_Pax_HBGSingleClient extends Two_Pax_HBG {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
