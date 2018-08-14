package com.hbg.test.PaxTypeBooking

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
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
import spock.lang.Unroll


abstract class TwoAdults_1extra_HBG extends XSellBaseTest{


	static final String SHEET_NAME="xmle2e_AddBookingPax_New"
	static final String SHEET_NAME_ADDBOOKINGITEMREQ="xmle2e_AddBookingItemRequest"
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("paxexcelSheet"))
	static final String SHEET_NAME_SEARCH="2Adults+1extra_HBG"


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
	def age

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



	@Unroll
/**
 * search for the HBG item untill found any avilable item
 */
	def "1. Verify Search for HBG items test || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
			List cities=numOfCitiesList(10)
		//List cities = ["LON","TPE","BKK"]
		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXmlWithChild(SHEET_NAME_SEARCH,rowNum,cities[k],age)
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug ("Search Response Xml..\n${responseBody_search}")
			//print "Search Response Xml..\n${responseBody_search}"

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
		softAssert.assertTrue(hotelListHBG.size() > 0,"Available HBG hotel not found for Cities : "+cities )
		softAssert.assertAll()

		where:
		rowNum | testDesc | age
		1|"search for avilable HBG hotel and save the item details" | 6
	}

	/**
	 * search for the GTA item untill found any avilable item
	 */
	@Unroll
	def "2. Verify Search for avilable GTA hotel  || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search

		for (k in 0..cities_GTA.size() - 1) {
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH, rowNum, cities_GTA[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
			responseBody_search = XmlUtil.serialize(node_search)
			logDebug("Search Response Xml..\n${responseBody_search}")
			//print "Search Response Xml..\n${responseBody_search}"

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
		rowNum | testDesc
		1|"search for avilable GTA hotel and save the item details"
	}
/**
 * AddBookingRequest
 * Creat booking an item with HBG item  inculding 1 child
 * @return
 */
	@Unroll
	def "3. Verify Create booking request with Child test  || #testDesc"(){
		itemDetails.put('randombookingRefNum',helper.getBookingReference())
		def bookingReferenceNum = null
		def expectedValue
		def result = false
		String responseBody_booking
		if(hotelListHBG.size() != 0) {
			println "Hotel size : " + hotelListHBG.size()
			for (k in  0 .. hotelListHBG.size()-1) {
				HotelDTO hDTO = hotelListHBG.get(k)
				bookingReferenceNum = helper.getBookingReference()
			
		//create a HBG booking with avilabel item including child
		def fileContents_booking=helper.getBookThreePaxXml(SHEET_NAME_SEARCH,rowNum,bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
		println "Request Xml...\n${fileContents_booking}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		 responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		// Verify the resonse "response code" and "confirmation status" number of pax
		 expectedValue = "Confirmed" +"|1,2,8"
		if(helper.getExpectedResult().equals("Success")  && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
			def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			successHotelDTO = hDTO
			successHotelDTO.setBookingReference(bookingReferenceNum)
			for (i in 0..respBookingRefsize) {
				if (respBookingRef[i].@ReferenceSource.equals("client")) {
					result = true
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
				}

				if (respBookingRef[i].@ReferenceSource.equals("api")) {
					result = true
					successHotelDTO.setBookingId(respBookingRef[i].text())
					break
				}

			}
			softAssert.assertTrue(result == true, "ReferenceSource is not client")
			bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
			break
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

			}
		}
		else{
			softAssert.assertTrue(hotelListHBG.size() != 0, "NO HBG Hotels found")
		}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc | searchRowNum
		2|"Book an item with child : HBG item booking " | 11
	}


 /**ABIR_HBG_2Adults+1cot
  * AddBookingItemRequest
 * Add new item to already created booking with 2pax and addational cot
 * @return
 **/
	@Unroll
	def "4. Verify add booking request with two pax and extra cot test  || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
			Node node_search
			List<HotelDTO> hotelListHBGLocal = new ArrayList<HotelDTO>()
			String responseBody_search
			List cities=numOfCitiesList(10)
			for(k in 0.. cities.size()-1 ){
				def fileContents_Search = helper.getPaxSearchXmlWithChild(SHEET_NAME_SEARCH,searchRowNum,cities[k],age)
				println "Search REquest Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)

				logDebug ("Search Response Xml..\n${responseBody_search}")
				//print "Search Response Xml..\n${responseBody_search}"

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

			if(hotelListHBGLocal.size() != 0) {

				for (k in 0..hotelListHBGLocal.size() - 1) {

					HotelDTO hDTO = hotelListHBGLocal.get(k)
					//add new item to already created boooking
					def fileContents_booking = helper.getAddBookingItemRequestTwoPax(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
					println "Request Xml...\n${fileContents_booking}"
					Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
					String responseBody_booking = XmlUtil.serialize(node_booking)
					logDebug("Search Response Xml..\n${responseBody_booking}")

					// Verify the response "response code" and "confirmation status" number of pax
					def expectedValue = "Confirmed" + "|3,4"
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
								successHotelDTO.setBookingId(respBookingRef[i].text())
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
			}
				else{
				softAssert.assertTrue(hotelListHBGLocal.size() > 0,"Available HBG hotel with 2 Adults 1 cot not found for Cities : "+cities )
			}
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}


	//helper.validateSuccessResponse(responseBody_booking,expectedValue)

	softAssert.assertAll()
	println "\n\n"

	where:
	rowNum | testDesc | searchRowNum
	3|"Add new item to already created booking with 2pax and addational cot  " | 13
}


/*
	* AddBookingItemRequest
	* Add new GTA item to already created booking with 2pax
	* @return
 */
	@Unroll
	def "5. Verify add booking request with GTA items  || #testDesc"(){
		println("Test Running ...$testDesc")
		def result = false
		if(successHotelDTO != null ) {

			HotelDTO hDTO = hotelListGTA.get(1)
			//add new item to already created boooking

		def fileContents_booking=helper.getAddBookingItemRequestTwoPax(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),hDTO.getItemCity(),hDTO.getItemCode(),hDTO.getCategoryId())
		println "Request Xml...\n${fileContents_booking}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		// Verify the response "response code" and "confirmation status" number of pax
		def expectedValue = hDTO.getItemCity().toString() +"|"+ hDTO.getItemCode().toString()+"|Confirmed" +"|5,6"
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
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
	
		if(result==true){
			helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}

		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		4|"Add new item to already created booking with 2pax and addational cot  "
	}

	/**SearchBooking
	 * search the booking after creation
	 * @return
	 */

	@Unroll
	def "6. Search for the created booking test || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
	
		def fileContents_bookingref=helper.getBookingRequest(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")
		def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
		def respBookingRefsize = respBookingRef.size()
		def result = false
		//def expectedValue = "Confirmed"
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
		//helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"
		where:
		testDesc | searchRowNum
		" Search for booking  -  booked HBG hotel booking id  " | 1
	}
/**
 * BookingItemPriceBreakdown
 * verify BookingItemPriceBreakdownResponse returns with ExtraBed="true" NumberOfCots="1"
 * @return
 */
	@Unroll
	def "7. Search for booking item price break down Pax test  || #testDesc"(){
		 println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getBookingItemPriceBreakdownRequest(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		// Verify the resonse "response code" and "confirmation status"
		def expectedValue = successHotelDTO.getItemCity().toString()+"|"+ successHotelDTO.getItemCode().toString()
		if(helper.getExpectedResult().equals("Success")&& node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.size() > 0){
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
		//validate room ID
		String roomId=node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id[0].toString()
		softAssert.assertTrue(roomId.equals(successHotelDTO.getCategoryId().toString()), "Booking reference ID does not match")



		helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll();4
		println "\n\n"

		where:

		testDesc | searchRowNum
		" Search for booking item price break down pax  -   HBG hotel booking id  " | 5
	}
/**
 * SBIR SearchBookingItemRequest
 * ●Valid response returns with the Booking status =Confirmed
 * ●All 3 Items returns in the response
 * @return
 */

	@Unroll
	def "8. Verify Search for the item after booking || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.	getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

	
		//validate SearchBookingItemResponse returned
		if(helper.getExpectedResult().equals("Success")&& node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.size() > 0){
			def respBookingRef = node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
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
	}
	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}

		softAssert.assertAll()
		where:

		rowNum|testDesc
		6|" Search for an item after  -   HBG hotel booking id  "

	}

	/**
	 * SCCR_Booking SearchChargeConditionsRequest
	 * BookingItem 2 charge conditions returns
	 * @return
	 */
	@Unroll
	def "9. Search Charge Conditions of booked items test || #testDesc"(){

		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getSearchChargeConditionsRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_SearchCondation = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_SearchCondation}")

		// Verify the resonse "search condation response" expacted value feccthed from excel

		if(helper.getExpectedResult().equals("Success")&& node_booking.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.size() > 0){
			helper.validateSuccessResponse(responseBody_SearchCondation)

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
		where:
		rowNum|testDesc
		7| " Search for charge condition for booked item   -   HBG hotel booking id  "

	}
	/**
	 * MBIR  ModifyBookingItemRequest
	 * ERR0155</ErrorId>
	 <ErrorText>Cannot modify Booking Item</ErrorText>
	 * @return
	 */

	@Unroll
	def "10. verify modify  item in a booking || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getModifyBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref,'ModifyBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReferences > 0){

			def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
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
		}else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("nova")){
			helper.validateFailureResponse(responseBody_booking)
		}
		else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("legacy")){

			helper.validateFailureResponse(responseBody_booking,"ErrorId","GRT1981")
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

		rowNum|testDesc
		8|" Modify by adding one more pax and verofy error-   HBG hotel booking id  "
	}
/**
 * MBR ModifyBookingRequest
 *●Booking response returns with Agent Ref changed
 * @return
 */
	@Unroll
	def "11. verify  modify batch Request in a booking || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getModifyBatchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		//def expectedValue = itemDetails.itemCity[0].toString()+"|"+itemDetails.itemCode[0].toString()
		if(helper.getExpectedResult().equals("Success")&& node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){

			def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
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
		}else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("nova")){
			helper.validateFailureResponse(responseBody_booking)
		}
		else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("legacy")){

			helper.validateFailureResponse(responseBody_booking,"ErrorId","GRT1981")
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
		//helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:

		rowNum|testDesc
		9|" Modify agentref name  -   HBG hotel booking id  "
	}

