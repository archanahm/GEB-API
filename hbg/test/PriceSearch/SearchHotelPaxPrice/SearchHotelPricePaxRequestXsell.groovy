package com.hbg.test.PriceSearch.SearchHotelPaxPrice

import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPriceHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.hbg.test.XSellBaseTest
import com.kuoni.finance.automation.xml.util.ConfigProperties
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Unroll

import static org.junit.Assert.assertTrue

abstract class SearchHotelPricePaxRequestXsell extends XSellBaseTest {
    static final String SHEET_NAME_SEARCH = "SHPPR_Xsell"
    static final String EXCEL_SHEET = new File(ConfigProperties.getValue("priceSearchSheet"))
    static final String URL = ConfigProperties.getValue("AuthenticationURL")
    static final String SITEID = ConfigProperties.getValue("SiteId")
    static final String FITSiteIdHeaderKey = ConfigProperties.getValue("FITSiteIdHeaderKey")
    SearchHotelPricePaxHelper helper = new SearchHotelPricePaxHelper(EXCEL_SHEET, getClientId())
    SearchHotelPriceHelper helper1 = new SearchHotelPriceHelper(EXCEL_SHEET, getClientId())
    int searchRowNum



    String PaxId
    private static def xmlParser = new XmlParser()
    SoftAssert softAssert = new SoftAssert()
    Node node_search
    String responseBody_search
    static def platform = System.getProperty("platform")

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
    int hotelCount = 0
    String DateFormat
    String mealBasisCode
    String mealBasisTxt
    def flag = false
    Map itemMapGTA = null
    Map itemMapHBG = null
    Map fields = null
    String childage
    String numberOfCots

    @Shared
    List<HotelDTO> hotelList = new ArrayList<HotelDTO>()


    @Shared
    List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()

    @Shared
    HotelDTO successHotelDTO = null

    @Shared
    List cities

    @Unroll
    /** Search Hotel Price Pax Request - City Search with Duration*/
    def "1.SearchHotelPricePaxRequest - City Searchwith Duration"() {
        println "SearchHotelPricePax Request - City Search with Duration"
        softAssert = new SoftAssert()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()
        List HbgNotfoundCities

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxCitySearchXmlwithDuration(SHEET_NAME_SEARCH, searchRowNum, cities[k])
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
            List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
            if (hList != null && gtaHList != null) {
                if (hList.size() > 0 && gtaHList.size() >0) {
                    hotelList.addAll(hList)
                    gtaHotelList.addAll(gtaHList)
                    def roomcategoryNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory
                    for (i in 0..roomcategoryNode.size()-1){
                        if(roomcategoryNode[i].Confirmation.@Code[0].equals("IM")){
                            softAssert.assertTrue(roomcategoryNode[i].Confirmation.text().equals("AVAILABLE"),"Confirmation code AVAILABLE is not dispalyed for code IM Confirmationcode : " +roomcategoryNode[i].Confirmation.@Code[0] + "ConfirmationText " + roomcategoryNode[i].Confirmation.text() )
                        }
                        if(roomcategoryNode[i].Confirmation.@Code[0].equals("OR")){
                            softAssert.assertTrue(roomcategoryNode[i].Confirmation.text().equals("On Request"),"Confirmation code On Request is not dispalyed for code OR Confirmationcode : " +roomcategoryNode[i].Confirmation.@Code[0] + "ConfirmationText " + roomcategoryNode[i].Confirmation.text() )
                        }

                    }
                    break

                }
            }

        }

        softAssert.assertTrue(hotelList.size() > 0, "HBG Hotels not found for City " + cities)
        softAssert.assertTrue(gtaHotelList.size() > 0, "GTA Hotels not found for City" + cities)
        softAssert.assertAll()
        println "\n\n"



