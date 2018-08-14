package com.hbg.test.PaxTypeBooking

import com.hbg.dto.BookingStatus
import com.hbg.util.AS400DatabaseUtil
import spock.lang.IgnoreIf
import spock.lang.Unroll

import java.util.List

import org.testng.asserts.SoftAssert;
import com.hbg.test.XSellBaseTest
import com.hbg.test.PriceSearch.SearchHotelPaxPrice.SearchHotelPricePaxRequestXsell
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.kuoni.finance.automation.xml.util.ConfigProperties

import groovy.xml.XmlUtil
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
	
	
abstract class TwoPax_TwoExtra_OneCot_HBG extends XSellBaseTest
{

		static final String SHEET_NAME="2Adults+2extra+1cot_HBG"
		static final String SHEET_NAME_ADDBOOKINGITEMREQ="xmle2e_AddBookingItemRequest"
		static final String EXCEL_SHEET=new File(ConfigProperties.getValue("paxexcelSheet"))
		static final String URL= ConfigProperties.getValue("AuthenticationURL")
		static final String SHEET_NAME_SEARCH="2Adults+2extra+1cot_HBG"
		String bookingIdnumber
		static final String SHEET_NAME_SEARCHITEMINFO="xmle2e_SearchItemInfo_New"

		static final String SITEID= ConfigProperties.getValue("SiteId")
		static final String FITSiteIdHeaderKey= ConfigProperties.getValue("FITSiteIdHeaderKey")
		SearchHotelPricePaxHelper helper = new SearchHotelPricePaxHelper(EXCEL_SHEET,getClientId())
		int searchRowNum
		def numberOfChildren
		def numberofCots
		def numberOfAdults
	def itemRefrnece
		String PaxId
		private static def xmlParser = new XmlParser()
		SoftAssert softAssert = new SoftAssert()
		Node node_search
		String responseBody_search
		int rowNum
		BookingStatus bookingStatus = null
	AS400DatabaseUtil dbconn= new AS400DatabaseUtil()
		int k
		@Shared
		def itemDetails = [:]
		@Shared
		def itemCode = []
		@Shared
		def roomCategory = []
		@Shared
		def itemCity = []
		@Shared
		def itemCityName = []
		@Shared
		def itemName = []
		int hotelCount =0
		def flag = false
		Map itemMapGTA=null
		Map itemMapHBG=null
		Map fields = null
		
		@Shared
		List cities
		
		@Shared
		List<HotelDTO> hotelList = new ArrayList<HotelDTO>()
	
		@Shared
		List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()
	
		@Shared
		HotelDTO successHotelDTO = null

