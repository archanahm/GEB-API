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
import org.junit.Assert
import org.testng.asserts.SoftAssert
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll

abstract class ASYNC extends XSellBaseTest{

	static final String SHEET_NAME="xmle2e_AddBookingPax_New"
	static final String SHEET_NAME_ADDBOOKINGITEMREQ="xmle2e_AddBookingItemRequest"
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("paxexcelSheet"))
	//static final String URL= ConfigProperties.getValue("AuthenticationURL")
	static final String SHEET_NAME_SEARCH="ASYNC"


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
	BookingStatus bookingStatus = null
	static final String GTACITY = ConfigProperties.getValue("City_List")
	List gtaCities = GTACITY.split(",")
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

	def "Verify avilable HBG hotel #1 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search
		List cities=numOfCitiesList(4)
		String responseBody
		for(k in 0.. 3 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)

			softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPriceResponse != 0)

			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
			if (hList.size() > 0) {
				hotelListHBG.addAll(hList)
			}
		}

		println "total hotels count for all the cities : "+hotelListHBG.size()
		softAssert.assertTrue(hotelListHBG.size() > 0,"Available hotel not found")


		softAssert.assertAll()
		println "\n\n"
		where:
		rowNum | testDesc | searchRowNum
		1|"search for avilable HBG hotel and save the item details" | 1
	}
	@Unroll
	/**
	 * search for the GTA item untill found any avilable item
	 */
	def "Verify avilable GTA hotel #2 || #testDesc"(){
		println("Test Running ...$testDesc")

		Node node_search
		String responseBody
		for(k in 0.. gtaCities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,gtaCities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)

			softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPriceResponse != 0)

			responseBody = XmlUtil.serialize(node_search)
			logDebug ("Search Response Xml..\n${responseBody}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
			if (hList.size() > 0) {
				hotelListGTA.addAll(hList)
			}
		}


		println "total hotels count for all the cities : "+hotelListGTA.size()
		softAssert.assertTrue(hotelListGTA.size() > 0,"Available hotel not found")

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc | searchRowNum
		1|"search for avilable GTA hotel and save the item details" | 1
	}

	@Unroll
