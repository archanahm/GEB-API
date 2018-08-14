package com.hbg.test.SearchChargeConditions

import spock.lang.IgnoreIf

import java.util.List
import java.util.Map
import com.hbg.test.XSellBaseTest
import com.hbg.test.PriceSearch.SearchHotelPaxPrice.SearchHotelPricePaxRequestXsell

import org.testng.asserts.SoftAssert;

import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPriceHelper
import com.hbg.helper.SearchHotelPricePaxHelper.SearchHotelPricePaxHelper
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil

import groovy.xml.XmlUtil
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assert.assertTrue


abstract class SearchChargeConditionsWithPkgRateFalseTest extends XSellBaseTest{
	
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
	List<HotelDTO> gtaHotelList = new ArrayList<HotelDTO>()

	@Shared
	HotelDTO successHotelDTO = null
	
	@Unroll
	/** SearchChargeConditions - Search Hotel Price Request */
	def "1. Search Charge Condtiions - Search Hotel Price Request"(){
		println("1.Search Charge Conditions - Search Hotel Price Request")
		Node node_search
		String responseBody_search
		cities=numOfCitiesList(6)

		for(k in 0.. cities.size()-1 ){
			def fileContents_Search = helper.getCitySearchDurationWithPkgRatesXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "**************SearchChargeConditions - Search Hotel Price Request************"
			println "Search Request for SearchChargeConditions.. \n${fileContents_Search}"			
			 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			 responseBody_search = XmlUtil.serialize(node_search)
			
			logDebug("Response Xml.for Search ChargeConditions\n${responseBody_search}")			
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)	
			
			if (hList.size() > 0){
				for (HotelDTO dto : hList){
					def rNode = dto.getRoomCategoryNode()
					def pkgRate = rNode.ItemPrice.@PackageRate[0]
					if (pkgRate != null && pkgRate == "false"){
						hotelList.add(dto)
					}
				}
			}
		}		
			
		softAssert.assertTrue(hotelList.size() > 0,"HBG Hotels not found for Search Charge Conditions")			
		softAssert.assertAll()
		println "\n\n"

		where:
		rowNum 	| testDesc 				| searchRowNum
		1		|"With Duration" 		| 1
	}

	/** SearchChargeConditions - Search Hotel Price Request WithDateFormatResponse*/

	def "2. Search Charge Condtiions - WithDateFormatResponse"(){
		println("2. Search Charge Condtiions - WithDateFormatResponse")
		Node node_search
		String responseBody_search
		
		println "HBG Hotel list Size:"+hotelList.size()
		assertTrue("No HBG hotels found or Search Charge Conditions", hotelList.size() != 0)
		HotelDTO hDTO
	 
		def itemCity = []
		def itemCode = []
		def itemRateKey = []

		int searchchargeconditions
		if (hotelList!=null)
			 {	
				 	int l = hotelList.size()-1
					for (k in  0 .. l) 
					{	 
					 hDTO = hotelList.get(k)
					 itemCity << hDTO.getItemCity()
					 itemCode << hDTO.getItemCode()
					 itemRateKey << hDTO.getCategoryId()	
					}		 
					 itemDetails.put('itemCity',itemCity)
					 itemDetails.put('itemCode',itemCode)
					 itemDetails.put('itemRateKey',itemRateKey)
					 def expectedValue ="cancellation,amendment"
							 for (int i = 0; i < l; i++) {
								 //HotelDTO hDTO = hotelList.get(i)
								 def fileContents_Search = helper.getsearchargeconditions(SHEET_NAME_SEARCH, rowNum, itemDetails.itemCity[i], itemDetails.itemCode[i], itemDetails.itemRateKey[i])
								 println "**************SearchChargeConditions - Search Hotel Price Request WithDateFormatResponse************"
								 println "Search Charge Conditions Request with Date Format Response.. \n${fileContents_Search}"
								 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents_Search, 'search', FITSiteIdHeaderKey,SITEID)
						 responseBody_search = XmlUtil.serialize(node_search)		
						logDebug("Search Response Xml..\n${responseBody_search}")

								 searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
								 if(searchchargeconditions> 0){
									 println "total count"+ searchchargeconditions
									 softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed")
									 int chargeconditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
									 for(int k in 0 ..chargeconditionCnt-1){

										 int conditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition.size()
										 if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].@Type.equals("cancellation")) {


											 for (int j in 0..conditionCnt - 1) {
												 if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@Charge.equals("true")){
													 softAssert.assertTrue(helper.isThisDateValid(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@ToDate), "Correct To date dateformat is not displayed when charge is true")
													 softAssert.assertTrue(helper.isThisDateValid(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@FromDate), "Correct From date dateformat is not displayed when charge is true ")
												 }
												 else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@Charge.equals("false")){
													 softAssert.assertTrue(helper.isThisDateValid(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@FromDate), "Correct From date dateformat is not displayed when charge is false")

												 }
											 }
										 }else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].@Type.equals("amendment")){
											 if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition.size() >0){
												 softAssert.assertTrue(helper.isThisDateValid(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[0].@FromDate), "Correct From date dateformat is not displayed when ChargeCondition is amendment")

											 }

										 }

									 }


									 helper.validateSuccessResponse(responseBody_search,expectedValue)
									 break
								 }

							 }
				 softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed even after trying fo " + hotelList.size() + " rate keys")
			 }else
	{
		softAssert.assertTrue(hotelList!=null,"HBG Hotels not found for SearchChargeConditions Search")


		println "\n\n"
		
	}

	softAssert.assertAll()
	where:
	rowNum 	| testDesc 				| searchRowNum
	2		|"With Duration" 		| 1
}

