package com.hbg.test.PriceSearch.SearchHotelPrice

import com.hbg.helper.RequestHelper.*
import com.hbg.test.XSellBaseTest
import com.kuoni.finance.automation.xml.util.*
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.*

abstract class SearchHotelPriceTest_MultiRooms extends XSellBaseTest {

    static final String SHEET_NAME = "xmle2e_HBG_SHPR_Ref"
    static final String EXCEL_SHEET = new File(ConfigProperties.getValue("priceSearchSheet"))
    static final String SITEID = ConfigProperties.getValue("SiteId")
    static final String FITSiteIdHeaderKey = ConfigProperties.getValue("FITSiteIdHeaderKey")
    SearchHotelPriceHelper helper = new SearchHotelPriceHelper(EXCEL_SHEET, getClientId())
    SoftAssert softAssert = new SoftAssert()
    int rowNum
    String sheet, testDesc

    @Shared
    List cities
    @Shared
    List<HotelDTO> hotelList = new ArrayList<HotelDTO>()
    @Shared
    List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()


    @Unroll
    //@IgnoreRest
    /** 3xTB_MultiRooms **/
    def "Verify SHPR_MultiRooms Test 1 || #testDesc "(){
        println("Test Running ...$testDesc")
        Node node_search
        String responseBody_search
        cities = numOfCitiesList(6)
        Map inputData = [room : roomCode,cots:numberOfCots, rooms:numberOfRooms]
        for(k in 0.. cities.size()-1 ){
            def fileContents_Search = helper.getSHPR_MultiRooms_CTY(SHEET_NAME, searchRowNum, cities[k], inputData)
            println "Request...\n${fileContents_Search}"
            node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(XSellBaseTest.url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            println "Response... \n$responseBody_search"
            //logDebug("Response...\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
            def hotelRoom = hotelNode.HotelRooms.HotelRoom

            if (testDesc.equals("3xTB_CTY")){
                println "IN 3xTB_CTY"
                if (hotelNode.size() != 0){
                    for (i in 0..hotelRoom.size()-1){
                        softAssert.assertTrue(hotelRoom[i].@Code == "TB", "HotelRoom Code does not equal TB for hotel in " + cities[k])
                        softAssert.assertTrue(hotelRoom[i].@NumberOfRooms == "3", "HotelRoom NumberOfRooms does not equal 3 for hotel in " + cities[k])
                    }
                }
            } else if (testDesc.equals("2xQ_CTY")){
                println "IN 2xQ_CTY"
                if (hotelNode.size() != 0){
                    for (i in 0..hotelRoom.size()-1){
                        softAssert.assertTrue(hotelRoom[i].@Code == "Q", "HotelRoom Code does not equal Q for hotel in " + cities[k])
                        softAssert.assertTrue(hotelRoom[i].@NumberOfRooms == "2", "HotelRoom NumberOfRooms does not equal 2 for hotel in " + cities[k])
                    }
                }
            }

            List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
            List<HotelDTO> gList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)

            if (hList.size() > 0){
                hotelList.addAll(hList)
            }

            if (gList.size() > 0){
                gtaHotelList.addAll(gList)
            }
        }

        for (HotelDTO dto : hotelList){
            println "HBG >>>" + dto.toString()
        }

        for (HotelDTO dto : gtaHotelList){
            println "GTA >>>" + dto.toString()
        }

        softAssert.assertTrue(hotelList.size() > 0,"HBG Hotels not found")
        softAssert.assertTrue(gtaHotelList.size() > 0, "GTA Hotels not found")

        softAssert.assertAll()
        println "\n\n"

        where:
        searchRowNum | roomCode | numberOfCots | numberOfRooms | testDesc
        1 | "TB" | "0" | "3" | "3xTB_CTY"
        1 | "Q" | "0" | "2" | "2xQ_CTY"
    }
}

    class SearchHotelPriceTest_MultiRooms_C1237 extends SearchHotelPriceTest_MultiRooms {
        @Override
        String getClientId() {
            return "1237"
        }
    }

    class SearchHotelPriceTest_MultiRooms_C1238 extends SearchHotelPriceTest_MultiRooms {
        @Override
        String getClientId() {
            return "1238"
        }
    }

    class SearchHotelPriceTest_MultiRooms_C1239 extends SearchHotelPriceTest_MultiRooms {
        @Override
        String getClientId() {
            return "1239"
        }
    }

    class SearchHotelPriceTest_MultiRooms_C1244 extends SearchHotelPriceTest_MultiRooms {
        @Override
        String getClientId() {
            return "1244"
        }
    }

    @IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
    class SearchHotelPriceTest_MultiRoomsSingleClient extends SearchHotelPriceTest_MultiRooms {

        @Override
        String getClientId() {
            String clientId = System.getProperty("clientId")
            println "Using client Id $clientId"
            return clientId
        }

    }
