package com.hbg.test.PriceSearch.SearchHotelPaxPrice

import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.kuoni.finance.automation.xml.util.ConfigProperties
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Ignore
class SearchHotelPricePaxRequestXsellCopy extends Specification {
    static final String SHEET_NAME_SEARCH = "SHPPR_Xsell"
    static final String EXCEL_SHEET = new File(ConfigProperties.getValue("excelSheet"))
    static final String URL = ConfigProperties.getValue("AuthenticationURL")
    static final String SITEID = ConfigProperties.getValue("SiteId")
    static final String FITSiteIdHeaderKey = ConfigProperties.getValue("FITSiteIdHeaderKey")
    SearchHotelPricePaxHelper helper = new SearchHotelPricePaxHelper(EXCEL_SHEET)
    int searchRowNum = 4

    private static def xmlParser = new XmlParser()
    SoftAssert softAssert = new SoftAssert()
    Node node_search
    String responseBody_search

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
    def flag = false
    Map itemMapGTA = null
    Map itemMapHBG = null
    Map fields = null

    static final String CITY = ConfigProperties.getValue("HBG_City_List")
    def cities = CITY.split(",")

    /** Search Hotel Price Pax Request - City Search */

    @Unroll
    @Issue("http://redmine/23432")
    def "SearchHotelPricePaxRequest - City Search - client Id #clientId"() {
        //println("Test Running ...$testDesc")
        System.setProperty("REQUEST.Client_id", clientId)

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxCitySearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k])
            println "Search REquest Xml.. \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)
            println "Search Response Xml..\n${responseBody_search}"
            itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search, fields)
            itemMapHBG = RequestNewHelper.getSearchDetailsHBG(node_search, fields)
            if (itemMapHBG != null) {
                itemDetails.put('itemCode', itemMapHBG["itemCode"])
                itemDetails.put('itemCity', itemMapHBG["itemCity"])
                itemDetails.put('itemName', itemMapHBG["itemName"])
                itemDetails.put('itemCityName', itemMapHBG["itemCityName"])
                itemDetails.put('roomCategory', itemMapHBG["roomCategory"])
                break
            }
            softAssert.assertFalse(itemMapGTA == null, "No GTA Hotels Found")
            softAssert.assertFalse(itemMapHBG == null, "No GTA Hotels Found")
            softAssert.assertFalse(itemCode.equals(""), "Available hotel not found")
            softAssert.assertTrue(node_search.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node_search.@ResponseReference}"
            softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPricePaxResponse != 0)

        }
        where:
        [rowNum ,testDesc, searchRowNum, clientId] << [[1], ["City Search"], [2], ConfigProperties.getValueList("clientIds")].combinations()
//        where:
//        rowNum | testDesc       | searchRowNum
//        1      | "City Search " | 2
    }

    /*SearchHotelPricePaxRequest - CityCode and ItemCode Search*/

    @Unroll
    def "SearchHotelPricePaxRequest - City +Item Code Search client Id #clientId"() {
        //println("Test Running ...$testDesc")

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxCityItemCodeSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k], itemDetails.get("itemCode"))
            println "Search REquest Xml.. \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)
            println "Search Response Xml..\n${responseBody_search}"
            itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search, fields)
            itemMapHBG = RequestNewHelper.getSearchDetailsHBG(node_search, fields)

            softAssert.assertFalse(itemMapGTA == null, "No GTA Hotels Found")
            softAssert.assertFalse(itemMapHBG == null, "No GTA Hotels Found")
            softAssert.assertFalse(itemCode.equals(""), "Available hotel not found")
            softAssert.assertTrue(node_search.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node_search.@ResponseReference}"
            softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPricePaxResponse != 0)

        }
        where:
        [rowNum ,testDesc, searchRowNum, clientId] << [[1], ["City Search and Item Code"], [2], ConfigProperties.getValueList("clientIds")].combinations()
//        rowNum | testDesc                    | searchRowNum
//        1      | "City Search and Item Code" | 2


    }

    /*SearchHotelPricePaxRequest - Child and Cot Search*/
    @Unroll
    def "SearchHotelPricePaxRequest - 1 Child and 1 cot client Id #clientId"() {
        //println("Test Running ...$testDesc")

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxChildandCotCodeSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k], itemDetails.get("itemCode"))
            println "Search REquest Xml.. \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)
            println "Search Response Xml..\n${responseBody_search}"
            itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search, fields)
            itemMapHBG = RequestNewHelper.getSearchDetailsHBG(node_search, fields)

            softAssert.assertFalse(itemMapGTA == null, "No GTA Hotels Found")
            softAssert.assertFalse(itemMapHBG == null, "No GTA Hotels Found")
            softAssert.assertFalse(itemCode.equals(""), "Available hotel not found")
            softAssert.assertTrue(node_search.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node_search.@ResponseReference}"
            softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPricePaxResponse != 0)

        }
        where:
        [rowNum ,testDesc, searchRowNum, clientId] << [[1], ["City Search with 1 Child and 1 cot"], [2], ConfigProperties.getValueList("clientIds")].combinations()

//        rowNum | testDesc                             | searchRowNum
//        1      | "City Search with 1 Child and 1 cot" | 2


    }

    /*SearchHotelPricePaxRequest - With Condition Dates*/

    @Unroll
    def "SearchHotelPricePaxRequest - With Charge Conditions row #rowNum Flag #testDesc Dates client Id #clientId"() {
        //println("Test Running ...$testDesc")

        for (k in 0..cities.size() - 1) {
            def fileContents_Search = helper.getPaxConditionsSearchXml(SHEET_NAME_SEARCH, searchRowNum, cities[k], itemDetails.get("itemCode"))
            println "Search REquest Xml.. \n${fileContents_Search}"
            node_search = helper.postHttpsXMLRequestWithHeaderNode(URL, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey, SITEID)
            responseBody_search = XmlUtil.serialize(node_search)
            println "Search Response Xml..\n${responseBody_search}"
            itemMapGTA = RequestNewHelper.getSearchDetailsGTA(node_search, fields)
            itemMapHBG = RequestNewHelper.getSearchDetailsHBG(node_search, fields)

            softAssert.assertFalse(itemMapGTA == null, "No GTA Hotels Found")
            softAssert.assertFalse(itemMapHBG == null, "No GTA Hotels Found")
            softAssert.assertFalse(itemCode.equals(""), "Available hotel not found")
            softAssert.assertTrue(node_search.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node_search.@ResponseReference}"
            softAssert.assertTrue(node_search.ResponseDetails.SearchHotelPricePaxResponse != 0)

        }
        where:
//        [rowNum ,testDesc, searchRowNum, clientId] << [[1,2], ["true", "false"], [2], ConfigProperties.getValueList("clientIds")].combinations()
//        [rowNum ,testDesc, searchRowNum, clientId] << [[[[1,"true"],[2, "false"]], [2]].combinations(), ConfigProperties.getValueList("clientIds")].combinations()
        rowNum | testDesc   | searchRowNum
        1      | "true"     | 2
        2      | "false"    | 2


    }


}