/**AddBookingRequest_receive voucher not email confirmation

 * AddBookingRequestAsync
 * Creat booking an item with HBG
 * @return
 */
	def "Verify Create booking request test #3 || #testDesc"(){
		println("Test Running ...$testDesc")
		def result = false
		Node node_booking
		String responseBody
		def bookingReferenceNum = null
		def expectedValue
		if(hotelListHBG .size() != 0) {
		def xmlName = helper.getXmlName()
		boolean flag = false
		println "hotel size : "+hotelListHBG.size()
		for (int k=0;k<5;k++) {
			HotelDTO hDTO = hotelListHBG.get(k)
			bookingReferenceNum = helper.getBookingReference()
			def fileContents = helper.getAddBookingRequestAsync(SHEET_NAME_SEARCH, rowNum, bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), xmlName)
			println "Search Request Xml.. \n${fileContents}"
			responseBody = RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
			logDebug ("Search Response Xml..\n${responseBody}")
			expectedValue = "1,2"
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")

				// Verify the resonse "response code" and "confirmation status" number of pax

					if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
						successHotelDTO = hDTO
							successHotelDTO.setBookingReference(bookingReferenceNum)

							def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
							if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {

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
								bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
								softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
							}
							softAssert.assertTrue(result == true, "ReferenceSource is not client")


						} else if (helper.getExpectedResult().equals("Failure")) {
							helper.validateFailureResponse(responseBody)
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
						break
					}
			} else {
				node_booking = xmlParser.parseText(responseBody)
				logDebug ("Search Response Xml..\n${responseBody}")
			}
			break

		}
		if( result == true){
			helper.validateSuccessResponse(responseBody, expectedValue)
		}

		}
		else{
			softAssert.assertTrue(hotelListHBG.size() != 0, "NO HBG Hotels found")
		}
			softAssert.assertAll()
			println "\n\n"
		where:
		rowNum | testDesc
		2|"Book an item with child : HBG item booking "
	}
	@Unroll
	/**AddBookingItem_Security***

	* AddBookingRequestAsync
	* Creat booking an item with HBG
	* @return
	*/
	def "Verify AddBookingItem_Security*** test #4 || #testDesc"(){
		println("Test Running ...$testDesc")
		itemDetails.put('randombookingRefNum',helper.getBookingReference())
		Node node_booking
		def xmlName = helper.getXmlName()
		HotelDTO hDTO
		def itemCity = []
		def itemCode = []
		def itemRateKey = []
		if(successHotelDTO != null ) {
		sleep(3*1000)
		for (k in  0 .. 3) {

			hDTO = hotelListHBG.get(k)
			itemCity << hDTO.getItemCity()
			itemCode << hDTO.getItemCode()
			itemRateKey << hDTO.getCategoryId()
		}
		itemDetails.put('itemCity',itemCity)
		itemDetails.put('itemCode',itemCode)
		itemDetails.put('itemRateKey',itemRateKey)

		//create a HBG booking with avilabel item including child
	def fileContents_booking=helper.getAddBookingItemRequestAsync(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),itemDetails.itemCity[0],itemDetails.itemCode[1],itemDetails.itemRateKey[1],xmlName)
	println "Request Xml...\n${fileContents_booking}"
	String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_booking, FITSiteIdHeaderKey, SITEID)
	def expectedValue = "1,2"
	if(responseBody == "") {
		for (j in 1) {
			com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
				String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml"
				node_booking = RequestNewHelper.getResponses(url, xmlName)
			}
			responseBody = groovy.xml.XmlUtil.serialize(node_booking)
			logDebug ("Search Response Xml..\n${responseBody}")

			// Verify the resonse "response code" and "confirmation status" number of pax
			if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
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
				bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
				softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 3 retries")


			} else if (helper.getExpectedResult().equals("Failure")) {
				helper.validateFailureResponse(responseBody)
			} else if (helper.getExpectedResult().equals("Empty")) {
				helper.validateXmlWithEmptyResponse(responseBody)
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
	}
	else {
		node_booking = xmlParser.parseText(responseBody)
		logDebug (" Response Xml..\n${responseBody}")
	}


		helper.validateSuccessResponse(responseBody,expectedValue)
	println "\n\n"
		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
	where:
	rowNum | testDesc
	3|"Book an item with child : HBG item booking "
}
	@Unroll
 /**AddBookingItem_multipleItems
  * AddBookingItemRequest
 * Add new item to already created booking with 2pax and addational cot
 * @return
 **/
	def "Verify add booking request with two items #6 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		Node node_search
		def result = false
		String responseBody_search
		List cities=numOfCitiesList(4)
		String responseBody
		List<HotelDTO> hotelListHBGLocal = new ArrayList<HotelDTO>()
		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)

			softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPriceResponse != 0)

			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
			if (hList.size() > 0) {
				hotelListHBGLocal.addAll(hList)
			}
		}

		println "total hotels count for all the cities : "+hotelListHBGLocal.size()
		softAssert.assertTrue(hotelListHBGLocal.size() > 0,"Available HBG  hotel not found")
		softAssert.assertAll()
		if(successHotelDTO != null && hotelListHBGLocal.size() > 0) {
			def xmlName = helper.getXmlName()
			boolean flag = false
			println "hotel size : "+hotelListHBGLocal.size()
			for (int k=0;k<hotelListHBGLocal.size()-2;k++) {
				HotelDTO hDTO = hotelListHBGLocal.get(k)
				HotelDTO hDTO1 = hotelListHBGLocal.get(k + 1)

				//add new item to already created boooking
				def fileContents_booking = helper.getAddBookingItemRequestMultiItemAsync(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), hDTO1.getItemCity(), hDTO1.getItemCode(), hDTO1.getCategoryId(), xmlName)
				println "Request Xml...\n${fileContents_booking}"
				responseBody = RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_booking, FITSiteIdHeaderKey, SITEID)
				def expectedValue = "3,4"
				if (responseBody == "") {
					for (j in 1) {
						com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
							String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
							node_booking = RequestNewHelper.getResponses(url, xmlName)
						}
						responseBody = groovy.xml.XmlUtil.serialize(node_booking)
						logDebug(" Response Xml..\n${responseBody}")
						if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReference.size() > 0) {
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
							bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
							softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 3 retries")
							softAssert.assertTrue(result == true, "ReferenceSource is not client")

							break
						} else if (helper.getExpectedResult().equals("Failure")) {
							helper.validateFailureResponse(responseBody)
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
					}
				} else {
					node_booking = xmlParser.parseText(responseBody)
					logDebug(" Response Xml..\n${responseBody}")
				}
				if(result == true ){
					helper.validateSuccessResponse(responseBody, expectedValue)
					break
				}
				else if (k == 50){
					break
				}


			}

		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

	softAssert.assertAll()
	println "\n\n"

	where:
	rowNum | testDesc | searchRowNum
	4|"Add new item to already created booking   " | 1
}

	@Unroll
