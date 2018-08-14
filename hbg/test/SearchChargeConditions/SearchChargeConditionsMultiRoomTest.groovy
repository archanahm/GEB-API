package com.hbg.test.SearchChargeConditions

import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPriceHelper
import com.hbg.test.XSellBaseTest
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Unroll

import static org.junit.Assert.assertTrue

abstract class SearchChargeConditionsMultiRoomTest extends XSellBaseTest{
	
	static final String SHEET_NAME="xmle2e_HBG_SHPR_Ref"
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("priceSearchSheet"))
	static final String URL= ConfigProperties.getValue("AuthenticationURL")
	static final String SHEET_NAME_SEARCH="xmle2e_HBG_SHPR_Ref"
	static final String SITEID= ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey= ConfigProperties.getValue("FITSiteIdHeaderKey")
	//static SearchHotelPriceHelper helper=new SearchHotelPriceHelper(EXCEL_SHEET,getClientId())
	SearchHotelPriceHelper helper = new SearchHotelPriceHelper(EXCEL_SHEET,getClientId())
	int rowNum
	String sheet,testDesc,Data
	private static def xmlParser = new XmlParser()
	SoftAssert softAssert= new SoftAssert()
	XMLValidationUtil XmlValidate = new XMLValidationUtil()
	int searchRowNum
	int numberOfCots
	int numberOfRooms
	int childAge
	int duration
	String dateFormatType
	String romeCode
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
	Map itemHbgMap=null
	Map hotelMap=null
	Map fields = null
	//static final String CITY= ConfigProperties.getValue("HBG_City_List")
	//def cities = CITY.split(",")
	@Shared
	List cities
	
	@Shared
	List<HotelDTO> hotelList = new ArrayList<HotelDTO>()

	@Shared
	List<HotelDTO> hotelList2Room = new ArrayList<HotelDTO>()
	@Shared
	List<HotelDTO> hotelList2RoomandCot = new ArrayList<HotelDTO>()
	@Shared
	List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()

	@Shared
	HotelDTO successHotelDTO = null
	
	//List 
	@Unroll

	/** SearchChargeConditions - Search Hotel Price Request */
	
	def "1. Search Charge Condtiions - ||#testDesc "(){
		println("1.Search Charge Conditions - Search Hotel Price Request")
		Node node_search
		String responseBody_search
		cities=numOfCitiesList(6)
		Map inputData = [dateformat:dateFormatType,room : romeCode,cots:numberOfCots, rooms:numberOfRooms, staydays:duration ]
		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getSHPRForMultiRoom(SHEET_NAME_SEARCH,searchRowNum,cities[k],inputData)
			println "Search Request for SCCR Multi Room.. \n${fileContents_Search}"
			 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			 responseBody_search = XmlUtil.serialize(node_search)
			
			logDebug("Response Xml.for Search ChargeConditions\n${responseBody_search}")			
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)	
			
			if (hList.size() > 0){
				hotelList.addAll(hList)
				
			}
		}		
			
			softAssert.assertTrue(hotelList.size() > 0,"HBG Hotels not found for Search Charge Conditions")			
			softAssert.assertAll()
			println "\n\n"

		where:
		 testDesc 				| searchRowNum | dateFormatType | romeCode | numberOfCots | numberOfRooms | duration
		"Search for avilable item with DateFormat - 3TB" 	| 1 | "true" | "TB" | 0 | 3 | 1
}

