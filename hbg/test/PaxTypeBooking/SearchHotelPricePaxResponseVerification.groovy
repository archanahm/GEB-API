package com.hbg.test.PaxTypeBooking

import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import com.hbg.helper.RequestHelper.RequestNewHelper
//import com.kuoni.qa.automation.xmle2e_SearchHotelPricePaxRequest_NewPlatform.SearchHotelPricePaxHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class SearchHotelPricePaxResponseVerification extends Specification{

	static final String SHEET_NAME="xmle2e_AddBookingPax_New"
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("excelSheet"))
	static final String URL= ConfigProperties.getValue("AuthenticationURL")
	static final String SHEET_NAME_SEARCH="xmle2e_SHPPR_Ref"
	static final String SITEID= ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey= ConfigProperties.getValue("FITSiteIdHeaderKey")
	static SearchHotelPricePaxHelper helper=new SearchHotelPricePaxHelper(EXCEL_SHEET)
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
	Map itemMap=null
	Map fields = null
	static final String CITY= ConfigProperties.getValue("HBG_City_List")
	def cities = CITY.split(",")

	@Unroll

	/** Scenario of authentication - verify Response Ref Attribute*/

	def "Verify Authentication test 4 || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody_search

		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getPaxSearchXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search REquest Xml.. \n${fileContents_Search}"
			 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			 responseBody_search = XmlUtil.serialize(node_search)
			println "Search Response Xml..\n${responseBody_search}"
			itemMap = RequestNewHelper.getSearchPaxDetails(node_search,fields,2)
			if(itemMap != null){
				itemDetails.put('itemCode' , itemMap["itemCode"])
				itemDetails.put('itemCity', itemMap["itemCity"])
				itemDetails.put('itemName', itemMap["itemName"])
				itemDetails.put('itemCityName',itemMap["itemCityName"])
				itemDetails.put('roomCategory', itemMap["roomCategory"])
				break
			}

		}
		softAssert.assertFalse(itemCode.equals(""),"Available hotel not found")
		softAssert.assertTrue(node_search.@ResponseReference.startsWith('XA'))
		println "\n	Valid response ... \nResponseReference : ${node_search.@ResponseReference}"
		softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPricePaxResponse != 0)
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum | testDesc | searchRowNum
		1|"Unique Response Ref Attribute - Response Ref Attribute (search with all mandatory criteria)" | 11
	}

	@Unroll
	/** Scenario of authentication - verify Response Ref Attribute*/

	def "Verify add booking request  test 2|| #testDesc"(){
		println("Test Running ...$testDesc")
		def addRef
		def flag= false
		def checkInDate = helper.getCheckInDate()
		def checkOutDate = helper.getCheckOutDate()
		itemDetails.put('bookingRef',helper.getBookingReference())
		Node node

		//create a HBG booking with avilabel item
		def fileContents_booking=helper.getBookPaxXml(SHEET_NAME,rowNum,itemDetails.bookingRef,itemDetails.itemCity[0],itemDetails.itemCode[0],itemDetails.roomCategory[0])
		println "Request Xml...\n${fileContents_booking}"
		Node node_booking =RequestNewHelper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_booking, 'AddBooking', FITSiteIdHeaderKey, SITEID)
		String responseBody_booking = XmlUtil.serialize(node_booking)
		println "Response Xml...\n${responseBody_booking}"

		// Verify the resonse "response code" and "confirmation status"

		if(helper.getExpectedResult().equals("Success")){
			def expectedValue = itemDetails.itemCity[0].toString() +"|"+ itemDetails.itemCode[0].toString()+"|Confirmed"
			helper.validateSuccessResponse(responseBody_booking,expectedValue)
			def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
			def respBookingRefsize = respBookingRef.size()
			def result = false
			for (i in 0.. respBookingRefsize)
			{
				if(respBookingRef[i].@ReferenceSource.equals("client"))
				{
					result=true
					softAssert.assertTrue(respBookingRef[i].text().equals(bookingRef.toString()), "Booking reference number does not match")
					break
				}
			}
			softAssert.assertTrue(result==true, "ReferenceSource is not client")
		}else if(helper.getExpectedResult().equals("Failure")){
			helper.validateFailureResponse(responseBody_booking)
		}else if(helper.getExpectedResult().equals("Empty")){
			helper.validateXmlWithEmptyResponse(responseBody_booking)
		}
		softAssert.assertAll();4
		println "\n\n"


		where:
		rowNum | testDesc | searchRowNum
		248| " Verify add booking request  - Able to book HBG hotel  " | 1


	}
}
