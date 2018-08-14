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


abstract class TwoAdults_1cot_HBG extends XSellBaseTest{


	static final String SHEET_NAME="xmle2e_AddBookingPax_New"
	static final String SHEET_NAME_ADDBOOKINGITEMREQ="xmle2e_AddBookingItemRequest"
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("paxexcelSheet"))
	static final String SHEET_NAME_SEARCH="2Adults+1cot_HBG"
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


	@Unroll

	/**
	 * SearchHotelPricePaxRequest
	 * search for the HBG item untill found any avilable item
	 */

	def "Search avilable HBG hotel #1 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
		List cities=numOfCitiesList(4)
		def itemCodelist=[]
		for(k in 0.. 3 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			 responseBody_search = XmlUtil.serialize(node_search)
			logDebug ("Search Response Xml..\n${responseBody_search}")

			//softAssert.assertTrue(!node_search.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")

			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
			if (hList.size() > 0) {
				hotelListHBG.addAll(hList)
			}
		}


		softAssert.assertTrue(hotelListHBG.size() > 0,"Available hotel not found")

		where:
		  testDesc | searchRowNum
		"search for avilable HBG hotel and save the item details" | 1
	}

	/**
	 * search for the GTA item untill found any avilable item
	 */
	@Unroll
	def "Search for avilable GTA hotel #2 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
		def itemCodelist=[]
		for(k in 0.. cities_GTA.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities_GTA[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody_search = XmlUtil.serialize(node_search)
			logDebug ("Search Response Xml..\n${responseBody_search}")

			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
			if (hList.size() > 0) {
				hotelListGTA.addAll(hList)
			}
		}

		softAssert.assertTrue(hotelListGTA.size() > 0,"Available hotel not found")

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
	def "Verify Create booking request with Child test #3 || #testDesc"(){
		println("Test Running ...$testDesc ")
		if(hotelListHBG.size() != 0) {
		def bookingStatusCode
		def bookingReferenceNum = null
		bookingReferenceNum = helper.getBookingReference()
		//itemDetails.put('randombookingRefNum',helper.getBookingReference())
		def result = false
		String responseBody_booking
		println "Hotel size : " + hotelListHBG.size()
		def expectedValue = "1,2"

			for (k in 0..hotelListHBG.size() - 1) {
				HotelDTO hDTO = hotelListHBG.get(k)
				def fileContents_booking = helper.getBookPaxXml(SHEET_NAME_SEARCH, rowNum, bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
				println "Request Xml...\n${fileContents_booking}"
				Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
				responseBody_booking = XmlUtil.serialize(node_booking)
				logDebug("Search Response Xml..\n${responseBody_booking}")

				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
					successHotelDTO = hDTO
					successHotelDTO.setBookingReference(bookingReferenceNum)

					def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()

					for (i in 0..respBookingRefsize - 1) {
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
					bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")

					break
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody_booking)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody_booking)
				}  else if (node_booking.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
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
			softAssert.assertTrue(hotelListHBG != null, "NO HBG Hotels found")
		}

		softAssert.assertAll()

		"\n\n"

		where:
		rowNum | testDesc
		2 | "Book an item with child : HBG item booking "
	}


	/**ABIR_multiItems
	 * AddBookingItemRequest
	 * Add new item to already created booking with 2pax and addational cot
	 * @return
	 */
	@Unroll
	def "Verify add booking item  #4 || #testDesc"(){
		println("Test Running ...$testDesc")
		HotelDTO hDTO

		def itemCity = []
		def itemCode = []
		def itemRateKey = []
		if(successHotelDTO != null ) {
		for (k in  0 .. 3) {

			 hDTO = hotelListHBG.get(k)
			itemCity << hDTO.getItemCity()
			itemCode << hDTO.getItemCode()
			itemRateKey << hDTO.getCategoryId()
		}
		itemDetails.put('itemCity',itemCity)
		itemDetails.put('itemCode',itemCode)
		itemDetails.put('itemRateKey',itemRateKey)
		//add new item to already created boooking
	def fileContents_booking=helper.getAddBookingItemRequestMultiItem(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),itemDetails.itemCity[0], itemDetails.itemCode[0],itemDetails.itemRateKey[0],itemDetails.itemCode[1],itemDetails.itemRateKey[1])
	println "Request Xml...\n${fileContents_booking}"
	Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
	String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

	// Verify the response "response code" and "confirmation status" number of pax
	def expectedValue = "3,4"
	if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReference.size() > 0){
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
		bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
		softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")

		helper.validateSuccessResponse(responseBody_booking, expectedValue)
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
	rowNum | testDesc
	3 | "Add new item to already created booking with 2pax 1 Child and addational cot  "
}


	/**SearchBooking
	 *search for creted booking
	 * @return
	 */
	@Unroll
	def "verify Search booking #5|| #testDesc"(){
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
			//helper.validateSuccessResponse(responseBody_booking, expectedValue)
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
	def "Search for booking item price break down Pax test #6 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getBookingItemPriceBreakdownRequest(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		// Verify the resonse "response code" and "confirmation status"
		def expectedValue = successHotelDTO.getItemCity()+"|"+ successHotelDTO.getItemCode()
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
/*		def numberofcots = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.@NumberOfCots
		softAssert.assertTrue(numberofcots[0].equals("1"), "numberofcots")*/
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

	/**SBIR SearchBookingItemRequest
	 * Valid response returns with the Booking status =Confirmed
	 * ●All 3 Items returns in the response
	 * @return
	 */

	@Unroll
	def "Verify Search for the item after booking #7|| #testDesc"(){
		println("Test Running ...$testDesc")
		def result = false
		String responseBody_booking
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.	getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		 responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")
		def expectedValue = "Confirmed"

		//validate response
		//validate SearchBookingItemResponse returned
		if(helper.getExpectedResult().equals("Success")  && node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.size() > 0){
			def respBookingRef = node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()

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
			//softAssert.assertTrue(node_booking.ResponseDetails.SearchBookingItemResponse.BookingItems.BookingItem.size() == 2, "Both items are not returned in the response")
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

	}
	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
		softAssert.assertAll()




		where:

		rowNum|testDesc
		6|" Search for an item after booking  -   HBG hotel booking id  "

	}

	/**
	 * SCCR_Booking SearchChargeConditionsRequest
	 * BookingItem 2 charge conditions returns
	 * @return
	 */
	@Unroll
	def "Verify Search Charge Conditions of booked items test #8|| #testDesc"(){

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
		7| " Search for charge condition for booked item   -   HBG hotel booking id  "

	}



/**
 * MBR ModifyBookingRequest
 *●Booking response returns with Agent Ref changed
 * @return
 */
	@Unroll
	def "verify  modify batch Request in a booking #9|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node
		def xmlName = helper.getXmlName()
		def fileContents_bookingref=helper.getModifyBatchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),itemDetails.itemRateKey[0])
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		def expectedValue = itemDetails.itemCity[0].toString()+"|"+itemDetails.itemCode[0].toString()
		if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.ModifyBookingRequest.size() > 0 ){

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
			helper.validateFailureResponse(responseBody)
		}
		else if(helper.getExpectedResult().equals("Failure") && System.getProperty("platform").equals("legacy")){

			helper.validateFailureResponse(responseBody,"ErrorId","GRT1981")
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
	 * MBIR  ModifyBookingItemRequest
	 * ERR0155</ErrorId>
	 <ErrorText>Cannot modify Booking Item</ErrorText>
	 * @return
	 */

	@Unroll
	def "verify modify  item in a booking #10|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {

		def fileContents_bookingref=helper.getModifyBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),itemDetails.itemRateKey[0])
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
 * CBIR_item1  CancelBookingItemRequest
 * ●Item 1 cancelled
 * ●Booking remains confirmed
 * @return
 */
	@Unroll
	def "verify Cancel item in a booking #11|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def result = false
		def fileContents_bookingref=helper.getCancelBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")

		def expectedValue ="Cancelled"+"|"+"Confirmed"
		if(helper.getExpectedResult().equals("Success")){
			result = true
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

		rowNum|testDesc
		10|" cancel an Item in booking verify Item status to be cancelled and booking status is confirmed  -   HBG hotel booking id  "
	}

