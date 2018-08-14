package com.hbg.test.RoomTypeBooking

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.RoomTypeBookingHelper.RoomTypeBookingHelper_ASYNC
import com.hbg.test.XSellBaseTest
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll
import com.hbg.util.AS400DatabaseUtil

abstract class RoomTypeBooking_ASYNC extends XSellBaseTest {


	static final String EXCEL_SHEET = new File(ConfigProperties.getValue("roomTypeBookingSheet"))
	static final String Sheet_NAME = "ASYNC"
	//static final String URL = ConfigProperties.getValue("AuthenticationURL")
	static final String SITEID = ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey = ConfigProperties.getValue("FITSiteIdHeaderKey")
	RoomTypeBookingHelper_ASYNC helper = new RoomTypeBookingHelper_ASYNC(EXCEL_SHEET,getClientId())

	int rowNum
	String sheet,testDesc,Data
	private static def xmlParser = new XmlParser()
	SoftAssert softAssert= new SoftAssert()
	XMLValidationUtil XmlValidate = new XMLValidationUtil()
	BookingStatus bookingStatus
	/*static final String CITY = ConfigProperties.getValue("HBG_City_List")
	List cities = CITY.split(",")*/

	static final String GTACITY = ConfigProperties.getValue("City_List")
	List gtaCities = GTACITY.split(",")

	@Shared
	List<HotelDTO> hotelList = new ArrayList<HotelDTO>()

	@Shared
	List<HotelDTO> hotelListSB = new ArrayList<HotelDTO>()
	@Shared
	List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()

	@Shared
	HotelDTO successHotelDTO = null

	@Shared
	HotelDTO gtaSuccessHotelDTO = null
	AS400DatabaseUtil dbconn= new AS400DatabaseUtil()


	@Unroll
	def "Verify Search for HBG TB Room Type #1 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		List cities=numOfCitiesList(8)
		String responseBody
		for(k in 0..cities.size()-1){
			def fileContents_shpr = helper.searchHotelPriceRequest(Sheet_NAME, rowNum, cities[k])
			println "Search Request Xml.. \n${fileContents_shpr}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_shpr,'SearchHotelPriceRequest',FITSiteIdHeaderKey,SITEID)
			//softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			softAssert.assertTrue(node.ResponseDetails.SearchHotelPriceResponse != 0)
			def hotelNode = node.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			responseBody = XmlUtil.serialize(node)
			logDebug ("Search Response Xml..\n${responseBody}")
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			println cities[k]+" city hotels count : "+hList.size()
			if (hList.size() > 0){
				hotelList.addAll(hList)
			}

		}