/*AddBookingItem_Single_HBG
	* AddBookingItemRequest
	* Add new GTA item to already created booking with 2pax
	* @return
 */
	def "Verify add booking request single item  #8 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		Node node_search
		def result = false
		String responseBody_search
		List cities=numOfCitiesList(4)
		String responseBody
		List<HotelDTO> hotelListHBGLocal = new ArrayList<HotelDTO>()
		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)

			softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPriceResponse != 0)

			def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
			if (hList.size() > 0) {
				hotelListHBGLocal.addAll(hList)
			}
		}
		def expectedValue
		println "total hotels count for all the cities : "+hotelListHBGLocal.size()
		softAssert.assertTrue(hotelListHBGLocal.size() > 0,"Available HBG  hotel not found")
		softAssert.assertAll()
		if(successHotelDTO != null && hotelListHBGLocal.size() > 0) {
			def xmlName = helper.getXmlName()
			boolean flag = false
			println "hotel size : "+hotelListHBGLocal.size()
			for (int k=0;k<hotelListHBGLocal.size()-1;k++) {
				HotelDTO hDTO = hotelListHBGLocal.get(k)

				//add new item to already created boooking

				def fileContents_booking = helper.getAddBookingItemRequestAsync(SHEET_NAME_SEARCH, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), xmlName)
				println "Request Xml...\n${fileContents_booking}"
				responseBody = RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_booking, FITSiteIdHeaderKey, SITEID)
				if (responseBody == "") {
					for (j in 1) {
						com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
							String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
							node_booking = RequestNewHelper.getResponses(url, xmlName)
						}
						responseBody = groovy.xml.XmlUtil.serialize(node_booking)
						logDebug(" Response Xml..\n${responseBody}")

						// Verify the response "response code" and "confirmation status" number of pax
						 expectedValue = itemDetails.itemCity[0].toString() + "|" + itemDetails.itemCode[0].toString() + "|Confirmed" + "|1,2"

						if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReference.size() > 0) {
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
							bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
							softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 3 retries")
							softAssert.assertTrue(result == true, "ReferenceSource is not client")

							break
						} else if (helper.getExpectedResult().equals("Failure")) {
							helper.validateFailureResponse(responseBody)
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
					}
				} else {
					node_booking = xmlParser.parseText(responseBody)
					logDebug(" Response Xml..\n${responseBody}")
				}
				if(result == true ){
					helper.validateSuccessResponse(responseBody, expectedValue)
					break
				} else if(k == 50){
					break

				}
				}
				}
	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc | searchRowNum
		5|"Add new item to already created booking   " |1
	}
	@Unroll
	/**SearchBooking
	 * getSearchBookingRequestAsync
	 * search the booking after creation
	 * @return
	 */

	def "Search for the created booking test #9|| #testDesc"(){
		println("Test Running ...$testDesc")
		def xmlName = helper.getXmlName()
		sleep(3*1000)
		Node node_booking
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getSearchBookingRequestAsync(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId(),xmlName)
		println "Request Xml...\n${fileContents_bookingref}"
		String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_bookingref, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug (" Response Xml..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.size() > 0){
		def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
		def respBookingRefsize = respBookingRef.size()
		def result = false
	//	def expectedValue = "Confirmed"
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
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 3 retries")
	//	helper.validateSuccessResponse(responseBody,expectedValue)
			} else if (helper.getExpectedResult().equals("Failure")) {
				helper.validateFailureResponse(responseBody)
			} else if (helper.getExpectedResult().equals("Empty")) {
				helper.validateXmlWithEmptyResponse(responseBody)
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
	}
else{
	softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
}
		softAssert.assertAll()
		println "\n\n"
		where:
		testDesc | searchRowNum
		" Search for booking  -  booked HBG hotel booking id  " | 6
	}