/**SearthBookingItem
 * Search for an item after cancel
 * @return
 */

	@Unroll
	def "Verify Search for the item after cancel #12|| #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.	getSearchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId())
		println "Request Xml...\n${fileContents_bookingref}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_bookingref, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		logDebug ("Search Response Xml..\n${responseBody_booking}")
		//def expectedValue = "Confirmed"
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
					softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking reference ID does not match")
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
	}
	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
		sleep(5*1000)

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
	def "Verify Cancel  entire booking #13|| #testDesc"(){
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

		bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"X",3)
		softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"),"Booking status is not cancelled in CBS after 5 retries")
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll();
		println "\n\n"
		where:

		rowNum|testDesc
		12|" cancel the booking and validate status -   HBG hotel booking id  "
	}

}
class TwoAdults_1cot_HBG_C1237 extends TwoAdults_1cot_HBG {
	@Override
	String getClientId() {
		return "1237"
	}
}

class TwoAdults_1cot_HBG_C1238 extends TwoAdults_1cot_HBG {
	@Override
	String getClientId() {
		return "1238"
	}
}

class TwoAdults_1cot_HBG_C1239 extends TwoAdults_1cot_HBG {
	@Override
	String getClientId() {
		return "1239"
	}
}

class TwoAdults_1cot_HBG_C1244 extends TwoAdults_1cot_HBG {
	@Override

	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class TwoAdults_1cot_HBGSingleClient extends TwoAdults_1cot_HBG {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