        where:
        rowNum | testDesc       | searchRowNum
        1      | "City Search " | 1

    }

    /** Search Hotel Price Pax Request - City Search with CheckOut Date*/
    def "2.SearchHotelPricePaxRequest - City Search with CheckoutDate"() {
        println "SearchHotelPricePax Request - City Search with Checkout Date"
        cities = numOfCitiesList(6)
        println "city size" + cities.size()
        List HbgNotfoundCities

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxCitySearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k])
            println "SHPPR City Search With CheckOut Date Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")

            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel

            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
            if (hList != null) {
                if (hList.size() > 0) {
                    hotelList.addAll(hList)
                }
            }
            else{
                HbgNotfoundCities.add(cities[k])
            }
            List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
            if (gtaHList != null) {
                if (gtaHList.size() > 0) {
                    gtaHotelList.addAll(gtaHList)
                }
            }
        }
        softAssert.assertTrue(hotelList.size() > 0, "HBG Hotels not found for City "+cities )
        softAssert.assertTrue(gtaHotelList.size() > 0, "GTA Hotels not found for City "+cities)
        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc       | searchRowNum
        1      | "City Search " | 1

    }

    @Unroll
    /*SearchHotelPricePaxRequest - CityCode and ItemCode Search*/
    def "3.SearchHotelPricePaxRequest - City +Item Code Search"() {
        println "SearchHotelPricePax Request - City +Item Code Search"
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxCityItemCodeSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ")
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }

        }
        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City"+ cities)

        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size()-1; i++) {
                hDTO = hotelListlocal.get(i)
                //HotelDTO hDTO = hotelList.get(i)
                def fileContents_Search = helper.getPaxCityItemCodeSearchXml(SHEET_NAME_SEARCH, searchRowNum,  hDTO.getItemCity(), hDTO.getItemCode())
                println "SHPPR CityCode and ItemCode Search Request.. \n ${fileContents_Search}"
                //logDebug("Search Request Xml..$k \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                logDebug("Search Response Xml..\n${responseBody_search}")

                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
                List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)

                if (hList != null || gtaHList != null) {
                    if (hList.size() > 0 && gtaHList.size() > 0) {
                        hotelList1.addAll(hList)
                        gtaHotelList.addAll(gtaHList)
                        break
                    }


                }

                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_search.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }
            }

            softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found for SHPPR City Code and Item Code Search")
            softAssert.assertTrue(gtaHotelList.size() > 0, "GTA Hotels not found  for SHPPR City Code and Item Code Search")
        } else {
            softAssert.assertTrue(hotelListlocal.size() != null, "HBG Hotels not found for SHPPR City Code and Item Code Search")
            println "\n\n"
        }
            softAssert.assertAll()

        where:
        rowNum | testDesc                    | searchRowNum
        2      | "City Search and Item Code" | 2


    }

    @Unroll
    //@IgnoreRest
    /*SearchHotelPricePaxRequest - Child and Cot Search*/
    def "4.SearchHotelPricePaxRequest - || #testDesc "() {
        println "SearchHotelPricePax Request -1Child and 1Cot Search"
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxChildandCotCodeSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ",numberOfCots,childage)
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)

                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }

        }
        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City"+ cities)

        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size()-1; i++) {
                hDTO = hotelListlocal.get(i)
                def fileContents_Search = helper.getPaxChildandCotCodeSearchXml(SHEET_NAME_SEARCH, searchRowNum,  hDTO.getItemCity(), hDTO.getItemCode(),numberOfCots,childage)
                println "SHPPR Child and Cot Search Request: \n${fileContents_Search}"
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)

                logDebug("Search Response Xml..\n${responseBody_search}")

                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
                List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
                if (hList != null || gtaHList != null) {
                    if (hList.size() > 0  ) {
                        hotelList1.addAll(hList)
                        gtaHotelList.addAll(gtaHList)
                        def roomcategoryNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory
                        for (j in 0..roomcategoryNode.size()-1){

                            softAssert.assertTrue(roomcategoryNode[j].SharingBedding != null,"SharingBed tag not dispalyed for room  " )
                            if(roomcategoryNode[i].HotelRoomPrices.HotelRoom[1].@ChildAge.equals(childage)  ){
                                softAssert.assertTrue(roomcategoryNode[i].HotelRoomPrices.HotelRoom[1].@Code.equals("CH"),"Hotel Room code for Room with Child is not correct HotelRoomCOde: " +roomcategoryNode[i].HotelRoomPrices.HotelRoom[1].@Code )

                            }
                            else{
                                softAssert.assertTrue(roomcategoryNode[i].HotelRoomPrices.HotelRoom[1].@ChildAge.equals(childage) ,"Childage in response is not same as in request: " + roomcategoryNode[i].HotelRoomPrices.HotelRoom[1].@ChildAge + "ChildAge in Request " + childage )

                            }
                            if( roomcategoryNode[i].HotelRoomPrices.HotelRoom[2].@NumberOfCots.equals(numberOfCots)){
                                softAssert.assertTrue(roomcategoryNode[i].HotelRoomPrices.HotelRoom[2].@Code.equals("CT"),"Hotel Room code for Room with Numberof cots is not correct HotelRoomCOde: " +roomcategoryNode[i].HotelRoomPrices.HotelRoom[2].@Code)
                            }
                            else{
                                softAssert.assertTrue(roomcategoryNode[i].HotelRoomPrices.HotelRoom[2].@NumberOfCots.equals(numberOfCots),"Numberofcots  in response is not same as in request: " + roomcategoryNode[i].HotelRoomPrices.HotelRoom[2].@NumberOfCots + "Numberofcots in Request " + numberOfCots )
                            }

                        }
                        break

                    }
                }

                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStrcc
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_search.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }
            }

            softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found for SHPPR - ONE CHILD AND ONE COT")

            println "\n\n"
        } else {
            softAssert.assertTrue(hotelListlocal.size() != null, "HBG Hotels not found for SHPPR City Code and Item Code Search")
            println "\n\n"
        }

            softAssert.assertAll()

        where:
        rowNum | testDesc                             | searchRowNum | numberOfCots | childage
        3      | "City Search with 1 Child and 1 cot" | 3           | 1  | 6
        3      | "City Search with 1 Child and 2 cot" | 3           | 1  | 8



    }

    @Unroll
    /*SearchHotelPricePaxRequest - With Condition DateFormat Response as True with Date in the Response*/
    def "5.SearchHotelPricePaxRequest - With Condition DateFormat Response as True with Date in the response"() {
        println "SearchHotelPricePax Request - With Condition DateFormat Response as True with Date in the response"
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxConditionsSearchXmlwithDateFormat(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ",DateFormat)
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }

        }
        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City"+ cities)

        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size()-1; i++) {
                hDTO = hotelListlocal.get(i)
                def fileContents_Search = helper.getPaxConditionsSearchXmlwithDateFormat(SHEET_NAME_SEARCH, searchRowNum, hDTO.getItemCity(), hDTO.getItemCode(),DateFormat)
                println "SHPPR with Condition Request: \n${fileContents_Search}"
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                logDebug("Search Response Xml..\n${responseBody_search}")
                //itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)

                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

                    if (hList != null) {
                        if (hList.size() > 0) {
                            hotelList1.addAll(hList)
                            //To verify whether the response has Date or not
                            boolean isChargeConditionAsDate = helper1.getPaxChargeConditionDetailsHBG(node_search, hDTO.getItemCode(), true, "date")
                            softAssert.assertTrue(isChargeConditionAsDate, "Charge Conditions not returned as Date Format")
                            break

                        }
                    }
                }

                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }
            }

            softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found for SHPPR DateFormat Response")
        } else {
            softAssert.assertTrue(hotelListlocal.size() != null, "HBG Hotels not found for SHPPR DateFormat Response")
        }
            println "\n\n"
            softAssert.assertAll()


        where:
        rowNum | testDesc                            | searchRowNum | DateFormat
        4      | "Charge Conditions with Flag true " | 4            | "true"


    }

    @Unroll
    /*SearchHotelPricePaxRequest - With Condition DateFormat Response as True with Day in the Response*/
    def "6.SearchHotelPricePaxRequest - With Condition DateFormat Response as True with Day in the response"() {
        println "SearchHotelPricePax Request - With Condition DateFormat Response as True with Day in the response"
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxConditionsSearchXmlwithDayFormat(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ")
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }

        }
        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City"+ cities)

        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size()-1; i++) {
                hDTO = hotelListlocal.get(i)

                def fileContents_Search = helper.getPaxConditionsSearchXmlwithDayFormat(SHEET_NAME_SEARCH, searchRowNum,hDTO.getItemCity(), hDTO.getItemCode())
                println "SHPPR With Condition Request.. \n${fileContents_Search}"

                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                logDebug("Search Response Xml..\n${responseBody_search}")


                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                def chargeconadtionNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory.ChargeConditions.size()
                if (chargeconadtionNode > 0) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

                    if (hList != null) {
                        if (hList.size() > 0) {
                            hotelList1.addAll(hList)
                            //To verify whether the response has day or not
                            boolean isChargeConditionAsDay = helper1.getPaxChargeConditionDetailsHBG(node_search, hDTO.getItemCode(), true, "day")
                            softAssert.assertTrue(isChargeConditionAsDay, "Charge Condition not returned as day Format")
                            break

                        }
                    }
                }
                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }
            }

            softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found for SHPPR Date Format Response)")
        } else {
            softAssert.assertTrue(hotelListlocal.size() != null, "HBG Hotels not found for SHPPR Date Format Response")
        }
            softAssert.assertAll()
            println "\n\n"

        where:
        rowNum | testDesc                            | searchRowNum
        4      | "Charge Conditions with Flag true " | 4


    }

    /*SearchHotelPricePaxRequest - With Condition DateFormat Response as false with Day in the response*/

    def "7.SearchHotelPricePaxRequest - With Condition DateFormat Response as false with Day in the response"() {
        println "SearchHotelPricePax Request - With DateFormat Response as false with Day in the response"
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxConditionsSearchXmlwithDateFormat(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ",DateFormat)
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }

        }
        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City"+ cities)

        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size()-1; i++) {
                hDTO = hotelListlocal.get(i)

                //HotelDTO hDTO = hotelList.get(i)
                def fileContents_Search = helper.getPaxConditionsSearchXmlwithDateFormat(SHEET_NAME_SEARCH, searchRowNum, hDTO.getItemCity(), hDTO.getItemCode(),DateFormat)
                println "SHPPR DateFormat with Date.. \n${fileContents_Search}"
                //logDebug("Search Request Xml..$k \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                //println "Search Response Xml..\n${responseBody_search}"
                logDebug("Search Response Xml..\n${responseBody_search}")
                //itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)
                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                def chargeCondationNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory.ChargeConditions.ChargeCondition

                if (chargeCondationNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

                    if (hList != null) {
                        if (hList.size() > 0) {
                            hotelList1.addAll(hList)
                            //To verify whether the response has date or not
                            boolean isChargeConditionAsDate = helper1.getPaxChargeConditionDetailsHBG(node_search, hDTO.getItemCode(), true, "day")
                            softAssert.assertTrue(isChargeConditionAsDate, "Charge Condition not returned as Date Format")
                            break
                        }
                    }

                }
                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }
            }
            softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found for SHPPR Dateformat Response")
            //softAssert.assertTrue(gtaHotelList.size() > 0,"GTA Hotels not found for SHPPR Dateformat Response")

        } else {
            softAssert.assertTrue(hotelListlocal.size() != null, "HBG Hotels not found for SHPPR Dateformat Response")
        }
            println "\n\n"
            softAssert.assertAll()
        where:
        rowNum | testDesc                            | searchRowNum | DateFormat
        5      | "Charge Conditions with Flag true " | 5            | "false"
    }


    /*@Unroll
  //  SearchHotelPricePaxRequest - With Rate_Keys"
    def "8.SearchHotelPricePaxRequest - With Rate_Keys"() {
        println "SearchHotelPricePax Request - With RateKeys"
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        assertTrue("No HBG hotels found or Search Charge Conditions", hotelList.size() != 0)
        HotelDTO hDTO

        def itemCity = []
        def itemCode = []
        def itemRateKey = []
        int m
        if (hotelList != null) {
            int l = hotelList.size() - 1
            for (k in 0..l) {
                hDTO = hotelList.get(k)
                itemCity << hDTO.getItemCity()
                itemCode << hDTO.getItemCode()
                itemRateKey << hDTO.getCategoryId()
            }
            itemDetails.put('itemCity', itemCity)
            itemDetails.put('itemCode', itemCode)
            itemDetails.put('itemRateKey', itemRateKey)
            if (hotelList.size() > 4) {
                m = 5
            } else {
                m = hotelList.size()
            }
            for (int i = 0; i < m; i++) {
                //HotelDTO hDTO = hotelList.get(i)
                def fileContents_Search = helper.getPaxWithRateKey(SHEET_NAME_SEARCH, searchRowNum, itemDetails.itemCity[i], itemDetails.itemCode[i], itemDetails.itemRateKey[i])
                println "SHPPR With RateKeys Request.. \n${fileContents_Search}"
                //logDebug("Search REquest Xml.. \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                //println "Search Response Xml..\n${responseBody_search}"
                logDebug("Search Response Xml..\n${responseBody_search}")


                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
                    if (hList != null) {
                        if (hList.size() > 0) {
                            hotelList1.addAll(hList)
                        }
                    }

                    List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
                    if (gtaHList != null) {
                        gtaHotelList.addAll(gtaHList)
                    }
                }
                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }

                softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found for SHPPR With RateKeys")
                softAssert.assertTrue(gtaHotelList.size() > 0, "GTA Hotels not found for SHPPR With RateKeys")
            }


        } else {
            softAssert.assertTrue(hotelList.size() != null, "HBG Hotels not found for SHPPR City Code and Item Code Search")
        }
            softAssert.assertAll()
            println "\n\n"
        where:
        rowNum | testDesc                 | searchRowNum
        6      | "With Rate_KeyGC_41456 " | 6


    }*/

    @Unroll
  //  @IgnoreRest
    @IgnoreIf({System.getProperty("testEnv") != "sit"})
   // SearchHotelPricePaxRequest - EssentialInformation
    def "9.SearchHotelPricePaxRequest - EssentialInformation"() {
        println "SearchHotelPricePax Request - Essential Information"
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        boolean isEssentialInformationValid = false

            def fileContents_Search = helper.getPaxEssentialInformation(SHEET_NAME_SEARCH, searchRowNum,"par", "eve")

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
             String responseBody = XmlUtil.serialize(node_search)
                logDebug("Search Response Xml..\n${responseBody}")
                def hotelNode1
                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
                    if (hList != null) {
                        if (hList.size() > 0) {
                            hotelList1.addAll(hList)
                            hotelNode1 = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.RoomCategories.RoomCategory.EssentialInformation.Information
                            if (hotelNode1 != null) {

                                isEssentialInformationValid = helper.getPaxEssentialInformationDetailsHBG(node_search)
                                softAssert.assertTrue(isEssentialInformationValid, "Invalid Essential information returned ")

                            }
                        }
                    }
                }
                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_search.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }


        else {
            softAssert.assertTrue(hotelList != null, "HBG Hotels not found for SHPPR Essential Information")
        }
            softAssert.assertAll()
            println "\n\n"
        where:
        rowNum | testDesc                 | searchRowNum
        7      | "Essential Information " | 7


    }

    @Unroll

    /*SearchHotelPricePaxRequest - Include Price BreakDown */
    def "10.SearchHotelPricePaxRequest - InlcudePriceBreakDown"() {
        println "SearchHotelPricePax Request - IncludePriceBreakDown"
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxIncludePriceBreakDown(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ")
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }

        }
        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City"+ cities)

        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size()-1; i++) {
                 hDTO = hotelListlocal.get(i)
                def fileContents_Search = helper.getPaxIncludePriceBreakDown(SHEET_NAME_SEARCH, searchRowNum, hDTO.getItemCity(), hDTO.getItemCode())
                println "SHPPR Include PriceBreakdown Request.. \n${fileContents_Search}"
                //logDebug("Search Request Xml..$k \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                logDebug("Search Response Xml..\n${responseBody_search}")

                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel

                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
                    if (hList != null) {
                    def hotelpricenodesize=node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory.HotelRoomPrices.size()
                    def roomCategorySize = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory.size()
                           if(hotelpricenodesize == roomCategorySize){

                               for(j in 0.. roomCategorySize-1){

                                   def hotelPriceNode =hotelNode[0].PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory[j].HotelRoomPrices.HotelRoom
                                   softAssert.assertFalse(hotelPriceNode.PriceRanges.PriceRange.@Price.contains("Available") , "NV-42461 Available attribute and value displayed in the  Price node " )
                                   softAssert.assertFalse(hotelPriceNode.@RoomPrice.contains("Available") , "NV-42461 Available attribute and value displayed in the  RoomPrice node" )
                                   softAssert.assertFalse(hotelPriceNode.RoomPrice.@Gross.equals("0.00"), "0.00 returned in room price gross value" )
                                   softAssert.assertFalse(hotelPriceNode.PriceRanges.PriceRange.Price.@Gross.equals("0.00"), "0.00 returned in  price gross value" )
                                   softAssert.assertFalse(hotelPriceNode.PriceRanges.PriceRange.@Price.contains("Gross"), "Gross attribute is not displayed in price node")
                               }

                           }

                        break
                    }
                }
                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }

            }



        } else {
            softAssert.assertTrue(hotelListlocal != null, "HBG Hotels not found for SHPPR City Code and Item Code Search")
        }
        softAssert.assertAll()
        println "\n\n"


        where:
        rowNum | testDesc                 | searchRowNum
        8      | "Inclde PriceBreakDown " | 8


    }

    @Unroll

    /*SearchHotelPricePaxRequest - Include Price BreakDown with cots*/
    def "11.SearchHotelPricePaxRequest - InlcudePriceBreakDownWithCots"() {
        println "SearchHotelPricePax Request - IncludePriceBreakDownWithCots"

        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(10)
        cities.add("ROM")
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxIncludePriceBreakDownwithCots(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ")
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }

        }
        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City"+ cities)

        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size(); i++) {
                hDTO = hotelListlocal.get(i)
                def fileContents_Search = helper.getPaxIncludePriceBreakDownwithCots(SHEET_NAME_SEARCH, searchRowNum, hDTO.getItemCity(), hDTO.getItemCode())
                println "SHPPR Include PriceBreakdown with Cots Request.. \n${fileContents_Search}"
                //logDebug("Search Request Xml..$k \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                //println "Search Response Xml..\n${responseBody_search}"
                logDebug("Search Response Xml..\n${responseBody_search}")


                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel

                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
                    if (hList != null) {
                        def hotelpricenodesize=node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory.HotelRoomPrices.size()
                        def roomCategorySize = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory.size()
                        if(hotelpricenodesize == roomCategorySize){

                            for(j in 0.. roomCategorySize-1){

                                def hotelPriceNode =hotelNode[0].PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory[j].HotelRoomPrices.HotelRoom
                                softAssert.assertFalse(hotelPriceNode.PriceRanges.PriceRange.@Price.contains("Available") , "NV-42461 Available attribute and value displayed in the  Price node " )
                                softAssert.assertFalse(hotelPriceNode.@RoomPrice.contains("Available") , "NV-42461 Available attribute and value displayed in the  RoomPrice node" )
                                softAssert.assertFalse(hotelPriceNode.RoomPrice.@Gross.equals("0.00"), "0.00 returned in room price gross value" )
                                softAssert.assertFalse(hotelPriceNode.PriceRanges.PriceRange.Price.@Gross.equals("0.00"), "0.00 returned in  price gross value" )
                                softAssert.assertFalse(hotelPriceNode.PriceRanges.PriceRange.@Price.contains("Gross"), "Gross attribute is not displayed in price node")
                            }

                        }

                        break
                    }
                }
                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }

            }



        } else {
            softAssert.assertTrue(hotelListlocal != null, "HBG Hotels not found for SHPPR City Code and Item Code Search")
        }
        softAssert.assertAll()
        println "\n\n"
        where:
        rowNum | testDesc                            | searchRowNum
        9      | "Include PriceBreakDown with Cots " | 9

    }

    @Unroll

    /*SearchHotelPricePaxRequest - with ID empty*/
    def "12.SearchHotelPricePaxRequest - with Id empty"() {
        println "SearchHotelPricePax Request - With ID empty"

        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxWithIdEmpty(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ")
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)

                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }
            List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
            if (gtaHList != null) {
                gtaHotelList.addAll(gtaHList)
            }
        }

        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City" + cities)
        softAssert.assertTrue(gtaHotelList.size() > 0, "GTA Hotels not found for City" + cities)
        softAssert.assertAll()
        println "\n\n"
        where:
        rowNum | testDesc                            | searchRowNum
        10      | "With ID empty " | 10

    }

    @Unroll
   // @IgnoreRest

    /*SearchHotelPricePaxRequest - Package*/
    def "13.SearchHotelPricePaxRequest - Package"() {
        println "SearchHotelPricePax Request - Package"
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxIncludePackage(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ")
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }
            List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
            if (gtaHList != null) {
                gtaHotelList.addAll(gtaHList)
            }
        }

        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City" + cities)

        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size(); i++) {
                hDTO = hotelListlocal.get(i)
                //HotelDTO hDTO = hotelList.get(i)
                def fileContents_Search = helper.getPaxIncludePackage(SHEET_NAME_SEARCH, searchRowNum, hDTO.getItemCity(), hDTO.getItemCode())
                println "SHPPR Packahe Request.. \n${fileContents_Search}"
                //logDebug("Search Request Xml..$k \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                //println "Search Response Xml..\n${responseBody_search}"
                logDebug("Search Response Xml..\n${responseBody_search}")
                //itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)

                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

                    if (hList != null) {

                            def itemPriceNode=node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory.ItemPrice

                        if(itemPriceNode) {
                            for(int j in 0 .. itemPriceNode.size()-1){
                                String packageRateValue = itemPriceNode[j].@PackageRate
                                softAssert.assertTrue(packageRateValue.equals("true") || packageRateValue.equals("false"), "packageRateValue True or false is not displayed in response ")
                            }


                        }
                        break
                    }
                    }

                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr

                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_search.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }

            }


        } else {
            softAssert.assertTrue(hotelListlocal != null, "HBG Hotels not found for SHPPR Package Search" + cities)
        }
            softAssert.assertAll()
            println "\n\n"

        where:
        rowNum | testDesc                            | searchRowNum
       12     | "Include PriceBreakDown with Cots " | 12

    }

    @Unroll

    /*SearchHotelPricePaxRequest - MultiRoom*/
    def "14.SearchHotelPricePaxRequest - MultiRoom"() {
        println "SearchHotelPricePax Request - MultiRoom"
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxMultiRooom(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ")
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }
            List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
            if (gtaHList != null) {
                gtaHotelList.addAll(gtaHList)
            }
        }

        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City" + cities)
        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size(); i++) {
                hDTO = hotelListlocal.get(i)
                //HotelDTO hDTO = hotelList.get(i)
                def fileContents_Search = helper.getPaxMultiRooom(SHEET_NAME_SEARCH, searchRowNum, hDTO.getItemCity(), hDTO.getItemCode())
                println "SHPPR MultiRoom Request.. \n${fileContents_Search}"
                //logDebug("Search Request Xml..$k \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                //println "Search Response Xml..\n${responseBody_search}"
                logDebug("Search Response Xml..\n${responseBody_search}")
                //itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)

                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

                    if (hList != null) {
                        if (hList.size() > 0) {
                            hotelList1.addAll(hList)

                        }
                        break
                    }
                }
                if (node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0) {
                    def errorStr = node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorId.text()
                    println "ERROR IN RESPONSE " + errorStr
                    errorStr = errorStr + ":" + node_search.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.ErrorText.text()
                    println "ERROR IN RESPONSE " + errorStr
                    softAssert.assertFalse(node.ResponseDetails.SearchHotelPricePaxResponse.Errors.Error.size() > 0, "Error Returned in Booking Response" + errorStr)
                } else if (node_search.ResponseDetails.Errors.Error.size() > 0) {
                    def Errostring = node_search.ResponseDetails.Errors.Error.ErrorId.text() + ":" + node_search.ResponseDetails.Errors.Error.ErrorText.text()
                    print "ERROR IN RESPONSE " + Errostring
                    softAssert.assertFalse(node_search.ResponseDetails.Errors.Error.size() > 0, "Error Returned in Booking Response " + Errostring)
                }

            }

            softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found for SHPPR MultiRoom")
        } else {
            softAssert.assertTrue(hotelList != null, "HBG Hotels not found for SHPPR City Code and Item Code Search"+ cities)
        }
            softAssert.assertAll()
            println "\n\n"
        where:
        rowNum | testDesc                            | searchRowNum
        13      | "SHHPR MultiRooms " | 13

    }

    @Unroll
    @IgnoreIf({System.getProperty("platform") != "nova"})
 
   //@IgnoreIf({(Boolean.valueOf(platform.equalsIgnoreCase("legacy")))})
    /*SearchHotelPricePaxRequest - CheapestRatesOnly*/
    def "15.SearchHotelPricePaxRequest - CheapestRatesOnly"() {
        println "SearchHotelPricePax Request - CheapestRatesOnly"
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
        List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxCheapestRateOnly(SHEET_NAME_SEARCH, searchRowNum, cities[k], Itemcodval =" ")
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }
            List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
            if (gtaHList != null) {
                gtaHotelList.addAll(gtaHList)
            }
        }

        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City" + cities)
        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size(); i++) {
                hDTO = hotelListlocal.get(i)
                //HotelDTO hDTO = hotelList.get(i)
                def fileContents_Search = helper.getPaxCheapestRateOnly(SHEET_NAME_SEARCH, searchRowNum, hDTO.getItemCity(), hDTO.getItemCode())
                println "SHPPR Cheapest Rates request .. \n${fileContents_Search}"
                //logDebug("Search Request Xml..$k \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                //println "Search Response Xml..\n${responseBody_search}"
                logDebug("Search Response Xml..\n${responseBody_search}")
                //itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search,fields)
                itemMapHBG = RequestNewHelper.getSearchPaxDetails_HBG_SHPPR(node_search, itemMapHBG, fields)

                //softAssert.assertFalse(itemMapGTA==null,"No GTA Hotels Found")

                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
                    List<HotelDTO> gtaList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
                    if (hList != null  ) {
                        if (hList.size() > 0 ) {
                            hotelList1.addAll(hList)
                            softAssert.assertTrue(hotelList1.size()== 1, "HBG Hotels not listed as CheapeastHotel in response")
                            break
                        }
                    }
                }

            }

            softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found SHPPR Cheapest Rooms")
        } else {
            softAssert.assertTrue(hotelListlocal != null, "BG Hotels not found SHPPR Cheapest Rooms")
        }
            softAssert.assertAll()
            println "\n\n"

        where:
        rowNum | testDesc                            | searchRowNum
        14     | "Include PriceBreakDown with Cots " | 14

    }

    @Unroll