/** SearchChargeConditions - Search Hotel Price Request Without Date Format Response */

def "3.Search Charge Condtiions - WithOutDateFormatResponse"(){
	println("3.Search Charge Condtiions - WithOutDateFormatResponse")
	Node node_search
	String responseBody_search
	assertTrue("No HBG hotels found or Search Charge Conditions", hotelList.size() != 0)

	HotelDTO hDTO
	
			def itemCity = []
			def itemCode = []
			def itemRateKey = []
			int m
			 if (hotelList!=null)
			 {	
				int l = hotelList.size()-1
				for (k in  0 .. l) {
	
					hDTO = hotelList.get(k)
					itemCity << hDTO.getItemCity()
					itemCode << hDTO.getItemCode()
					itemRateKey << hDTO.getCategoryId()
								}
				itemDetails.put('itemCity',itemCity)
				itemDetails.put('itemCode',itemCode)
				itemDetails.put('itemRateKey',itemRateKey)
				 def expectedValue ="cancellation,amendment"
				 int searchchargeconditions
				for (int i=0;i<l;i++)
				{
				//HotelDTO hDTO = hotelList.get(i)
				def fileContents_Search = helper.getsearchargeconditionswithoutdates(SHEET_NAME_SEARCH,searchRowNum,itemDetails.itemCity[i], itemDetails.itemCode[i],itemDetails.itemRateKey[i])
				println "**************SearchChargeConditions - Search Hotel Price Request Without Date Format Response************"
				println "Search Request Xml.. \n${fileContents_Search}"		
				 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
				 responseBody_search = XmlUtil.serialize(node_search)		
				logDebug("Search Response Xml..\n${responseBody_search}")

				//println "value dom dom:"+ node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()

				 searchchargeconditions=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
					if(searchchargeconditions> 0){
						println "total count"+ searchchargeconditions
						softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed")
						int chargeconditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()
						for(int k in 0 ..chargeconditionCnt-1){

							int conditionCnt=node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition.size()
							if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].@Type.equals("cancellation")) {


								for (int j in 0..conditionCnt - 1) {
									if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@Charge.equals("true")){
									softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].ToDay != null, "Correct Today is not displayed when charge is true")
									softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].FromDay != null, "Correct FromDay is not displayed when charge is true ")
									}
									else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].@Charge.equals("false")){
										softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[j].FromDay != null , "Correct FromDay is not displayed when charge is false")

									}
								}
							}else if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].@Type.equals("amendment")){
								if(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition.size() >0) {
									softAssert.assertTrue(node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition[k].Condition[0].FromDay != null, "Correct FromDay is not displayed when ChargeCondition is amendment")
								}
							}

						}


						helper.validateSuccessResponse(responseBody_search,expectedValue)
						break
					}

				}
				 softAssert.assertTrue(searchchargeconditions>0, "No Charge Conditions displayed even after trying for " + hotelList.size() + " rate keys")

				//softAssert.assertFalse((node_search.ResponseDetails.SearchChargeConditionsResponse.ChargeConditions.ChargeCondition.size()!=0),"No Hotels Returned with Search Charge Conditions")

			 }else
			 {
				 softAssert.assertTrue(hotelList!=null,"HBG Hotels not found for SearchChargeConditions Search")


				 println "\n\n"

			 }

	softAssert.assertAll()
	where:
	rowNum 	| testDesc 				| searchRowNum
	3		|"With Duration" 		| 1
	}
}

class SearchChargeConditionsWithPkgRateTest_C1237 extends SearchChargeConditionsWithPkgRateFalseTest {
	@Override
	String getClientId() {
		return "1237"
	}
}

class SearchChargeConditionsWithPkgRateFalseTest_C1238 extends SearchChargeConditionsWithPkgRateFalseTest {
	@Override
	String getClientId() {
		return "1238"
	}
}

class SearchChargeConditionsWithPkgRateFalseTest_C1239 extends SearchChargeConditionsWithPkgRateFalseTest {
	@Override
	String getClientId() {
		return "1239"
	}
}

class SearchChargeConditionsWithPkgRateFalseTest_C1244 extends SearchChargeConditionsWithPkgRateFalseTest {
	@Override
	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class SearchChargeConditionsWithPkgRateFalseTestSingleClient extends SearchChargeConditionsWithPkgRateFalseTest {

	@Override
	String getClientId() {
		String clientId = System.getProperty("clientId")
		println "Using client Id $clientId"
		return clientId
	}
}