		println "total hotels count for all the cities : "+hotelList.size()
		softAssert.assertTrue(hotelList.size() > 0,"Available hotel not found for " + cities)

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		1 | "ASYNC - SearchHotelPriceRequest -HBG  "
	}


	@Unroll
	def "Verify Search for GTA for DB room Type #2 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		for(k in 0.. gtaCities.size()-1){
			def fileContents_shpr = helper.searchHotelPriceRequest(Sheet_NAME, rowNum, gtaCities[k])
			println "Search Request Xml.. \n${fileContents_shpr}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_shpr,'SearchHotelPriceRequest',FITSiteIdHeaderKey,SITEID)
			//softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			softAssert.assertTrue(node.ResponseDetails.SearchHotelPriceResponse != 0)
			def hotelNode = node.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			responseBody = XmlUtil.serialize(node)
			logDebug ("Search Response Xml..\n${responseBody}")
			List<HotelDTO> gList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
			println gtaCities[k]+" city hotels count : "+gList.size()
			if (gList.size() > 0){
				gtaHotelList.addAll(gList)
			}


		}

		println "total hotels count for all the cities : "+gtaHotelList.size()
		softAssert.assertTrue(gtaHotelList.size() > 0,"Available GTA hotel not found for " +gtaCities )

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		17 | "ASYNC - SearchHotelPriceRequest -GTA "
	}

	@Unroll
	def "Verify Search Hotel Price BreakDown  #3 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		String responseBody
		if(hotelList.size() != 0) {

			for (int i = 0; i < hotelList.size(); i++) {
				HotelDTO hDTO = hotelList.get(i)
				def fileContents_shpr = helper.hotelPriceBreakdownRequest(Sheet_NAME, rowNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
				println "Search Request Xml.. \n${fileContents_shpr}"
				node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_shpr, 'SearchHotelPriceRequest', FITSiteIdHeaderKey, SITEID)
				responseBody = XmlUtil.serialize(node)
				logDebug("Search Response Xml..\n${responseBody}")

			//	softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				softAssert.assertTrue(node.ResponseDetails.HotelPriceBreakdownResponse != 0)

//			println "Search Response Xml..\n${responseBody}"
				def expectedValue = hDTO.getItemCity().toString() + "|" + hDTO.getItemCode().toString()
				if (helper.getExpectedResult().equals("Success") && node.ResponseDetails.HotelPriceBreakdownResponse.size() > 0) {
					helper.validateSuccessResponse(responseBody, expectedValue)
					break
				} else if (helper.getExpectedResult().equals("Failure")) {
					helper.validateFailureResponse(responseBody)
				} else if (helper.getExpectedResult().equals("Empty")) {
					helper.validateXmlWithEmptyResponse(responseBody)
				} else if (node.ResponseDetails.BookingResponse.Errors.Error.size() > 0) {
					def errorStr = node.ResponseDetails.BookingResponse.Errors.Error.ErrorId.text()
					println "ERROR IN RESPONSE " + errorStr
					errorStr = errorStr + ":" + node.ResponseDetails.BookingResponse.Errors.Error.ErrorText.text()
					println "ERROR IN RESPONSE " + errorStr
					softAssert.assertFalse(node.ResponseDetails.BookingResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
				} else if (node.ResponseDetails.Errors.Error.size() > 0) {
					def Errostring = node.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node.ResponseDetails.Errors.Error.ErrorText.text()
					print "ERROR IN RESPONSE " + Errostring
					softAssert.assertFalse(node.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
				}
				break
			}
		}
		else{
			softAssert.assertTrue(hotelList.size() != 0, "NO HBG Hotels found")
		}
	softAssert.assertAll();4
	println "\n\n"

	where:
		rowNum | testDesc
		2 | "ASYNC - SearchHotelPriceRequest - HBG item "
	}




	@Unroll
	def "Verify Add booking request test 4 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(hotelList.size() != 0) {
			Node node_booking
			String responseBody
			def bookingReferenceNum = null
			def expectedValue
			def result = false
			def xmlName = helper.getXmlName()
			boolean flag = false
			println "hotel size : " + hotelList.size()
			if (hotelList.size() != 0) {
				for (k in 0..hotelList.size() - 1) {
					HotelDTO hDTO = hotelList.get(k)
					bookingReferenceNum = helper.getBookingReference()
					def fileContents = helper.getAddBookingRequestAsync(Sheet_NAME, rowNum, bookingReferenceNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), xmlName)
					println "Search Request Xml.. \n${fileContents}"

					responseBody = RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
					//responseBody = XmlUtil.serialize(node)
					logDebug("Search Response Xml..\n${responseBody}")
					expectedValue = hDTO.getItemCity().toString() + "|" + hDTO.getItemCode().toString() + "|" + "3,4"
					if (responseBody == "") {
						for (j in 1) {
							com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
								String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
								node_booking = RequestNewHelper.getResponses(url, xmlName)
							}
							responseBody = groovy.xml.XmlUtil.serialize(node_booking)
							logDebug("Search Response Xml..\n${responseBody}")
							if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.size() > 0) {
								successHotelDTO = hDTO
								successHotelDTO.setBookingReference(bookingReferenceNum)

								def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
								if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {

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
									bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
									softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")

								}
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
						logDebug("Search Response Xml..\n${responseBody}")
					}
					if(result == true)
						break

				}

				helper.validateSuccessResponse(responseBody, expectedValue)
			}
		}
		else{
			softAssert.assertTrue(hotelList.size() != 0, "NO HBG Hotels found")
		}

			softAssert.assertAll()
			println "\n\n"

		where:
		rowNum | testDesc
		3 | "Create booking with one HBG item "
	}


	@Unroll
	def "Verify search booking test 5 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody
		def xmlName = helper.getXmlName()
		List<String> bookingRefList = new ArrayList<String>()
		List<String> bookingIdList = new ArrayList<String>()

		def fileContents = helper.searchBookingRequestAsync(Sheet_NAME, rowNum,xmlName)
		println "Search Request Xml.. \n${fileContents}"
		responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.size() > 0){
					def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
				//	def expectedValue = "Confirmed"
					for (i in 0.. respBookingRefsize-1)
					{
						if(respBookingRef[i].@ReferenceSource.equals("client"))
						{
							result=true
							if(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString())){
								softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking refrence number not found in the list of booking")
								softAssert.assertTrue(respBookingRef[i+2].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")
								result=true
								break
							}

						}

						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							if(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString())){
								softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")

							}


						}

					}
					softAssert.assertTrue(result==true, "BOOKING ID AND REFREBCE IS NOT FOUND IN LIST OF BOOKING")
					bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
					//helper.validateSuccessResponse(responseBody,expectedValue)
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
		else {
			node_booking = xmlParser.parseText(responseBody)
			logDebug ("Search Response Xml..\n${responseBody}")
		}

		}
		else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		4 | "Search for the Booked item - Date Range  "
	}

	@Unroll
	def "Verify Search Booking With bookingId test 6 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody
		def xmlName = helper.getXmlName()
		def fileContents = helper.searchBookingRequestBookingIDAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(),xmlName)
		println "Search Request Xml.. \n${fileContents}"
		responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.size() > 0){
					def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					//def expectedValue = "Confirmed"
					for (i in 0.. respBookingRefsize)
					{
						if(respBookingRef[i].@ReferenceSource.equals("client"))
						{
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking refrence number not found in the list of booking")
						}

						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")
							break
						}

					}
					softAssert.assertTrue(result==true, "ReferenceSource is not client")
					bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
					//helper.validateSuccessResponse(responseBody,expectedValue)
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
		else {
			node_booking = xmlParser.parseText(responseBody)
			logDebug ("Search Response Xml..\n${responseBody}")
		}
	}
	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		4 | "Search for booking - passing Booking ID"
	}
	

	@Unroll
	def "Verify Price break down test 7 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
			Node node_booking
			String responseBody
			def xmlName = helper.getXmlName()
			def fileContents = helper.BookingItemPriceBreakdownAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(), xmlName)
			println "Search Request Xml.. \n${fileContents}"
			responseBody = RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)

			if (responseBody == "") {
				for (j in 1) {
					com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
						String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
						node_booking = RequestNewHelper.getResponses(url, xmlName)
					}
					responseBody = groovy.xml.XmlUtil.serialize(node_booking)
					logDebug("Search Response Xml..\n${responseBody}")

					// Verify the resonse "response code" and "confirmation status"
					def expectedValue = successHotelDTO.getItemCity().toString() + "|" + successHotelDTO.getItemCode().toString()
					if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.size() > 0) {
						def respBookingRef = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.BookingReference
						def respBookingRefsize = respBookingRef.size()
						def result = false
						for (i in 0..respBookingRefsize) {
							if (respBookingRef[i].@ReferenceSource.equals("client")) {

								result = true
								softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking refrence number not found in the list of booking")
							}

							if (respBookingRef[i].@ReferenceSource.equals("api")) {
								result = true
								softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")
								break
							}

						}
						softAssert.assertTrue(result == true, "ReferenceSource is not client")
						bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
						softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
						String roomId = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.BookingItem.HotelItem.HotelRooms.HotelRoom.@Id[0].toString()
						softAssert.assertTrue(roomId.equals(successHotelDTO.getCategoryId().toString()), "Booking reference ID does not match")
						helper.validateSuccessResponse(responseBody, expectedValue)
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
				logDebug("Search Response Xml..\n${responseBody}")
			}
		}
			else{
				softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
			}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		5 | "price break down for HBG item"
	}
	@Unroll
	def "Verify Search for HBG SB items test #8 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		List cities=numOfCitiesList(8)
		String responseBody
		for(k in 0..cities.size()-1){
			def fileContents_shpr = helper.searchHotelPriceRequest(Sheet_NAME, rowNum, cities[k])
			println "Search Request Xml.. \n${fileContents_shpr}"
			node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_shpr,'SearchHotelPriceRequest',FITSiteIdHeaderKey,SITEID)
			//softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			softAssert.assertTrue(node.ResponseDetails.SearchHotelPriceResponse != 0)
			def hotelNode = node.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			responseBody = XmlUtil.serialize(node)
			logDebug ("Search Response Xml..\n${responseBody}")
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			println cities[k]+" city hotels count : "+hList.size()
			if (hList.size() > 0){
				hotelListSB.addAll(hList)
			}

		}

		println "total hotels count for all the cities : "+hotelListSB.size()
		softAssert.assertTrue(hotelListSB.size() > 0,"Available hotel with SB room Type not found for " + cities)

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		16 | "ASYNC - SearchHotelPriceRequest - HBG SB roomType  "
	}

	@Unroll
	def "Verify add item with room Type SB test 9 || #testDesc"() {
		println("Test Running ...$testDesc")
		Node node_booking
		def xmlName = helper.getXmlName()
		String responseBody
		def result = false

		boolean flag = false
			if(successHotelDTO != null && hotelListSB.size() != 0 ) {
				for (k in 0..hotelListSB.size() - 1) {
					HotelDTO hDTO = hotelListSB.get(k)

					def fileContents = helper.AddBookingItemRequestSbAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), xmlName)
					println "AddBookingItemRequest Xml.. \n${fileContents}"

					responseBody = RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
					//responseBody = XmlUtil.serialize(node)
					logDebug("Search Response Xml..\n${responseBody}")

					if (responseBody == "") {
						for (j in 1) {
							com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
								String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
								node_booking = RequestNewHelper.getResponses(url, xmlName)
							}
							responseBody = groovy.xml.XmlUtil.serialize(node_booking)
							logDebug("Search Response Xml..\n${responseBody}")
							if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
								successHotelDTO.setItemCity(hDTO.getItemCity())
								successHotelDTO.setItemCode(hDTO.getItemCode())
								def expectedValue = successHotelDTO.getItemCity().toString() + "|" + successHotelDTO.getItemCode().toString()
								def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
								def respBookingRefsize = respBookingRef.size()

								for (i in 0..respBookingRefsize) {
									if (respBookingRef[i].@ReferenceSource.equals("client")) {
										result = true
										softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
									}

									if (respBookingRef[i].@ReferenceSource.equals("api")) {
										result = true
										softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")
										break
									}

								}
								softAssert.assertTrue(result == true, "ReferenceSource is not client")
								bookingStatus = dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(), SITEID.toString(), "C", 3)
								softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"), "Booking status is not confirmed in CBS after 5 retries")
								helper.validateSuccessResponse(responseBody, expectedValue)
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
						logDebug("Search Response Xml..\n${responseBody}")
					}
					if(result == true)
						break
				}


			}
		else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}

		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		6 | "Add an item to booking with Room type SB"
	}

	@Unroll
	def "Verify add booking with GTA 10 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody
		def expectedValue
			def result = false
		def xmlName = helper.getXmlName()
			for (k in  0 .. gtaHotelList.size()-1) {
			HotelDTO hDTO = gtaHotelList.get(k)
			def fileContents = helper.AddBookingItemRequestDB_GTA_ASYNC(Sheet_NAME, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(),xmlName)
			println "AddbookingItemREquest Xml.. \n${fileContents}"
			responseBody = RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
			//responseBody = XmlUtil.serialize(node)
			logDebug ("Search Response Xml..\n${responseBody}")

			if (responseBody == "") {
				for (j in 1) {
					com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
						String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
						node_booking = RequestNewHelper.getResponses(url, xmlName)
					}
					responseBody = groovy.xml.XmlUtil.serialize(node_booking)
					logDebug ("Search Response Xml..\n${responseBody}")
					if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
						gtaSuccessHotelDTO = hDTO
						expectedValue = gtaSuccessHotelDTO.getItemCity().toString() +"|"+ gtaSuccessHotelDTO.getItemCode().toString()
						def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
						def respBookingRefsize = respBookingRef.size()

						for (i in 0..respBookingRefsize) {
							if (respBookingRef[i].@ReferenceSource.equals("client")) {
								result = true
								softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
							}

							if (respBookingRef[i].@ReferenceSource.equals("api")) {
								result = true
								softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")
								break
							}

						}
						softAssert.assertTrue(result == true, "ReferenceSource is not client")
						bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
						softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
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
				logDebug ("Search Response Xml..\n${responseBody}")
			}
				if(result == true)
					break
		}

		helper.validateSuccessResponse(responseBody, expectedValue)
		}	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		7 | "Add gta item to booking "
	}


	@Unroll
	def "Verify Search Booking item  11 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody
		def xmlName = helper.getXmlName()
		def fileContents = helper.SearchBookingItemAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(),xmlName)
		println "Search Request Xml.. \n${fileContents}"
		responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchBookingResponse.BookingReferences.BookingReference.size() > 0){
					def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					//def expectedValue = "Confirmed"
					for (i in 0.. respBookingRefsize)
					{
						if(respBookingRef[i].@ReferenceSource.equals("client"))
						{
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking refrence number not found in the list of booking")
						}

						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")
							break
						}

					}
					softAssert.assertTrue(result==true, "ReferenceSource is not client")
					bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
					//helper.validateSuccessResponse(responseBody,expectedValue)
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
		else {
			node_booking = xmlParser.parseText(responseBody)
			logDebug ("Search Response Xml..\n${responseBody}")
		}
	}	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		8 | "Search for booking - passing Booking ID"
	}

	@Unroll
	def "Verify Charge condation test 12 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody
		def xmlName = helper.getXmlName()

		def fileContents = helper.ChargeConditionBookingAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(),xmlName)
		println "ChargeConditionBookingRequest Xml.. \n${fileContents}"
		responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		def expectedValue ="cancellation,amendment"
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")

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
		else {
			node_booking = xmlParser.parseText(responseBody)
			logDebug ("Search Response Xml..\n${responseBody}")
		}
		}	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		9 | "charge condation for the booked item "
	}

	@Unroll
	def "Verify addBooking Security test 13 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		def xmlName = helper.getXmlName()
		String responseBody
			def result = false
		boolean flag = false
			for (int k in  0 .. gtaHotelList.size()-1) {
			HotelDTO hDTO = gtaHotelList.get(k)

			def fileContents = helper.AddBookingItemRequestDB_GTA_ASYNC(Sheet_NAME, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(),xmlName)
			println "AddBookingItemRequest Xml.. \n${fileContents}"

			responseBody = RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
			//responseBody = XmlUtil.serialize(node)
			logDebug ("Search Response Xml..\n${responseBody}")

			if (responseBody == "") {
				for (j in 1) {
					com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
						String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
						node_booking = RequestNewHelper.getResponses(url, xmlName)
					}
					responseBody = groovy.xml.XmlUtil.serialize(node_booking)
					logDebug ("Search Response Xml..\n${responseBody}")
					if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0) {
						successHotelDTO.setItemCity(hDTO.getItemCity())
						successHotelDTO.setItemCode(hDTO.getItemCode())
						def expectedValue = successHotelDTO.getItemCity().toString() +"|"+ successHotelDTO.getItemCode().toString()
						def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
						def respBookingRefsize = respBookingRef.size()

						for (i in 0..respBookingRefsize) {
							if (respBookingRef[i].@ReferenceSource.equals("client")) {
								result = true
								softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking reference number does not match")
							}

							if (respBookingRef[i].@ReferenceSource.equals("api")) {
								result = true
								softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")
								break
							}

						}
						softAssert.assertTrue(result == true, "ReferenceSource is not client")
						bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
						softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
						helper.validateSuccessResponse(responseBody, expectedValue)
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
				logDebug ("Search Response Xml..\n${responseBody}")
			}
				if(result == true  || (k = 50))
					break
		}
	}	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		10 | "Add booking item Security check "
	}


	@Unroll
	def "Verify Modify Agent name test 14 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody
		def xmlName = helper.getXmlName()
		def fileContents = helper.ModifyBookingRequestAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(), xmlName)
		println "ModifyBookingRequest Xml.. \n${fileContents}"
		responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.ModifyBookingRequest.size() > 0){

					def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0..respBookingRefsize - 1) {
						if (respBookingRef[i].@ReferenceSource.equals("agent")) {
							result = true
							softAssert.assertTrue(respBookingRef[i].text().equals("JMB"), "agent details not updated")
							break
						}
					}
					softAssert.assertTrue(result == true, "ReferenceSource is not api")
					break
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

		}	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll()
		println "\n\n"

		where:

		rowNum|testDesc
		11|" Modify by chaging the name of agent and chek the update in response-   HBG hotel booking id  "
	}

	@Unroll
	def "Verify Modify item test 15 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody
		def xmlName = helper.getXmlName()
		def fileContents = helper.ModifyBookingItemRequestAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(), successHotelDTO.getCategoryId(),xmlName)
		println "ModifyBookingItemRequest Xml.. \n${fileContents}"
		responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
							logDebug ("ModifyBookingItemRequest Response Xm..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReferences > 0) {

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
		}
	}	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
				softAssert.assertAll()
				println "\n\n"

		where:
		rowNum | testDesc
		12 | "Modify item deatils and validate error "
	}

	@Unroll
	def "Verify cancel item request test 16 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody
		def xmlName = helper.getXmlName()
		def fileContents = helper.CancelBookingItemRequestAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(),xmlName)
		println "CancelBookingItemRequest Xml.. \n${fileContents}"
		responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)

				logDebug ("CancelBookingItemRequest Response Xml..\n${responseBody}")
				def expectedValue = "Cancelled"
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
					bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
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
		}	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		softAssert.assertAll();
		println "\n\n"

		where:
		rowNum | testDesc
		13 | "cancel one item in booking and check status"
	}

	@Unroll
	def "Verify search for item  test 17 || #testDesc"(){
		println("Test Running ...$testDesc")
		if(successHotelDTO != null ) {
		Node node_booking
		String responseBody

		def xmlName = helper.getXmlName()
		def fileContents = helper.SearchBookingItemAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(),xmlName)
		println "Search Request Xml.. \n${fileContents}"
		responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)
				logDebug ("Search Response Xml..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.size() > 0){
					def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
				//	def expectedValue = "Confirmed"
					for (i in 0.. respBookingRefsize)
					{
						if(respBookingRef[i].@ReferenceSource.equals("client"))
						{
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingReference().toString()), "Booking refrence number not found in the list of booking")
						}

						if(respBookingRef[i].@ReferenceSource.equals("api"))
						{
							result=true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.getBookingId().toString()), "Booking Id not found in the list of booking")
							break
						}

					}
					softAssert.assertTrue(result==true, "ReferenceSource is not client")
					bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"C",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("C"),"Booking status is not confirmed in CBS after 5 retries")
					//helper.validateSuccessResponse(responseBody,expectedValue)
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
		else {
			node_booking = xmlParser.parseText(responseBody)
			logDebug ("search Response Xml..\n${responseBody}")
		}
	}	else{
		softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
	}
		softAssert.assertAll()
		println "\n\n"


		where:
		rowNum | testDesc
		14 | "search for item in booking after cancel"
	}

	@Unroll
	def "Verify cancel the booking test 18 || #testDesc"(){
		if(successHotelDTO != null ) {
		println("Test Running ...$testDesc")
		Node node_booking
		String responseBody
		def xmlName = helper.getXmlName()
		def fileContents = helper.CancelBookingRequestAsync(Sheet_NAME, rowNum, successHotelDTO.getBookingId(),xmlName)
		println "CancelBookingItemRequest Xml.. \n${fileContents}"
		//def expectedValue = "Cancelled"
		 responseBody =RequestNewHelper.postRequestAsynchronously(url, 'application/xml', fileContents, FITSiteIdHeaderKey, SITEID)
		if(responseBody == "") {
			for (j in 1) {
				com.kuoni.qa.automation.requestHelper.RequestNewHelper.retry {
					String url = "http://10.241.102.113/xml/${xmlName}-${j}.xml";
					node_booking = RequestNewHelper.getResponses(url, xmlName)
				}
				responseBody = groovy.xml.XmlUtil.serialize(node_booking)

				logDebug ("Response Response Xml..\n${responseBody}")
				if (helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.BookingResponse.BookingReferences.size() > 0){
					def respBookingRef = node_booking.ResponseDetails.BookingResponse.BookingReferences.BookingReference
					def respBookingRefsize = respBookingRef.size()
					def result = false
					for (i in 0..respBookingRefsize) {
						if (respBookingRef[i].@ReferenceSource.equals("client")) {
							result = true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.bookingReference.toString()), "Booking reference number does not match")
						}

						if (respBookingRef[i].@ReferenceSource.equals("api")) {
							result = true
							softAssert.assertTrue(respBookingRef[i].text().equals(successHotelDTO.bookingId.toString()), "Booking reference ID does not match")

							break
						}

					}
					softAssert.assertTrue(result == true, "ReferenceSource is not client")
					BookingStatus bookingStatus=dbconn.getBookingStatusDetails(successHotelDTO.getBookingId(),SITEID.toString(),"X",3)
					softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"),"Booking status is not Cancelled in CBS after 5 retries")
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

		}	else{
			softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
		}
		//helper.validateSuccessResponse(responseBody,expectedValue)
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc
		15 | "cancel the booking and check the status"
	}
}
class RoomTypeBooking_ASYNC_C1237 extends RoomTypeBooking_ASYNC {
	@Override
	String getClientId() {
		return "1237"
	}
}

class RoomTypeBooking_ASYNC_C1238 extends RoomTypeBooking_ASYNC {
	@Override
	String getClientId() {
		return "1238"
	}
}

class RoomTypeBooking_ASYNC_C1239 extends RoomTypeBooking_ASYNC {
	@Override
	String getClientId() {
		return "1239"
	}
}

class RoomTypeBooking_ASYNC_C1244 extends RoomTypeBooking_ASYNC {
	@Override

	String getClientId() {
		return "1244"
	}
}
@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class RoomTypeBooking_ASYNCSingleClient extends RoomTypeBooking_ASYNC {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}