//@IgnoreRest
    /*SearchHotelPricePaxRequest - MealBasis Request*/
    def "16.SearchHotelPricePaxRequest - || #testDesc "() {
        println "SearchHotelPricePax Request - MealBasis Request " + testDesc
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        HotelDTO hDTO
        def Itemcodval
                List HbgNotfoundCities = new ArrayList()
        List<HotelDTO> hotelListlocal = new ArrayList<HotelDTO>()
        cities = numOfCitiesList(8)
        println "city size" + cities.size()

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxMealBasisRequest(SHEET_NAME_SEARCH, rowNum, cities[k], Itemcodval =" ", mealBasisCode)
            println "Cities name" + cities[k]

            println "SHPPR City Search with Duration Request..$k \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)

            logDebug("Search Response Xml..$k\n${responseBody_search}")
            def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

            if (hList != null) {
                if (hList.size() > 0) {
                    hotelListlocal.addAll(hList)
                }
            }else{
                HbgNotfoundCities.addAll(cities[k])
            }
            }

        softAssert.assertTrue(hotelListlocal.size() > 0, "HBG Hotels not found for City" + cities)
        if (hotelListlocal.size() != 0 ) {
            for (int i = 0; i < hotelListlocal.size(); i++) {
                hDTO = hotelListlocal.get(i)
                //HotelDTO hDTO = hotelList.get(i)
                def fileContents_Search = helper.getPaxMealBasisRequest(SHEET_NAME_SEARCH, rowNum, hDTO.getItemCity(), hDTO.getItemCode(),mealBasisCode)
                println "SHPPR MealBasis Request .. \n${fileContents_Search}"
                //logDebug("Search Request Xml..$k \n${fileContents_Search}")
                node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
                responseBody_search = XmlUtil.serialize(node_search)
                //	println "Search Response Xml..\n${responseBody_search}"
                logDebug("Search Response Xml..\n${responseBody_search}")

                def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel

                if (hotelNode != null) {
                    List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

                    if (hList != null) {

                        def MealsbasisNode=node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel.PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory.Meals

                        if(MealsbasisNode) {
                            for(int j in 0 .. MealsbasisNode.size()-1){
                                String mealCodeInRes = MealsbasisNode[j].Basis.@Code[0]
                                String mealCodeTxtInRes = MealsbasisNode[j].Basis.text()
                                softAssert.assertTrue(mealCodeInRes.equals(mealBasisCode), "mealbasis is not same as in request expected : " + mealBasisCode + " actual : " + mealCodeInRes )
                                softAssert.assertTrue(mealCodeTxtInRes.equals(mealBasisTxt), "mealbasis is not same as in request expected : " + mealBasisTxt + " actual : " + mealCodeTxtInRes )
                            }

                        }
                        break
                    }
                }

            }


        } else {
            softAssert.assertTrue(hotelList != null, "HBG Hotels not found for SHPPR MealBasis Code Search")
        }
            softAssert.assertAll()
            println "\n\n"

        where:
        rowNum | testDesc                              | mealBasisCode  | mealBasisTxt
        15      | "Meal Basis with Code N "             | "N"           | "None"
        15      | "Meal Basis With Code B "             | "B"           |  "Breakfast"

    }

    @Unroll
    /*SearchHotelPricePaxRequest - GeoCode Request*/
    def "17.SearchHotelPricePaxRequest - GeoCode Request"() {
        println "SearchHotelPricePax Request - GeoCode Request"
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        def fileContents_Search = helper.getPaxGeoCodeRequest(SHEET_NAME_SEARCH, searchRowNum)
        //Longitude
        //Latitude
        //Radius are hard coded in the XML as of now
        println "SHPPR GeoCode Request.. \n${fileContents_Search}"
        //logDebug("Search Request Xml..$k \n${fileContents_Search}")
        node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
        responseBody_search = XmlUtil.serialize(node_search)
        logDebug("Search Response Xml..\n${responseBody_search}")

        def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
        if (hotelNode != null) {
            List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
            if (hList.size() > 0) {
                hotelList1.addAll(hList)
            }

            List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)

            if (gtaHList.size() > 0) {
                gtaHotelList.addAll(gtaHList)
            }

            println "total hotels count for all the cities : " + hotelList.size()
            softAssert.assertTrue(hotelList1.size() > 0, "HBG Hotels not found SHPPR GeoCode Request")
            softAssert.assertTrue(gtaHotelList.size() > 0, "GTA Hotels not found SHPPR GeoCode Request")
        } else {
            softAssert.assertTrue(hotelNode != null, "No Data exists for GeoCode Request")
        }
            softAssert.assertAll()
            println "\n\n"
        where:
        rowNum | testDesc                            | searchRowNum
        9      | "SearchHotelPricePaxRequest -GeoCode " | 9

    }

    @Unroll

    /*SearchHotelPricePaxRequest - onSale Request*/
    def "18.SearchHotelPricePaxRequest - OnSale Request"() {
        println "SearchHotelPricePax Request - onSale Request"
        softAssert = new SoftAssert()
        List<HotelDTO> hotelList1 = new ArrayList<HotelDTO>()
        //HotelDTO hDTO = hotelList.get(i)
        def fileContents_Search = helper.getPaxonSaleRequest(SHEET_NAME_SEARCH, searchRowNum, "RIO", "MER1")
        //Longitude
        //Latitude
        //Radius are hard coded in the XML as of now
        println "SHPPR OnSale Request.. \n${fileContents_Search}"
        //logDebug("Search Request Xml..$k \n${fileContents_Search}")
        node_search = helper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
        responseBody_search = XmlUtil.serialize(node_search)
        //	println "Search Response Xml..\n${responseBody_search}"
        logDebug("Search Response Xml..\n${responseBody_search}")


        def hotelNode = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
        //List<HotelDTO> hList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)

        if (hotelNode != null) {
            List<HotelDTO> gtaHList = RequestNewHelper.getSearchPaxDetails_GTA(hotelNode, 0)
            List<HotelDTO> hHList = RequestNewHelper.getSearchPaxDetails_HBG(hotelNode, 0)
            if (gtaHList.size() > 0) {
                gtaHotelList.addAll(gtaHList)
            }

            softAssert.assertTrue(gtaHotelList.size() > 0, "GTA Hotels not found SHPPR OnSale Request")
            softAssert.assertTrue(!(hHList.size() > 0), "HBG  Hotels  found SHPPR OnSale Request")
        } else {
            softAssert.assertTrue(hotelNode != null && hotelNode.size() > 0 , "No Data exists for OnSale Request")
        }
            println "\n\n"
        softAssert.assertAll()

        where:
        rowNum | testDesc                            | searchRowNum
        9      | "SearchHotelPricePaxRequest -Onsale " | 9

    }

}

class SearchHotelPricePaxRequestXsell_C1237 extends SearchHotelPricePaxRequestXsell {
    @Override
    String getClientId() {
        return "1237"
    }
}

class SearchHotelPricePaxRequestXsell_C1238 extends SearchHotelPricePaxRequestXsell {
    @Override
    String getClientId() {
        return "1238"
    }
}

class SearchHotelPricePaxRequestXsell_C1239 extends SearchHotelPricePaxRequestXsell {
    @Override
    String getClientId() {
        return "1239"
    }
}

class SearchHotelPricePaxRequestXsell_C1244 extends SearchHotelPricePaxRequestXsell {
    @Override
    String getClientId() {
        return "1244"
    }
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class SearchHotelPricePaxRequestXsellSingleClient extends SearchHotelPricePaxRequestXsell {

    @Override
    String getClientId() {
        String clientId = System.getProperty("clientId")
        println "Using client Id $clientId"
        return clientId
    }
}