/**
 * BookingItemPriceBreakdown
 * verify BookingItemPriceBreakdownResponse
 * @return
 */
	@Unroll
	def "Search for booking item price break down #10 || #testDesc"(){
		println("Test Running ...$testDesc")
		def xmlName = helper.getXmlName()
		sleep(3*1000)
		Node node_booking
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getBookingItemPriceBreakdownRequestAsync(SHEET_NAME_SEARCH,searchRowNum,successHotelDTO.getBookingId(),xmlName)
		println "Request Xml...\n${fileContents_bookingref}"
		String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_bookingref, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug (" Response Xml..\n${responseBody}")

				// Verify the resonse "response code" and "confirmation status"
				def expectedValue = itemDetails.itemCity[0].toString() + "|" + itemDetails.itemCode[0].toString()
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.size() > 0){
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
					//helper.validateSuccessResponse(responseBody, expectedValue)
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody)
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
	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		//validate room ID
		String roomId = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id[0].toString()
		//softAssert.assertTrue(roomId.equals(successHotelDTO.getCategoryId().toString()), "room Category  ID does not match")
		softAssert.assertAll()
		println "\n\n"

		where:

		testDesc | searchRowNum
		" Search for booking item price break down pax  -   HBG hotel booking id  " | 7
	}

	/**
	 * SCCR_Booking getSearchChargeConditionsRequestAsync
	 * BookingItem 1 charge conditions returns
	 * @return
	 */
	@Unroll
	def "Search Charge Conditions of bookied items test #11|| #testDesc"(){

		println("Test Running ...$testDesc")
		def xmlName = helper.getXmlName()
		sleep(3*1000)
		Node node_booking
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getSearchChargeConditionsRequestAsync(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),xmlName)
		println "Request Xml...\n${fileContents_bookingref}"
		String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_bookingref, FITSiteIdHeaderKey, SITEID)
		def expectedValue ="cancellation,amendment"
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug (" Response Xml..\n${responseBody}")

				// Verify the resonse "search condation response" expacted value feccthed from excel

				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.size() > 0){
					helper.validateSuccessResponse(responseBody,expectedValue)

				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody)
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

		print "*************DELAY ADDING SCRIPT ***********************"
		def fileContents_booking=helper.getSearchBookingRequestAsync(SHEET_NAME_SEARCH,6,successHotelDTO.getBookingId(),xmlName)
		println "Request Xml...\n${fileContents_booking}"
		String responseBodystr =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_booking, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug (" Response Xml..\n${responseBody}")
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
		8| " Search for charge condition for booked item   -   HBG hotel booking id  "

	}


	@Unroll
	def "verify modify  request #13|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		if(successHotelDTO != null ) {
		def xmlName = helper.getXmlName()
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
				logDebug (" Response Xml..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.ModifyBookingRequest.size() > 0){

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

	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:

		rowNum|testDesc
		10|" Modify by adding one more pax and verify error-   HBG hotel booking id  "
	}


/**
 * CBIR_item1  CancelBookingItemRequest
 * �?Item 1 cancelled
 * �?Booking remains confirmed
 * @return
 */
	@Unroll
	def "verify Cancel item in a booking #15|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getCancelBookingItemRequestAsync(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),xmlName)
		println "Request Xml...\n${fileContents_bookingref}"
		String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_bookingref, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug (" Response Xml..\n${responseBody}")

				def expectedValue = "Cancelled" + "|Confirmed"
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
					helper.validateSuccessResponse(responseBody, expectedValue)
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody)
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
	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}

		softAssert.assertAll();
		println "\n\n"
		where:

		rowNum|testDesc
		11|" cancel an Item in booking verify Item status to be cancelled and booking status is confirmed  -   HBG hotel booking id  "
	}



/**SearthBookingItem
 * Search for an item after cancel
 * @return
 */

	@Unroll
	def "Search for the item after cancel #16|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.	getSearchBookingItemRequestAsync(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),xmlName)
		println "Request Xml...\n${fileContents_bookingref}"
		String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_bookingref, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug (" Response Xml..\n${responseBody}")

				//validate response
				//validate SearchBookingItemResponse returned
				//def expectedValue = "Confirmed"
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchBookingItemResponse.BookingReferences.size() > 0){
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
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")

							break
						}

					}
					softAssert.assertTrue(result == true, "ReferenceSource is not client")
					bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 3 retries")
					//helper.validateSuccessResponse(responseBody, expectedValue)
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody)
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

	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		where:

		rowNum|testDesc
		12|" Search for an item after cancel of any item  -   HBG hotel booking id  "

	}
/**CancelBookingRequest
 * cancel entier booking
 * �?All Items status = Cancelled
 * �?The Gross value = "0.00"   Nett="0.00"
 * �?Booking cancelled in CBS and GCRES
 * @return
 */
	@Unroll
	def "Cancel  entire booking #17|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_booking
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null ) {
		def fileContents_bookingref=helper.getCancelBookingRequestAsync(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),xmlName)
		println "Request Xml...\n${fileContents_bookingref}"
		String responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents_bookingref, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug (" Response Xml..\n${responseBody}")


			//	def expectedValue = "Cancelled"
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
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
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking ID does not match")

							break
						}

					}
					softAssert.assertTrue(result == true, "ReferenceSource is not client")
					bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"X",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"),"Booking status is not Cancelled in CBS after 3 retries")
				//	helper.validateSuccessResponse(responseBody,expectedValue)
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody)
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
	}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"
		where:

		rowNum|testDesc
		12|" cancel the booking and validate status-   HBG hotel booking id  "
	}


}
class ASYNC_C1237 extends ASYNC {
	@Override
	String getClientId() {
		return "1237"
	}
}
class ASYNC_C1238 extends ASYNC {
	@Override
	String getClientId() {
		return "1238"
	}
}

class ASYNC_C1239 extends ASYNC {
	@Override
	String getClientId() {
		return "1239"
	}
}

class ASYNC_C1244 extends ASYNC {
	@Override

	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class ASYNCSingleClient extends ASYNC {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
        println "Using client Id $clientId"
		return clientId
	}
}