/** SearchChargeConditions - Search Hotel Price Request WithDayFormatResponse*/
	@Unroll
	def "2. Search Charge Condtions - ||#testDesc "(){
	println("2. Search Charge Condtions - WithDayFormatResponse")
	Node node_search
	String responseBody_search
	int searchchargeconditions
        Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, staydays:duration ]
		if(hotelList.size() != 0) {
			println "Hotel size : " + hotelList.size()
			for (k in  0 .. hotelList.size()-1) {
				HotelDTO hDTO = hotelList.get(k)
                def fileContents_Search = helper.getSCCRWithDurationDayResponse(SHEET_NAME_SEARCH, rowNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(),inputData)
				println "Search Request for SCCR Day Format.. \n${fileContents_Search}"
                node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey,SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                logDebug("Search Response Xml..\n${responseBody_search}")
                println("Search Response Xml..\n${responseBody_search}")

                searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
                if(searchchargeconditions> 0){
                    println "total count"+ searchchargeconditions
                    softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed")
                    int chargeconditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
                    for(int i in 0 ..chargeconditionCnt-1){

                        int conditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size()

                        softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[0].@Type.equals("cancellation"), "cancellation Charge Type Not returned " )
                        softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[1].@Type.equals("amendment"), "Amendment Charge Type Not returned " )
                        if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].@Type.equals("cancellation")) {
                            for (int j in 0..conditionCnt - 1) {
                                if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@Charge.equals("true")){
                                   def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@FromDay
                                   def today = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@ToDay
                                    softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
                                    softAssert.assertTrue(today.isNumber(), "Correct to  day format is not displayed when charge is true : ACTUAL = " + today)
                                }
                                else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@Charge.equals("false")){
                                    def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@FromDay
                                    softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
                                }
                            }
                        }else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].@Type.equals("amendment")){
                            if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition.size() >0){
                                def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[0].@FromDay
                                softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
                            }
                        }

                    }
                       break
                }
            }
				 softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed even after trying fo " + hotelList.size() + " rate keys")
        }
        else{
            softAssert.assertTrue(hotelList.size()  != 0, "NO HBG Hotels found")
        }


        softAssert.assertAll()
	where:
	rowNum 	| testDesc 				|  romeCode | numberOfCots | numberOfRooms | duration
	2		|"SCCR Day Format for Room Code TB and number of rooms 3 " 		| "TB" | 0 | 1 | 1
}

    @Unroll