/**
 * CBIR_item1  CancelBookingItemRequest
 * ●Item 1 cancelled
 * ●Booking remains confirmed
 * @return
 */
	@Unroll
	def "12. verify Cancel item in a booking || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getCancelBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		def expectedValue ="Cancelled"+"|Confirmed"
		if(helper.getExpectedResult().equals("Success")){
			bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
			softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
			helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}
	}
	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}


		softAssert.assertAll();
		println "\n\n"
		where:

		rowNum|testDesc
		10|" cancel an Item in booking verify Item status to be cancelled and booking status is confirmed  -   HBG hotel booking id  "
	}
/**SearthBookingItem
 * Search for an item after cancel
 * @return
 */

	@Unroll
	def "13. Search for the item after cancel || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.	getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		//validate response
		//validate SearchBookingItemResponse returned
		if(helper.getExpectedResult().equals("Success")&& node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.size() > 0){
			def respBookingRef = node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
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
		bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
		softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		where:

		rowNum|testDesc
		11|" Search for an item after cancel of any item  -   HBG hotel booking id  "

	}
/**CancelBookingRequest
 * cancel entier booking
 * ●All Items status = Cancelled
 * ●The Gross value = "0.00"   Nett="0.00"
 * ●Booking cancelled in CBS and GCRES
 * @return
 */
	@Unroll
	def "14. Cancel  entire booking || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getCancelBookingRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		//def expectedValue = "Cancelled"
		if(helper.getExpectedResult().equals("Success")){
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

		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}

		bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"X",3)
		softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"),"Booking status is not cancelled in CBS after 5 retries")
		//helper.validateSuccessResponse(responseBody_booking,expectedValue)
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll();4
		println "\n\n"
		where:

		rowNum|testDesc
		12|" cancel the booking after completing the booking -   HBG hotel booking id  "
	}

	/**SearthBookingItem
	 * Search for an item after cancel
	 * @return
	 */
	@Unroll
	def "15. Search Booking after Cancel || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		//validate response
		//validate SearchBookingItemResponse returned
		String SearchBookingItemResponse=node_booking.ResponseDetails.SearchBookingItemResponse
		softAssert.assertTrue(SearchBookingItemResponse.size() != 0, "SearchBookingItemResponse does not return")

	//	def expectedValue = "Cancelled"
		if(helper.getExpectedResult().equals("Success")){
			def respBookingRef = node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
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

		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}

		bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"X",3)
		softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"),"Booking status is not cancelled in CBS after 5 retries")
	//	helper.validateSuccessResponse(responseBody_booking,expectedValue)

	}
	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
		softAssert.assertAll()

		println "\n\n"
		where:

		rowNum|testDesc
		11|" search for the booking after doing cancel -   HBG hotel booking id  "
	}
}
class TwoAdults_1extra_HBG_C1237 extends TwoAdults_1extra_HBG {
	@Override
	String getClientId() {
		return "1237"
	}
}

class TwoAdults_1extra_HBG_C1238 extends TwoAdults_1extra_HBG {
	@Override
	String getClientId() {
		return "1238"
	}
}

class TwoAdults_1extra_HBG_C1239 extends TwoAdults_1extra_HBG {
	@Override
	String getClientId() {
		return "1239"
	}
}

class TwoAdults_1extra_HBG_C1244 extends TwoAdults_1extra_HBG {
	@Override

	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class TwoAdults_1extra_HBGSingleClient extends TwoAdults_1extra_HBG {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