		/** 2Adutls+2Extra+1Cot - SHPPR  */
		def "1. 2Adutls+2Extra+1Cot - SHPPR"()
		{
			println("2Adults+2Extra+1Cot - SHPPR Request")
			cities=numOfCitiesList(8)
			if (cities.contains("CAS")){
				cities.remove("CAS")
			}
			for(k in 0.. cities.size()-1 )
			{
				def fileContents_Search = helper.get2beds2extra1cot_SHPPR(SHEET_NAME_SEARCH,searchRowNum,cities[k])
				println "SHPPR Search Request Xml.. \n${fileContents_Search}"
				 node_search = helper.postHttpsXMLRequestWithHeaderNode(URL,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)
				//println "Search Response Xml..\n${responseBody_search}"
				logDebug("Search Response Xml..\n${responseBody_search}")
				//itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)
				def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
				List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

				if (hList.size() > 0)
				{
					hotelList.addAll(hList)
				}
				
				List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)

				if (gtaHList.size() > 0)
				{
					gtaHotelList.addAll(gtaHList)
					//hotelList.addAll(gtaHList)
				}

			}
			println "total hotels count for all the cities : "+hotelList.size()
			softAssert.assertTrue(hotelList.size() > 0,"HBG hotels not found for 2Adults+2Extra+1Cot - SHPPR Request" +cities )
			softAssert.assertAll()
			println "\n\n"
			where:
			rowNum | testDesc | searchRowNum
			1|"City Search "  | 1
}
		
		/** 2Adutls+2Extra+1Cot - AddBookingRequest  */
		def "2. 2Adutls+2Extra+1Cot - AddBookingRequest - Pass BookingRefrence as Input"()
		{
			println("2Adults+2Extra+1Cot - AddBooking Request")

			def addRef
				def expectedValue
				def result = false
				String responseBody_booking
			def checkInDate = helper.getCheckInDate()
			def checkOutDate = helper.getCheckOutDate()
			def bookingReferenceNum = null

			Node node
			if(hotelList.size() != 0) {
					println "Hotel size : " + hotelList.size()
					for (k in  0 .. hotelList.size()-1) {
						HotelDTO hDTO = hotelList.get(k)
						bookingReferenceNum = helper.getBookingReference()
				//create a HBG booking with available Item
				def fileContents_booking=helper.getBookingfor2Adults2Extra1cot(SHEET_NAME,rowNum,bookingReferenceNum,hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(),numberOfChildren,numberofCots,numberOfAdults)
				println "Request Xml.ADDBOOKING..\n${fileContents_booking}"
				Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
				responseBody_booking = XmlUtil.serialize(node_booking)
				logDebug("Search Response Xml..\n${responseBody_booking}")
				// Verify the resonse "response code" and "confirmation status"
				expectedValue = hDTO.getItemCity().toString() + "|" + hDTO.getItemCode().toString()
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
					helper.validateSuccessResponse(responseBody_booking,expectedValue)
				}
			}
			else{
				softAssert.assertTrue(hotelList.size()  != 0, "NO HBG Hotels found")
			}

			softAssert.assertAll()
			println "\n\n"

		where:
		rowNum | testDesc | searchRowNum | numberOfChildren |  numberofCots | numberOfAdults
		2| " Verify add booking request  - Able to book HBG hotel  " | 1 | 2 | 1 | 2
		}
		

		/** 2Adutls+2Extra+1Cot - ABIR_2Adults - Adding an extra hotel for existing booking */
		def "3. 2Adutls+2Extra+1Cot - ABIR_2Adults - Pass BookingID as Input"()
		{
			println("2Adults+2Extra+1Cot - AddBooking ItemRequest with 2 Adults")				
				def addRef
				def flag = false
				def checkInDate = helper.getCheckInDate()
				def checkOutDate = helper.getCheckOutDate()
				Node node
				def result = false
				def expectedValue
			if(successHotelDTO != null ) {
					println "Hotel size : " + hotelList.size()
					for (k in 0..hotelList.size() - 1) {
						HotelDTO hDTO = hotelList.get(k)
						def fileContents_booking = helper.getABIR2adults(SHEET_NAME, rowNum, successHotelDTO.getBookingId(), hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), numberOfChildren, numberofCots, numberOfAdults)
						println "Request Xml...\n${fileContents_booking}"
						Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
						String responseBody_booking = XmlUtil.serialize(node_booking)
						logDebug("Response Xml...\n${responseBody_booking}")
						//Verify the response "response code" and "Confirmation Status"
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

						if(result == true ){
							helper.validateSuccessResponse(responseBody_booking, expectedValue)
							break
						} else if(k == 50) {
							break
						}

					}
				}
					else{
						softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
					}

				softAssert.assertAll();
				println "\n\n"


				where:
		rowNum | testDesc | searchRowNum | numberOfChildren |  numberofCots | numberOfAdults
		2| " Verify add booking request  - Able to book HBG hotel  " | 1 | 0 | 1 | 2
	
	}


	/** 2Adutls+2Extra+1Cot - Search Booking  Pass booking Id as Input*/
		def "4. 2Adutls+2Extra+1Cot - Search Booking"()
		{
			println("2Adults+2Extra+1Cot - Search Booking with Booking Id as Input")

			def addRef
			def result= false
			def checkInDate = helper.getCheckInDate()
			def checkOutDate = helper.getCheckOutDate()
			if(successHotelDTO != null ) {

			//create a HBG booking with available Item
			def fileContents_booking=helper.getsearchbookingdetails(SHEET_NAME,rowNum,successHotelDTO.getBookingId())
			println "Search Request Xml after Booking...\n${fileContents_booking}"
			Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
			String responseBody_booking = XmlUtil.serialize(node_booking)
			logDebug("Response Xml...\n${responseBody_booking}")
			 //Verify the response "response code" and "Confirmation Status"
			
			if(helper.getExpectedResult().equals("Success") &&  node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.size() > 0) {
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

			}else if (helper.getExpectedResult().equals("Empty")) {
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
			else{
				softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
			}

				softAssert.assertAll()
			println "\n\n"
	
		
		where:
		rowNum | testDesc
		2| " Verify Search Booking with Booking Id as Input  "
	
		}
		
		
		/** 2Adutls+2Extra+1Cot - Booking Item Price BreakDown*/
			def "5. 2Adutls+2Extra+1Cot - Booking Item PriceBreakDown"()
			{
				println("2Adults+2Extra+1Cot - BookingItemPriceBreakDown Request")

				def addRef
				def flag= false
				def checkInDate = helper.getCheckInDate()
				def checkOutDate = helper.getCheckOutDate()
				if(successHotelDTO != null ) {
					
				//create a HBG booking with available Item
				def fileContents_booking=helper.getbookingitempriceBreakdown(SHEET_NAME,rowNum,successHotelDTO.getBookingId(), itemRefrnece)
				println "Request Xml...\n${fileContents_booking}"
				Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
				String responseBody_booking = XmlUtil.serialize(node_booking)
					logDebug("Response Xml...\n${responseBody_booking}")
				 //Verify the response "response code" and "Confirmation Status"

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

					} else if (helper.getExpectedResult().equals("Failure")) {
						helper.validateFailureResponse(responseBody_booking)
					} else if (helper.getExpectedResult().equals("Empty")) {
						helper.validateXmlWithEmptyResponse(responseBody_booking)
					}
					else if (node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.Errors.Error.size() > 0) {
						def errorStr = node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.Errors.Error.ErrorId.text()
						println "ERROR IN RESPONSE " + errorStr
						errorStr = errorStr + ":" + node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.Errors.Error.ErrorText.text()
						println "ERROR IN RESPONSE " + errorStr
						softAssert.assertFalse(node_booking.ResponseDetails.BookingItemPriceBreakdownResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
					} else if (node_booking.ResponseDetails.Errors.Error.size() > 0) {
						def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
						print "ERROR IN RESPONSE " + Errostring
						softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
					}

					helper.validateSuccessResponse(responseBody_booking, expectedValue)
				}
				else{
					softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
				}

				softAssert.assertAll();
				println "\n\n"
		
			
			where:
			rowNum | testDesc | itemRefrnece
			2| " Verify BookingItemPriceBreakDown Request" | 1
		
			}

			
			
			/** 2Adutls+2Extra+1Cot - ChargeConditionsBookingItem*/
			def "6. 2Adutls+2Extra+1Cot - ChargeConditionsBookingItem"()
			{
				println("2Adults+2Extra+1Cot -ChargeConditionsBookingItem Request")

					def addRef
					def flag = false
					def checkInDate = helper.getCheckInDate()
					def checkOutDate = helper.getCheckOutDate()
				if(successHotelDTO != null ) {
					def fileContents_booking = helper.getchargeconditionsbookingitem(SHEET_NAME, rowNum, successHotelDTO.getBookingId(), itemRefrnece)
					println "Request Xml...\n${fileContents_booking}"
					Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
					String responseBody_booking = XmlUtil.serialize(node_booking)

					logDebug("Response Xml...\n${responseBody_booking}")
					if(helper.getExpectedResult().equals("Success") && node_booking.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.size() > 0){
						helper.validateSuccessResponse(responseBody_booking)

					}else if(helper.getExpectedResult().equals("Failure")){
						helper.validateFailureResponse(responseBody_booking)
					}else if(helper.getExpectedResult().equals("Empty")){
						helper.validateXmlWithEmptyResponse(responseBody_booking)
					}
					else if (node_booking.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
						def errorStr = node_booking.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
						println "ERROR IN RESPONSE " + errorStr
						errorStr = errorStr + ":" + node_booking.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
						println "ERROR IN RESPONSE " + errorStr
						softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
					}
					else if(node_booking.ResponseDetails.Errors.Error.size() > 0) {
						def Errostring = node_booking.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_booking.ResponseDetails.Errors.Error.ErrorText.text()
						print "ERROR IN RESPONSE " + Errostring
						softAssert.assertFalse(node_booking.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
					}
				}
				else{
					softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
				}

				softAssert.assertAll()
				println "\n\n"


			
			where:
			rowNum | testDesc | itemRefrnece
			7| " Verify ChargeConditionsBookingItem Request  " | 1
		
			}
	@Unroll
	def "7. Verify modify batch item in a booking #13|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null) {
			def fileContents_bookingref=helper.getModifyBatchBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId())
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

		rowNum|testDesc
		11|" Modify agent details In booking -   HBG item  "
	}


	@Unroll
	def "8. Verify modify batch Request in a booking #14|| #testDesc"(){
		println("Test Running ...$testDesc")
		Node node
		def xmlName = helper.getXmlName()
		if(successHotelDTO != null) {
			def fileContents_bookingref=helper.getModifyBookingItemRequest(SHEET_NAME_SEARCH,rowNum,successHotelDTO.getBookingId(),successHotelDTO.getCategoryId())
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

		rowNum|testDesc
		12|" Modify batch booking details In booking -   HBG item  "
	}

	/** 2Adutls+2Extra+1Cot - 	CBIR - Cancel Booking Item Request*/
			def "9. 2Adutls+2Extra+1Cot - CancelBookingItemRequest"()
			{
				println("2Adults+2Extra+1Cot - Cancel Booking Item Request")

					def addRef
					def flag = false
					def checkInDate = helper.getCheckInDate()
					def checkOutDate = helper.getCheckOutDate()

					if(successHotelDTO != null ) {
					def fileContents_booking = helper.cancelBookingItemRequest(SHEET_NAME, rowNum, successHotelDTO.getBookingId(), itemRefrnece)
					println "Request Xml...\n${fileContents_booking}"
					Node node_booking = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
					String responseBody_booking = XmlUtil.serialize(node_booking)
						logDebug("Response Xml...\n${responseBody_booking}")

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
			rowNum | testDesc | itemRefrnece
			10| " Verify Cancel Booking Item Request  " | 1
		
			}
			
			/** 2Adutls+2Extra+1Cot - Search BookingItem details after cancellation  */
			def "10. 2Adutls+2Extra+1Cot - Search BookingItem Details"()
			{
				println("2Adults+2Extra+1Cot - SearchBookingItem Details After Cancellation")

				def addRef
				def flag= false
				def checkInDate = helper.getCheckInDate()
				def checkOutDate = helper.getCheckOutDate()

				if(successHotelDTO != null ) {
				//create a HBG booking with available Item
				def fileContents_booking=helper.getsearchbookingitemdetails(SHEET_NAME,rowNum,successHotelDTO.getBookingId(),false)
				println "Request Xml...\n${fileContents_booking}"
				Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
				String responseBody_booking = XmlUtil.serialize(node_booking)
				//println "Response Xml...\n${responseBody_booking}"
				logDebug("Response Xml...\n${responseBody_booking}")
				 //Verify the response "response code" and "Confirmation Status"

					if(helper.getExpectedResult().equals("Success")){
						def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
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

				}
				else{
					softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
				}

				softAssert.assertAll()
				where:
			rowNum | testDesc
			2| " Verify SearchBookingItem Details After Cancellation  "
		
			}
			
			/** 2Adutls+2Extra+1Cot - 	CBIR - Cancel Booking  Request for all items*/
			def "11. 2Adutls+2Extra+1Cot - CancelBookingRequest - All Items "()
			{
				println("2Adults+2Extra+1Cot - Cancel Booking Request for all Items")


				def addRef
				def flag= false
				def checkInDate = helper.getCheckInDate()
				def checkOutDate = helper.getCheckOutDate()
				if(successHotelDTO != null ) {
				//create a HBG booking with available Item
				def fileContents_booking=helper.cancelBooking(SHEET_NAME,rowNum,successHotelDTO.getBookingId())
				println "Request Xml...\n${fileContents_booking}"
				Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
				String responseBody_booking = XmlUtil.serialize(node_booking)
				//println "Response Xml...\n${responseBody_booking}"
				logDebug("Response Xml...\n${responseBody_booking}")

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

				}
				else{
					softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
				}

				softAssert.assertAll()
				println "\n\n"
				where:
			rowNum | testDesc
			2| " Verify Cancel Booking Request for all Items "
		
			}
			
			/** 2Adutls+2Extra+1Cot - Search BookingItem details after full cancellation  */
			def "12. 2Adutls+2Extra+1Cot - Search BookingItem Details  -after full cancellation"()
			{
				println("2Adults+2Extra+1Cot - Searching Booking Details After Cancellation")

				
				def addRef
				def flag= false
				def checkInDate = helper.getCheckInDate()
				def checkOutDate = helper.getCheckOutDate()

				if(successHotelDTO != null ) {
					
				//create a HBG booking with available Item
				def fileContents_booking=helper.getsearchbookingitemdetails(SHEET_NAME,rowNum,successHotelDTO.getBookingId(),false)
				println "Request Xml...\n${fileContents_booking}"
				Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
				String responseBody_booking = XmlUtil.serialize(node_booking)
				//println "Response Xml...\n${responseBody_booking}"
				logDebug("Response Xml...\n${responseBody_booking}")
					String SearchBookingItemResponse=node_booking.ResponseDetails.SearchBookingItemResponse
					softAssert.assertTrue(SearchBookingItemResponse.size() != 0, "SearchBookingItemResponse does not return")

					def expectedValue = "Cancelled"
					if(helper.getExpectedResult().equals("Success")){
						def respBookingRef = node_booking.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
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

				}
				else{
					softAssert.assertTrue(successHotelDTO != null, "Booking is not successful in first Step")
				}

				softAssert.assertAll()


				println "\n\n"
				where:

				rowNum | testDesc
			2| " Verify Searching Booking Details After Cancellation  "
		
			}
}

class TwoPax_TwoExtra_OneCot_HBG_C1237 extends TwoPax_TwoExtra_OneCot_HBG {
	@Override
	String getClientId() {
		return "1237"
	}
}

class TwoPax_TwoExtra_OneCot_HBG_C1238 extends TwoPax_TwoExtra_OneCot_HBG {
	@Override
	String getClientId() {
		return "1238"
	}
}

class TwoPax_TwoExtra_OneCot_HBG_C1239 extends TwoPax_TwoExtra_OneCot_HBG {
	@Override
	String getClientId() {
		return "1239"
	}
}

class TwoPax_TwoExtra_OneCot_HBG_C1244 extends TwoPax_TwoExtra_OneCot_HBG {
	@Override
	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class TwoPax_TwoExtra_OneCot_HBGSingleClient extends TwoPax_TwoExtra_OneCot_HBG {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}