def "3.Search Charge Condtiions - ||#testDesc"(){
	println("3.Search Charge Condtiions - WithDayFormatResponse")
	Node node_search
	String responseBody_search
    int searchchargeconditions
    Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, staydays:duration ]
    if(hotelList.size() != 0) {
        println "Hotel size : " + hotelList.size()
        for (k in  0 .. hotelList.size()-1) {
            HotelDTO hDTO = hotelList.get(k)
            List roomid = [hDTO.getCategoryId(), hDTO.getCategoryId(),hDTO.getCategoryId()]
				def fileContents_Search = helper.getSCCRWithDurationDayResponse3RateKeys(SHEET_NAME_SEARCH,rowNum,hDTO.getItemCity(), hDTO.getItemCode(), roomid,inputData)
                println "Search Request Xml.. \n${fileContents_Search}"
				 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)		
				logDebug("Search Response Xml..\n${responseBody_search}")
				 searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
					if(searchchargeconditions> 0){
						println "total count"+ searchchargeconditions
						softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed")
						int chargeconditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
						for(int i in 0 ..chargeconditionCnt-1){

							int conditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size()
                            softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[0].@Type.equals("cancellation"), "cancellation Charge Type Not returned " )
                            softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[1].@Type.equals("amendment"), "Amendment Charge Type Not returned " )

                            if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("cancellation")) {
								for (int j in 0..conditionCnt - 1) {
									if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("true")){
                                        def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDay
                                        def today = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@ToDay
                                        softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
                                        softAssert.assertTrue(today.isNumber(), "Correct to  day format is not displayed when charge is true : ACTUAL = " + today)
									}
									else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("false")){
                                        def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDay
                                        softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )

									}
								}
							}else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("amendment")){
								if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size() >0) {
                                    def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[0].@FromDay
                                    softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
								}
							}

						}

						break
					}

				}
				 softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed even after trying for " + hotelList.size() + " rate keys")

			 }else
			 {
				 softAssert.assertTrue(hotelList!=null,"HBG Hotels not found for SearchChargeConditions Search")

			 }

	softAssert.assertAll()
	where:
	rowNum 	| testDesc 				| romeCode | numberOfCots | numberOfRooms | duration
	2		|"SCCR Day Format for Room code  TB with Same Room Id for 3 Rooms " 		| "TB" | 0 | 1 | 1
	}

	@Unroll
    def "4.Search Charge Condtiions - ||#testDesc"(){
        println("4.Search Charge Condtiions - WithDayFormatResponse")
        Node node_search
        String responseBody_search
        int searchchargeconditions
        Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, staydays:duration ]
        if(hotelList.size() != 0) {
            println "Hotel size : " + hotelList.size()
            for (k in  0 .. hotelList.size()-1) {
                HotelDTO hDTO = hotelList.get(k)
                HotelDTO hDTO1 = hotelList.get(k+1)
                HotelDTO hDTO2 = hotelList.get(k+2)
                List roomid = [hDTO.getCategoryId(), hDTO1.getCategoryId(),hDTO2.getCategoryId()]
                def fileContents_Search = helper.getSCCRWithDurationDayResponse3RateKeys(SHEET_NAME_SEARCH,rowNum,hDTO.getItemCity(),hDTO.getItemCode(),roomid,inputData)
                println "Search Request Xml.. \n${fileContents_Search}"
                node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                logDebug("Search Response Xml..\n${responseBody_search}")
                searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
                if(searchchargeconditions == 0 && node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.size() != 0 ){
                    if(System.getProperty("platform").equals("nova")){
                        softAssert.assertTrue( node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error.ErrorId.text().equals("ERR0094"), "Expected errorId is not Dispalyed  " )
                        softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error.ErrorText.text().equals("If there are more than one Rate Key Ids in the request, they should be identical"), "Expected error TEXT is not Dispalyed")
                        break
                    }
                    else if(System.getProperty("platform").equals("legacy")){
                        softAssert.assertTrue( node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error[0].ErrorId.text().equals("ERR0005"), "Expected errorId is not Dispalyed  " )
                        softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error[0].ErrorText.text().equals("Parameter " + "\"Id\"" + " must be entered."), "Expected error TEXT is not Dispalyed")
                        break
                    }

                }
                else{
                    softAssert.assertTrue(searchchargeconditions>0, "Expected error Not dispalyed in the response")
                    break
                }

            }


        }else
        {
            softAssert.assertTrue(hotelList!=null,"HBG Hotels not found for SearchChargeConditions Search")

        }

        softAssert.assertAll()
        where:
        rowNum 	| testDesc 				| romeCode | numberOfCots | numberOfRooms | duration
        2		|"SCCR Day Format for Room code  TB with Diffrent Room Id for 3 Rooms " 		| "TB" | 0 | 1 | 1
    }
	@Unroll
	def "5. Search Charge Condtions - ||#testDesc "() {
		println("5.Search Charge Conditions - Search Hotel Price Request")
		Node node_search
		String responseBody_search
		cities = numOfCitiesList(6)
		Map inputData = [dateformat: dateFormatType, room: romeCode, cots: numberOfCots, rooms: numberOfRooms, staydays: duration, age: childAge]
		for (k in 0..cities.size() - 1) {
			def fileContents_Search = helper.getSHPRForMultiRoomWithAge(SHEET_NAME_SEARCH, searchRowNum, cities[k], inputData)
			println "Search Request for SCCR Multi Room.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug("Response Xml.for Search ChargeConditions\n${responseBody_search}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)

			if (hList.size() > 0) {
				hotelList2Room.addAll(hList)

			}
		}

		softAssert.assertTrue(hotelList2Room.size() > 0, "HBG Hotels not found for Search Charge Conditions")
		softAssert.assertAll()
		println "\n\n"

		where:
		testDesc                                            | searchRowNum | dateFormatType | romeCode | numberOfCots | numberOfRooms | duration | childAge
		"Search for avilable item without DayFormat - 2TB + 1 Extra" | 1            | "false"        | "TB"     | 0            | 2             | 2        | 6
	}
	@Unroll
	def "6.Search Charge Condtiions - ||#testDesc"(){
		println("6.Search Charge Condtiions - WithDayFormatResponse")
		Node node_search
		String responseBody_search
		int searchchargeconditions
		Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, age:childAge]
		if(hotelList2Room.size() != 0) {
			println "Hotel size : " + hotelList2Room.size()
			for (k in  0 .. hotelList2Room.size()-1) {
				HotelDTO hDTO = hotelList2Room.get(k)
				List roomid = [hDTO.getCategoryId(), hDTO.getCategoryId()]
				def fileContents_Search = helper.getSCCRWithDayResponse2RateKeys(SHEET_NAME_SEARCH,rowNum,hDTO.getItemCity(), hDTO.getItemCode(), roomid,inputData)
				println "Search Request Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
				if(searchchargeconditions> 0){
					println "total count"+ searchchargeconditions
					softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed")
					int chargeconditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
					for(int i in 0 ..chargeconditionCnt-1){

						int conditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size()
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[0].@Type.equals("cancellation"), "cancellation Charge Type Not returned " )
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[1].@Type.equals("amendment"), "Amendment Charge Type Not returned " )

						if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("cancellation")) {
							for (int j in 0..conditionCnt - 1) {
								if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("true")){
									def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDay
									def today = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@ToDay
									softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
									softAssert.assertTrue(today.isNumber(), "Correct to  day format is not displayed when charge is true : ACTUAL = " + today)
								}
								else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("false")){
									def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDay
									softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )

								}
							}
						}else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("amendment")){
							if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size() >0) {
								def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[0].@FromDay
								softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
							}
						}

					}

					break
				}

			}
			softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed even after trying for " + hotelList.size() + " rate keys")

		}else
		{
			softAssert.assertTrue(hotelList2Room!=null,"HBG Hotels not found for SearchChargeConditions Search 2TB with same Age ")

		}

		softAssert.assertAll()
		where:
		rowNum 	| testDesc 				| romeCode | numberOfCots | numberOfRooms | childAge
		2		|"SCCR Day Format 2 TB room With same age and same ratekeys " 		| "TB" | 0 | 1 | 6
	}

	@Unroll
	def "7.Search Charge Condtiions - ||#testDesc"(){
		println("7.Search Charge Condtiions - WithDayFormatResponse")
		Node node_search
		String responseBody_search
		int searchchargeconditions
		Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, age:childAge]
		if(hotelList2Room.size() != 0) {
			println "Hotel size : " + hotelList2Room.size()
			for (k in  0 .. hotelList2Room.size()-1) {
				HotelDTO hDTO = hotelList2Room.get(k)
				List roomid = [hDTO.getCategoryId(), hDTO.getCategoryId()]
				def fileContents_Search = helper.getSCCRWithAge(SHEET_NAME_SEARCH, rowNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(),inputData)

				println "Search Request Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
				if(searchchargeconditions> 0){
					println "total count"+ searchchargeconditions
					softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed")
					int chargeconditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
					for(int i in 0 ..chargeconditionCnt-1){

						int conditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size()
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[0].@Type.equals("cancellation"), "cancellation Charge Type Not returned " )
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[1].@Type.equals("amendment"), "Amendment Charge Type Not returned " )

						if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("cancellation")) {
							for (int j in 0..conditionCnt - 1) {
								if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("true")){
									def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDay
									def today = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@ToDay
									softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
									softAssert.assertTrue(today.isNumber(), "Correct to  day format is not displayed when charge is true : ACTUAL = " + today)
								}
								else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("false")){
									def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDay
									softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )

								}
							}
						}else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("amendment")){
							if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size() >0) {
								def fromday = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[0].@FromDay
								softAssert.assertTrue(fromday.isNumber(), "Correct from day format is not displayed when charge is true : ACTUAL = " + fromday )
							}
						}

					}

					break
				}

			}
			softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed even after trying for " + hotelList.size() + " rate keys")

		}else
		{
			softAssert.assertTrue(hotelList2Room!=null,"HBG Hotels not found for SearchChargeConditions Search 2TB with same Age ")

		}

		softAssert.assertAll()
		where:
		rowNum 	| testDesc 				| romeCode | numberOfCots | numberOfRooms | childAge
		2		|"SCCR Day Format 2 TB room With same age number of rooms 2  " 		| "TB" | 0 | 2 | 6
	}
	@Unroll
	def "8.Search Charge Condtiions - ||#testDesc"(){
		println("8.Search Charge Condtiions - WithDayFormatResponse")
		Node node_search
		String responseBody_search
		int searchchargeconditions
		Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, age:childAge]
		if(hotelList.size() != 0) {
			println "Hotel size : " + hotelList.size()
			for (k in  0 .. hotelList.size()-1) {
				HotelDTO hDTO = hotelList.get(k)
				HotelDTO hDTO1 = hotelList.get(k+1)
				List roomid = [hDTO.getCategoryId(), hDTO1.getCategoryId()]
				def fileContents_Search = helper.getSCCRWithDayResponse2RateKeys(SHEET_NAME_SEARCH,rowNum,hDTO.getItemCity(),hDTO.getItemCode(),roomid,inputData)
				println "Search Request Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
				if(searchchargeconditions == 0 && node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.size() != 0 ){
					if(System.getProperty("platform").equals("nova")){
						softAssert.assertTrue( node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error.ErrorId.text().equals("ERR0094"), "Expected errorId is not Dispalyed  " )
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error.ErrorText.text().equals("If there are more than one Rate Key Ids in the request, they should be identical"), "Expected error TEXT is not Dispalyed")
						break
					}
					else if(System.getProperty("platform").equals("legacy")){
						softAssert.assertTrue( node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error[0].ErrorId.text().equals("ERR0005"), "Expected errorId is not Dispalyed  " )
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error[0].ErrorText.text().equals("Parameter " + "\"Id\"" + " must be entered."), "Expected error TEXT is not Dispalyed")
						break
					}
				}
				else{
					softAssert.assertTrue(searchchargeconditions>0, "Expected error Not dispalyed in the response")
					break
				}
			}

		}else
		{
			softAssert.assertTrue(hotelList!=null,"HBG Hotels not found for SearchChargeConditions Search")

		}

		softAssert.assertAll()
		where:
		rowNum 	| testDesc 				| romeCode | numberOfCots | numberOfRooms | childAge
		2		|"SCCR Day Format 2 TB room With same age Diffrent Rate Keys " 		| "TB" | 0 | 1 | 6
	}

	@Unroll
	def "9. Search Charge Condtions - ||#testDesc "() {
		println("5.Search Charge Conditions - Search Hotel Price Request")
		Node node_search
		String responseBody_search
		cities = numOfCitiesList(6)
		Map inputData = [dateformat: dateFormatType, room: romeCode, cots: numberOfCots, rooms: numberOfRooms, staydays: duration, age: childAge]
		for (k in 0..cities.size() - 1) {
			def fileContents_Search = helper.getSHPRFor2RoomWithAge(SHEET_NAME_SEARCH, searchRowNum, cities[k], inputData)
			println "Search Request for SCCR Multi Room.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
			responseBody_search = XmlUtil.serialize(node_search)

			logDebug("Response Xml.for Search ChargeConditions\n${responseBody_search}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)

			if (hList.size() > 0) {
				hotelList2RoomandCot.addAll(hList)

			}
		}

		softAssert.assertTrue(hotelList2RoomandCot.size() > 0, "HBG Hotels not found for Search Charge Conditions")
		softAssert.assertAll()
		println "\n\n"

		where:
		testDesc                                            | searchRowNum | dateFormatType | romeCode | numberOfCots | numberOfRooms | duration | childAge
		"Search for avilable item without DateFormat - 2TB 1 extra and 1 cot" | 1            | "true"        | "TB"     | 1            | 1             | 2       | 6
	}
	@Unroll
	def "10.Search Charge Condtiions - ||#testDesc"(){
		println("10.Search Charge Condtiions - WithDateFormatResponse")
		Node node_search
		String responseBody_search
		int searchchargeconditions
		Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, age:childAge]
		if(hotelList2RoomandCot.size() != 0) {
			println "Hotel size : " + hotelList2RoomandCot.size()
			for (k in  0 .. hotelList2RoomandCot.size()-1) {
				HotelDTO hDTO = hotelList2RoomandCot.get(k)
				List roomid = [hDTO.getCategoryId(), hDTO.getCategoryId()]
				def fileContents_Search = helper.getSCCRDateFormatWithAge(SHEET_NAME_SEARCH, rowNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(),inputData)

				println "Search Request Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
				if(searchchargeconditions> 0){
					println "total count"+ searchchargeconditions
					softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed")
					int chargeconditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
					for(int i in 0 ..chargeconditionCnt-1){

						int conditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size()
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[0].@Type.equals("cancellation"), "cancellation Charge Type Not returned " )
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[1].@Type.equals("amendment"), "Amendment Charge Type Not returned " )

						if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("cancellation")) {
							for (int j in 0..conditionCnt - 1) {
								if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("true")){
									def todate = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@ToDate
									def fromdate = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDate
									softAssert.assertTrue(helper.isThisDateValid(todate), "Correct To date dateformat is not displayed when charge is true" + todate )
									softAssert.assertTrue(helper.isThisDateValid(fromdate), "Correct From date dateformat is not displayed when charge is true " + fromdate)
								}
								else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("false")){
									def fromdate = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDate
									softAssert.assertTrue(helper.isThisDateValid(fromdate), "Correct From date dateformat is not displayed when charge is true " + fromdate)

								}
							}
						}else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("amendment")){
							if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size() >0) {
								def fromdate = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[0].@FromDate
								softAssert.assertTrue(helper.isThisDateValid(fromdate), "Correct From date dateformat is not displayed when charge is true " + fromdate)
							}
						}

					}

					break
				}

			}
			softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed even after trying for " + hotelList2RoomandCot.size() + " rate keys")

		}else
		{
			softAssert.assertTrue(hotelList2RoomandCot!=null,"HBG Hotels not found for SearchChargeConditions Search 2TB with same Age and addational cot ")

		}

		softAssert.assertAll()
		where:
		rowNum 	| testDesc 				| romeCode | numberOfCots | numberOfRooms | childAge
		2		|"SCCR Date Format 2 TB + 1Extra + 1 Cot 2 rooms " 		| "TB" | 2 | 2 | 6
	}

	@Unroll
	def "11.Search Charge Condtiions - ||#testDesc"(){
		println("11.Search Charge Condtiions - WithDateFormatResponse")
		Node node_search
		String responseBody_search
		int searchchargeconditions
		Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, age:childAge]
		if(hotelList2RoomandCot.size() != 0) {
			println "Hotel size : " + hotelList2RoomandCot.size()
			for (k in  0 .. hotelList2RoomandCot.size()-1) {
				HotelDTO hDTO = hotelList2RoomandCot.get(k)
				List roomid = [hDTO.getCategoryId(), hDTO.getCategoryId()]
				def fileContents_Search = helper.getSCCRWithDateResponse2RateKeys(SHEET_NAME_SEARCH,rowNum,hDTO.getItemCity(), hDTO.getItemCode(), roomid,inputData)
				println "Search Request Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
				if(searchchargeconditions> 0){
					println "total count"+ searchchargeconditions
					softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed")
					int chargeconditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
					for(int i in 0 ..chargeconditionCnt-1){

						int conditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size()
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[0].@Type.equals("cancellation"), "cancellation Charge Type Not returned " )
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[1].@Type.equals("amendment"), "Amendment Charge Type Not returned " )

						if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("cancellation")) {
							for (int j in 0..conditionCnt - 1) {
								if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("true")){
									def todate = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@ToDate
									def fromdate = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDate
									softAssert.assertTrue(helper.isThisDateValid(todate), "Correct To date dateformat is not displayed when charge is true" + todate )
									softAssert.assertTrue(helper.isThisDateValid(fromdate), "Correct From date dateformat is not displayed when charge is true " + fromdate)
								}
								else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@Charge.equals("false")){
									def fromdate = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[j].@FromDate
									softAssert.assertTrue(helper.isThisDateValid(fromdate), "Correct From date dateformat is not displayed when charge is true " + fromdate)
								}
							}
						}else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].@Type.equals("amendment")){
							if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition.size() >0) {
								def fromdate = node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[i].Condition[0].@FromDate
								softAssert.assertTrue(helper.isThisDateValid(fromdate), "Correct From date dateformat is not displayed when charge is true " + fromdate)
							}
						}

					}

					break
				}

			}
			softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed even after trying for " + hotelList.size() + " rate keys")

		}else
		{
			softAssert.assertTrue(hotelList2RoomandCot!=null,"HBG Hotels not found for SearchChargeConditions Search 2TB with same Age ")

		}

		softAssert.assertAll()
		where:
		rowNum 	| testDesc 				| romeCode | numberOfCots | numberOfRooms | childAge
		2		|"SCCR Date Format 2 TB + 1 extra + 1 Cot 2 room with same rate Keys" 		| "TB" | 1 | 1 | 6
	}
	@Unroll
	def "12.Search Charge Condtiions - ||#testDesc"(){
		println("12.Search Charge Condtiions - WithDayFormatResponse")
		Node node_search
		String responseBody_search
		int searchchargeconditions
		Map inputData = [room : romeCode,cots:numberOfCots, rooms:numberOfRooms, age:childAge]
		if(hotelList2RoomandCot.size() != 0) {
			println "Hotel size : " + hotelList2RoomandCot.size()
			for (k in  0 .. hotelList2RoomandCot.size()-1) {
				HotelDTO hDTO = hotelList2RoomandCot.get(k)
				HotelDTO hDTO1 = hotelList2RoomandCot.get(k+1)
				List roomid = [hDTO.getCategoryId(), hDTO1.getCategoryId()]
				def fileContents_Search = helper.getSCCRWithDayResponse2RateKeys(SHEET_NAME_SEARCH,rowNum,hDTO.getItemCity(),hDTO.getItemCode(),roomid,inputData)
				println "Search Request Xml.. \n${fileContents_Search}"
				node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				responseBody_search = XmlUtil.serialize(node_search)
				logDebug("Search Response Xml..\n${responseBody_search}")
				searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
				if(searchchargeconditions == 0 && node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.size() != 0 ){
					if(System.getProperty("platform").equals("nova")){
						softAssert.assertTrue( node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error.ErrorId.text().equals("ERR0094"), "Expected errorId is not Dispalyed  " )
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error.ErrorText.text().equals("If there are more than one Rate Key Ids in the request, they should be identical"), "Expected error TEXT is not Dispalyed")
						break
					}
					else if(System.getProperty("platform").equals("legacy")){
						softAssert.assertTrue( node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error[0].ErrorId.text().equals("ERR0005"), "Expected errorId is not Dispalyed  " )
						softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.Errors.Error[0].ErrorText.text().equals("Parameter " + "\"Id\"" + " must be entered."), "Expected error TEXT is not Dispalyed")
						break
					}
				}
				else{
					softAssert.assertTrue(searchchargeconditions>0, "Expected error Not dispalyed in the response")
					break
				}
			}

		}else
		{
			softAssert.assertTrue(hotelList2RoomandCot!=null,"HBG Hotels not found for SearchChargeConditions Search")

		}

		softAssert.assertAll()
		where:
		rowNum 	| testDesc 				| romeCode | numberOfCots | numberOfRooms | childAge
		2		|"SCCR Date Format 2 TB + 1 extra + 1 Cot 2 room with Diffrent Rate Keys " 		| "TB" | 1 | 1 | 6
	}
}


class SearchChargeConditionsMultiRoomTest_C1237 extends SearchChargeConditionsMultiRoomTest {
	@Override
	String getClientId() {
		return "1237"
	}
}

class SearchChargeConditionsMultiRoomTest_C1238 extends SearchChargeConditionsMultiRoomTest {
	@Override
	String getClientId() {
		return "1238"
	}
}

class SearchChargeConditionsMultiRoomTest_C1239 extends SearchChargeConditionsMultiRoomTest {
	@Override
	String getClientId() {
		return "1239"
	}
}

class SearchChargeConditionsMultiRoomTest_C1244 extends SearchChargeConditionsMultiRoomTest {
	@Override
	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class SearchChargeConditionsMultiRoomTestSingleClient extends SearchChargeConditionsMultiRoomTest {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}



