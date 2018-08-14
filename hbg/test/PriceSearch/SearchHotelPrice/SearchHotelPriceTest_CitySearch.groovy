package com.hbg.test.PriceSearch.SearchHotelPrice

import groovy.xml.XmlUtil

import org.testng.asserts.SoftAssert

import spock.lang.IgnoreIf;
import spock.lang.Shared
import spock.lang.Unroll

import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.test.XSellBaseTest
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil

abstract class SearchHotelPriceTest_CitySearch extends XSellBaseTest{
	static final String EXCEL_SHEET=new File(ConfigProperties.getValue("priceSearchSheet"))
	static final String SHEET_NAME_SEARCH="xmle2e_HBG_SHPR_Ref"
	static final String SITEID= ConfigProperties.getValue("SiteId")
	static final String FITSiteIdHeaderKey= ConfigProperties.getValue("FITSiteIdHeaderKey")
	SearchHotelPriceHelper helper=new SearchHotelPriceHelper(EXCEL_SHEET,getClientId())
	int rowNum
	String sheet,testDesc,Data
	private static def xmlParser = new XmlParser()
	SoftAssert softAssert= new SoftAssert()
	XMLValidationUtil XmlValidate = new XMLValidationUtil()
	int searchRowNum
	def addRef = ""
	def dateChargeCondition
	def noOfCots
	
	@Shared
	def cities
	
	@Unroll
	/** City Search Duration - verify Response */
	def "#rowNum Verify City Search || #testDesc"(){
		
		println("Test Running ...$testDesc")
		
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		
		cities = numOfCitiesListForSearch(6)
		
		boolean hbgHotelsFound = false
		boolean hotelsFound = false

		List<HotelDTO> hList = null
		List<HotelDTO> hGTAList = null
		String city = null
		boolean roomConfirmationCheckPassed = true
		boolean roomDescriptionCheckPassed = true
		
		for(int k=0;k<cities.size();k++){
			
			def fileContents_Search = helper.getCitySearchDurationXml(SHEET_NAME_SEARCH,searchRowNum,cities[k])
			println "Search Request Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
			 
			logDebug("Response XML : "+responseBody)
			if (responseBody.contains("<ErrorId>")){
				softAssert.assertTrue(false, "Failed : Error response returned for the city : "+cities[k])
			}
	
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
						
			if (hotelNode != null){
				hotelsFound = true
				hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				hGTAList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
				
				if (hList != null && hList.size() > 0){
					city = cities[k]
					hbgHotelsFound = true
					for (HotelDTO dto : hList){
						if (dto.getRoomConfirmationCode().equals("IM") && !dto.getRoomConfirmationText().equals("AVAILABLE")){
							roomConfirmationCheckPassed = false
						}
						
						if (dto.getRoomDescription() == null || dto.getRoomDescription().equals("")){
							roomDescriptionCheckPassed = false
							
						}
					}
					break
				}
			}
		}
		
		if (!hbgHotelsFound){
			softAssert.assertTrue(false,"No HBG hotels found")
		}else{
			for (HotelDTO dto : hList){
				if (city != dto.getItemCity()){
					softAssert.assertTrue(false, "Response contains wrong city code")
					break
				}
			}
			if (hGTAList == null || hGTAList.size() ==0){
				softAssert.assertTrue(false, "Failed : No GTA hotels found for the city code :"+ city)
			}
		}

		softAssert.assertTrue(roomConfirmationCheckPassed, "Room Confirmation text is not AVAILABLE for the Code IM")
		softAssert.assertTrue(roomDescriptionCheckPassed, "Room description is missing/empty")
		
		println "\n\n"
		softAssert.assertAll()

		where:
		rowNum 	| testDesc 				| searchRowNum
		1		|"With Duration" 		| 1
	}
	
	@Unroll
	/** City Search Duration With Child - verify Response */
	def "#rowNum Verify City Search with Child || #testDesc"(){
		
		println("Test Running ...$testDesc")
		
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		
		boolean hbgHotelsFound = false
		boolean hotelsFound = false

		List<HotelDTO> hList = null
		List<HotelDTO> hGTAList = null
		String city = null
		
		cities = numOfCitiesListForSearch(6)
		
		for(int k=0;k<cities.size();k++){
			
			def fileContents_Search = helper.getCitySearchWithChildXml(SHEET_NAME_SEARCH,searchRowNum,cities[k], noOfCots)
			println "Search Request Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
			 
			logDebug("Response XML : "+responseBody)
			if (responseBody.contains("<ErrorId>")){
				softAssert.assertTrue(false, "Failed : Error response returned for the city : "+cities[k])
			}
	
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			
			if (hotelNode != null){
				hotelsFound = true
				hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				hGTAList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
				if (hList != null && hList.size() > 0){
					city = cities[k]
					hbgHotelsFound = true
//					for (HotelDTO dto : hList){
//						if (dto.getSharingBedding() == "false"){
//							softAssert.assertTrue(false, "SharingBedding value is false (it should be true) in Response")
//							break
//						}
//					}
					break
				}
			}
		}
		
		if (!hbgHotelsFound){
			softAssert.assertTrue(false,"No HBG hotels found")
		}else{
			for (HotelDTO dto : hList){
				if (city != dto.getItemCity()){
					softAssert.assertTrue(false, "Response contains wrong city code")
					break
				}
			}
			if (hGTAList == null || hGTAList.size() ==0){
				softAssert.assertTrue(false, "Failed : No GTA hotels found for the city code :"+ city)
			}
		}

		println "\n\n"
		softAssert.assertAll()

		where:
		rowNum 	| testDesc 				| searchRowNum | noOfCots
		2		|"With Child" 			| 2 | 0
		3		|"With Child and Cot" 	| 3 | 1
	}
	
	@Unroll
	/** City Search With Item Code - verify Response */
	def "#rowNum Verify City Search with Item Code || #testDesc"(){
		
		println("Test Running ...$testDesc")
		
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		
		cities = numOfCitiesListForSearch(6)
		
		boolean hbgHotelsFound = false
		boolean hotelsFound = false

		List<HotelDTO> hList = null
		List<HotelDTO> hGTAList = null
		String city = null
		
		HotelDTO dto = getHotelDTO(null,"TB","0","1")
		if (dto == null){
			softAssert.assertTrue(false, "No HBG hotels found")
			softAssert.assertAll()
		}
		def fileContents_Search = helper.getCitySearchWithOnlyItemCodeXml(SHEET_NAME_SEARCH,searchRowNum,dto.getItemCity(), dto.getItemCode())
		println "Search Request Xml.. \n${fileContents_Search}"
		node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
		responseBody = XmlUtil.serialize(node_search)
		 
		logDebug("Response XML : "+responseBody)
		if (responseBody.contains("<ErrorId>")){
			softAssert.assertTrue(false, "Failed : Error response returned")
			softAssert.assertAll()
		}

		def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			
		if (hotelNode != null){
			hotelsFound = true
			hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			hGTAList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
			if (hList != null && hList.size() > 0){
				hbgHotelsFound = true
				for (HotelDTO hDTO : hList){
					if (dto.getItemCity() != hDTO.getItemCity() || dto.getItemCode() != hDTO.getItemCode()){
						softAssert.assertTrue(false, "Response contains wrong city/item code")
						break
					}
				}
			}else{
				softAssert.assertTrue(false,"No HBG hotels found")
			}
		}
		if (!hotelsFound){
			softAssert.assertTrue(false,"No hotels (GTA & HBG)) found in all the cities")
		}else if (!hbgHotelsFound){
			softAssert.assertTrue(hbgHotelsFound,"No HBG hotels found in all the cities")
		}
		println "\n\n"
		softAssert.assertAll()

		where:
		rowNum 	| testDesc 				| searchRowNum
		4		|"With Only Item Code" 		| 4
	}
	
	
	@Unroll
	/** City Search With Date Format Response- verify Response */
	def "#rowNum Verify City Search with Condition Date || #testDesc"(){
		println("Test Running ...$testDesc")
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		cities = numOfCitiesListForSearch(6)
		boolean hbgHotelsFound = false
		boolean hotelsFound = false

		List<HotelDTO> hList = null
		List<HotelDTO> hGTAList = null
		String city = null
		HotelDTO dto = null
		def fileContents_Search = null
		if (dateChargeCondition == "true"){
			dto = getHotelDTO("DateChargeConditionTrue","SB","0","1")
			if (dto == null){
				softAssert.assertTrue(false, "No HBG hotels found")
				softAssert.assertAll()
			}
	
			fileContents_Search = helper.getCitySearchXmlWithDateChargeCondition(SHEET_NAME_SEARCH,searchRowNum,dto.getItemCity(), dto.getItemCode(), "true")
		}else if (dateChargeCondition == "false"){
			dto = getHotelDTO("DateChargeConditionFalse","SB","0","1")
			if (dto == null){
				softAssert.assertTrue(false, "No HBG hotels found")
				softAssert.assertAll()
			}
	
			fileContents_Search = helper.getCitySearchXmlWithDateChargeCondition(SHEET_NAME_SEARCH,searchRowNum,dto.getItemCity(), dto.getItemCode(), "false")
		}else{
			dto = getHotelDTO("DateChargeConditionEmpty","SB","0","1")
			if (dto == null){
				softAssert.assertTrue(false, "No HBG hotels found")
				softAssert.assertAll()
			}
	
			fileContents_Search = helper.getCitySearchXmlWithEmptyChargeCondition(SHEET_NAME_SEARCH,searchRowNum,dto.getItemCity(), dto.getItemCode())
		}
		
		println "Search Request Xml.. \n${fileContents_Search}"
		node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
		responseBody = XmlUtil.serialize(node_search)
		logDebug("Response XML : "+responseBody)
		if (responseBody.contains("<ErrorId>")){
			softAssert.assertTrue(false, "Failed : Error response returned")
			softAssert.assertAll()
		}
		def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
		if (hotelNode != null){
			hotelsFound = true
			hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			hGTAList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
			if (hList != null && hList.size() > 0){
				hbgHotelsFound = true
				for (HotelDTO hDTO : hList){
					if (dto.getItemCity() != hDTO.getItemCity() || dto.getItemCode() != hDTO.getItemCode()){
						softAssert.assertTrue(false, "Response contains wrong city/item code")
						break
					}
				}
				if (dateChargeCondition == "true"){
					softAssert.assertTrue(helper.checkChargeConditionType(hotelNode, true),"Response return ChargeCondition with Day but expected with Date")
				}else{
					softAssert.assertTrue(helper.checkChargeConditionType(hotelNode, false),"Response return ChargeCondition with Date but expected with Day")
				}
			}else{
				softAssert.assertTrue(false,"No HBG hotels found")
			}
		}
		if (!hotelsFound){
			softAssert.assertTrue(false,"No hotels (GTA & HBG)) found in all the cities")
		}else if (!hbgHotelsFound){
			softAssert.assertTrue(hbgHotelsFound,"No HBG hotels found in all the cities")
		}
		println "\n\n"
		softAssert.assertAll()
		where:
		rowNum 	| testDesc 								| searchRowNum 	| dateChargeCondition
		5		|"With Date Format Response - True" 	| 5 			| "true"
		6		|"With Date Format Response - False" 	| 6 			| "false"
		7		|"With Date Format Response - None" 	| 7 			| ""
	}

	@Unroll
	/** Area Search With and With/Without HBG Hotels- verify Response */
	def "#rowNum Verify Area Search with HBG || #testDesc"(){
		
		println("Test Running ...$testDesc")
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		
		def areas = numOfAreasList(3)
		
		List<HotelDTO> hList = null
		List<HotelDTO> hGTAList = null
		String city = null
		boolean hbgHotelsFound = false
		boolean hotelsFound = false
		
		def clientId = getClientId()
		if (withHBG=="false"){
			clientId = "1004"
		}
		for(int k=0;k<areas.size();k++){
			
			def fileContents_Search = helper.getAreaSearchXml(SHEET_NAME_SEARCH,searchRowNum,areas[k], clientId)
			println "Search Request Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
			if (responseBody.contains("<ErrorId>")){
				softAssert.assertTrue(false, "Failed : Error response returned for the area :"+areas[k])
			}
	
			logDebug("Response XML : "+responseBody)
			 
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
						
			if (hotelNode != null){
				hotelsFound = true
				hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				hGTAList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
				println ">>>hList size : "+hList.size()
				if (hList.size()>0){ 
					hbgHotelsFound = true 
					if (hGTAList == null || hGTAList.size()==0){
						softAssert.assertTrue(false,"No GTA hotels found")
					}
					break				
				}
			}
		}
		
		if (!hotelsFound){
			softAssert.assertTrue(false,"No hotels (GTA & HBG)) found in all the areas")
		}
		
		if (withHBG=="true" && !hbgHotelsFound){
			softAssert.assertTrue(false,"Response should return HBG hotels but it's not returned any HBG hotels")
		}else if (withHBG=="false" & hbgHotelsFound){
			softAssert.assertTrue(false,"Response shouldn't return HBG hotels but it's returned HBG hotels")
		}

		println "\n\n"
		softAssert.assertAll()
		
		where:
		rowNum 	| testDesc 							| searchRowNum | withHBG
		8		|"With HBG Hotels in response" 		| 8 | "true"
		9		|"Without HBG Hotels in response" 	| 9 | "false"
	}
	
	
	@Unroll
	/** Geocode Search for HBG Hotels- verify Response */
	def "#rowNum Verify GeoCode Search for HBG Hotels || #testDesc"(){
		println("Test Running ...$testDesc")
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		
		boolean hbgHotelsFound = false
		List<HotelDTO> hList = null
		List<HotelDTO> hGTAList = null
		String city = null
		
		def fileContents_Search = helper.getGeocodeSearchXml(SHEET_NAME_SEARCH,searchRowNum)
		println "Search Request Xml.. \n${fileContents_Search}"
		node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
		responseBody = XmlUtil.serialize(node_search)
		logDebug("Response XML : "+responseBody)
		if (responseBody.contains("<ErrorId>")){
			softAssert.assertTrue(false, "Failed : Error response returned")
			softAssert.assertAll()
		}
		 
		def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
					
		if (hotelNode != null){
			hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			if (hList == null || hList.size()==0){
				softAssert.assertTrue(false, "No HBG hotels retunred")
			}
		}else if (responseBody.contains("<ErrorId>")){
			softAssert.assertTrue(false,"Error response returned")
		}else{
			softAssert.assertTrue(false,"No Hotels Available")
		}
		println "\n\n"
		softAssert.assertAll()
		
		where:
		rowNum 	| testDesc 							| searchRowNum
		10		|"With Latitude, Longitude, Radius" 	| 10
	}
	

	@Unroll
	@IgnoreIf({System.getProperty("testEnv") != "sit"})
	/** Verify Essential Information for HBG Hotels- verify Response */
	
	def "#rowNum Verify Essential Information || #testDesc"(){
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = false
		boolean isEssentialInformationValid = false
		softAssert= new SoftAssert()

		def fileContents_Search = helper.getEssentialInformationXml(SHEET_NAME_SEARCH,searchRowNum)
		println "Search REquest Xml.. \n${fileContents_Search}"
		 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
		 responseBody = XmlUtil.serialize(node_search)
		logDebug("Search Response Xml..\n${responseBody}")
		if (responseBody.contains("ErrorId")){
			softAssert.assertTrue(false, "Failed: Received Error response")
		}else{
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			if (hList.size() > 0){
				isEssentialInformationValid = helper.getEssentialInformationDetailsHBG(node_search)
				softAssert.assertTrue(isEssentialInformationValid, "Invalid Essential information available")
			}else{
				softAssert.assertTrue(false, "HBG hotel not found")
			}
		}
		
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 							| searchRowNum
		11		|"With Essential Info" 				| 11

	}
	
	@Unroll
	/** Verify Include Price Break Down for HBG Hotels- verify Response */
	def "#rowNum Verify Include Price Break Down || #testDesc"(){
		println("Test Running ...$testDesc")
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = false
		
		cities = numOfCitiesListForSearch(6)
		HotelDTO hDTO = null
		def fileContents_Search
		if (rowNum == 12){
			hDTO = getHotelDTOWithExtraBed(roomCode,"0","1", 0)
			if (hDTO == null){
				softAssert.assertTrue(false, "No HBG hotels found")
				softAssert.assertAll()
			}
	
			fileContents_Search = helper.getIncludePriceBreakDownXml(SHEET_NAME_SEARCH,searchRowNum, hDTO.getItemCity(), hDTO.getItemCode())
		}else{
			hDTO = getHotelDTOWithExtraBed(roomCode,"1","1", 1)
			if (hDTO == null){
				softAssert.assertTrue(false, "No HBG hotels found")
				softAssert.assertAll()
			}
	
			fileContents_Search = helper.getIncludePriceBreakDownXmlWithExtraBed(SHEET_NAME_SEARCH,searchRowNum, hDTO.getItemCity(), hDTO.getItemCode())
		}
		
		println "Search REquest Xml.. \n${fileContents_Search}"
		node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
		responseBody = XmlUtil.serialize(node_search)
		logDebug("Search Response Xml..\n${responseBody}")
		if (responseBody.contains("ErrorId")){
			softAssert.assertTrue(false, "Failed : Received Error response")
		}else{
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			
			 List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			 if (hList.size() == 0){
				 softAssert.assertTrue(isHBGHotelAvailable, "HBG Available hotel not found")
			 }else{
			 	for (HotelDTO dto : hList){
					 def rCategoryNode = dto.getRoomCategoryNode()
					def rooms = rCategoryNode.HotelRoomPrices.HotelRoom
					boolean flag = true
					 for (int j=0;j<rooms.size();j++){
						 String code = rooms[j].@Code
						 Map atrributes = rooms[j].attributes()
						 List<String> keys = new ArrayList<String>(atrributes.keySet())
						 keys.removeAll("Code","ExtraBed","NumberOfExtraBeds","NumberOfCots","NumberOfRooms","ChildAge")
						 if(keys.size()> 0)
						 {
							 softAssert.assertTrue(false, "HotelRoomPrice.HotelRoom tag contains addational attributes: " + keys )
							 flag = false
							 break
						 }
						 if (code == null){
							 softAssert.assertTrue(false, "Attribute 'Code' is missing in HotelRoomPrice.HotelRoom tag")
							 flag = false
							 break
						 }
						 def priceRange = rooms[j].PriceRanges.PriceRange
						 for (int k=0;k<priceRange.size();k++){
							 def grossPrice = priceRange[k].Price.@Gross[0]
							 if (grossPrice == null ){
								 softAssert.assertTrue(false, "Attribute 'Gross' is missing in HotelRoomPrice.HotelRoom.PriceRanges.PriceRange.Price tag")
								 flag = false
								 break
							 }
							 if (rowNum == 12 && grossPrice == "0.00"){
								 softAssert.assertTrue(false, "Attribute 'Gross' is zero in HotelRoomPrice.HotelRoom.PriceRanges.PriceRange.Price tag")
								 flag = false
								 break
							 }
							 def priceAvailable = rooms[j].PriceRanges.PriceRange[0].Price.@Avilable[0]
							 if (priceAvailable != null){
								 softAssert.assertTrue(false, "Wrong attribute (Available) is exists in HotelRoomPrice.HotelRoom.PriceRanges.PriceRange.Price tag")
								 flag = false
								 break
							 }
						 }
						 if (!flag){
							 break
						 }
					 }
					 if (!flag){
						 break
					 }
				 }
			 }
		}
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 										| searchRowNum | roomCode
		12		|"Include Price Break Down" 						| 12 | "DB"
		//13		|"Include Price Break Down with 2 rooms" 		| 13 | "TB"
		14		|"Include Price Break Down with 1 child 1 cot" 	| 14 | "TB"
	}
	
	@Unroll
	/** Verify With Id for HBG Hotels- verify Response */
	def "#rowNum Verify with Id  || #testDesc"(){
		println("Test Running ...$testDesc")
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = false
		cities = numOfCitiesListForSearch(6)
		for (int i=0;i<cities.size();i++){
			def fileContents_Search = helper.getSearchRequestWithIdXml(SHEET_NAME_SEARCH,searchRowNum,cities[i])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
			logDebug("Search Response Xml..\n${responseBody}")
			if (responseBody.contains("ErrorId")){
				continue
			}else{
				def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
				
				 List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				 List<HotelDTO> hGTAList = RequestNewHelper.getSearchDetails_GTA(hotelNode, 0)
				 if (hList.size() > 0){
					 isHBGHotelAvailable = true
					 if (hGTAList == null || hGTAList.size()==0){
						 softAssert.assertTrue(false,"Response contains HBG hotels but not GTA hotels for the city : "+ cities[i])
					 }
					 break
				 }
			}
		}
		
		softAssert.assertTrue(isHBGHotelAvailable, "HBG hotel not found")
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 					| searchRowNum
		15		|"With Id in Room" 			| 15

	}
	
	@Unroll
	/** Verify with Show Pacakage rates for HBG Hotels- verify Response */
	def "#rowNum Verify with Show Package Rates || #testDesc"(){
		println("Test Running ...$testDesc")
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = false
		cities = numOfCitiesListForSearch(6)
		HotelDTO hDTO = getHotelDTO("PackageRates", "TB","0","1")
		if (hDTO == null){
			softAssert.assertTrue(false, "No HBG hotels found")
			softAssert.assertAll()
		}

		def fileContents_Search = helper.getSearchRequestWithPackageXml(SHEET_NAME_SEARCH,searchRowNum,hDTO.getItemCity(), hDTO.getItemCode())
		println "Search REquest Xml.. \n${fileContents_Search}"
		 node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
		 responseBody = XmlUtil.serialize(node_search)
		logDebug("Search Response Xml..\n${responseBody}")
		if (responseBody.contains("ErrorId")){
			softAssert.assertTrue(false, "Failed : Received Error response")
		}else{
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			
			 List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			 if (hList.size() == 0){
				 softAssert.assertTrue(false, "HBG hotels not found")
			 }else{
			 	softAssert.assertTrue(helper.verifyResponseContainsPackageRates(hotelNode),"HBG hotel package attribute value : expected is True, but actual is False/Empty")
			 }
		}
		
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 					| searchRowNum
		16		|"With Show Package Rates" 	| 16

	}
	
	@Unroll
	/** Verify Cheapest Room Rate with HBG Hotels- verify Response */
	@IgnoreIf({System.getProperty("platform") != "nova"})
	def "#rowNum Verify Cheapest Room Rate || #testDesc"(){
		println("Test Running ...$testDesc")
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = false
		boolean isHotelHasMoreThanOneRateKey = false
		cities = numOfCitiesListForSearch(6)
		HotelDTO hDTO = getHotelDTOWithMultipleRoomsInSameCity("SB","0","1")
		if (hDTO == null){
			softAssert.assertTrue(false, "No HBG hotels found")
			softAssert.assertAll()
		}

		def fileContents_Search = helper.getCheapestRoomRateRequestXml(SHEET_NAME_SEARCH,searchRowNum, hDTO.getItemCity(), hDTO.getItemCode())
		println "Search REquest Xml.. \n${fileContents_Search}"
		node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
		responseBody = XmlUtil.serialize(node_search)
		logDebug("Search Response Xml..\n${responseBody}")
		if (responseBody.contains("ErrorId")){
			softAssert.assertTrue(false, "Failed: Received Error response")
		}else{
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			
			 List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			 if (hList.size() ==0){
				 softAssert.assertTrue(false, "HBG Hotel not found")
			 }else if (hList.size() > 1){
			 	softAssert.assertTrue(false, "Failed : Expected only one HBG ratekey but returned more than one HBG rate key in response")
			 }
		}
		
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 					| searchRowNum
		19		|"Cheapest Room Rate" 		| 19
	}
	
	@Unroll
	/** Verify Mealbasis Request with HBG Hotels- verify Response */
	def "#rowNum Verify MealBasis || #testDesc"(){
		println("Test Running ...$testDesc")
		softAssert= new SoftAssert()
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = false
		cities = numOfCitiesListForSearch(6)
		for (int i=0;i<cities.size();i++){
			def fileContents_Search = helper.getMealBasisRequestXml(SHEET_NAME_SEARCH,searchRowNum, cities[i], mealBasisCode)
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
			logDebug("Search Response Xml..\n${responseBody}")
			if (responseBody.contains("ErrorId")){
				continue
			}else{
				def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
				
				 List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				 println " city hotels count : "+hList.size()
				 if (hList.size() > 0){
					 isHBGHotelAvailable = true
					 for (HotelDTO dto : hList){
						 if (dto.getMealBasisCode() != mealBasisCode || dto.getMealBasisText() != mealBasisText){
							 softAssert.assertTrue(false, "Failed : MealBasis Code/text : expected  "+ mealBasisCode+"/"+ mealBasisText +" but actual is "+dto.getMealBasisCode() + "/"+ dto.getMealBasisText())
							 break
						 }
					 }
					 break
				 }
			}
		}
		softAssert.assertTrue(isHBGHotelAvailable, "HBG Available hotel not found")
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 					| searchRowNum | mealBasisCode | mealBasisText
		20		|"MealBasis None (N) Request" 		| 20 | "N" | "None"
		20		|"MealBasis Breakfast (B) Request" 		| 20 | "B" | "Breakfast"

	}
	
	@Unroll
	/** Verify StarRating  with HBG Hotels- verify Response */
	def "#rowNum Verify StarRating || #testDesc"(){
		softAssert= new SoftAssert()
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = false
		cities = numOfCitiesListForSearch(6)
		for (int i=0;i<cities.size();i++){
			def fileContents_Search = helper.getStarRatingRequestXml(SHEET_NAME_SEARCH,searchRowNum, cities[i])
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
			logDebug("Search Response Xml..\n${responseBody}")
			if (responseBody.contains("ErrorId")){
				continue
			}else{
				def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
				
				 List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				 println " city hotels count : "+hList.size()
				 if (hList.size() > 0){
					 isHBGHotelAvailable = true
					 for (HotelDTO dto : hList){
						 if (dto.getStarRating() != "4"){
							 softAssert.assertTrue(false, "Failed : StarRating expected  4 but actual is "+dto.getStarRating())
							 break
						 }
					 }
					 break
				 }
			}
		}
		softAssert.assertTrue(isHBGHotelAvailable, "HBG Available hotel not found")
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 					| searchRowNum
		22		|"Star Rating" 				| 22
	}
	
	@Unroll
	/** Verify Location Code  with HBG Hotels- verify Response */
	def "#rowNum Verify LocationCode || #testDesc"(){
		softAssert= new SoftAssert()
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = false
		
		cities = numOfCitiesListForSearch(6)
		for (int i=0;i<cities.size();i++){
			def fileContents_Search = helper.getLocationCodeRequestXml(SHEET_NAME_SEARCH,searchRowNum, cities[i], locCode)
			println "Search REquest Xml.. \n${fileContents_Search}"
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
			logDebug("Search Response Xml..\n${responseBody}")
			if (responseBody.contains("ErrorId")){
				continue
			}else{
				def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
				
				 List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				 println " city hotels count : "+hList.size()
				 if (hList.size() > 0){
					 isHBGHotelAvailable = true
					 for (HotelDTO dto : hList){
						 def locations = dto.getHotelNode().LocationDetails.Location
						 boolean locationMatched = false
						 for (int k=0;k<locations.size();k++){
							 def locationCode = locations[k].@Code[0]
							 if (locationCode == locCode){
								 locationMatched = true
								 break
							 }
						 }
						 if (!locationMatched){
							 softAssert.assertTrue(false, "Failed : Response xml not contains the location code "+locCode)
							 break
						 }
					 }
					 break
				 }
			}
		}
		softAssert.assertTrue(isHBGHotelAvailable, "HBG Available hotel not found")
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 					| searchRowNum | locCode
		23		|"Location Code G1" 			| 23 | "G1"
		23		|"Location Code G9" 			| 23 | "G9"
	}
	
	@Unroll
	@IgnoreIf({System.getProperty("testEnv") != "sit"})
	/** Verify OnSale Flag - verify Response */
	def "#rowNum Verify OnSale Flag || #testDesc"(){
		softAssert= new SoftAssert()
		println("Test Running ...$testDesc")
		Node node_search
		String responseBody
		boolean isHBGHotelAvailable = true
	
		def fileContents_Search = helper.getSearchOnSaleFlagRequestXml(SHEET_NAME_SEARCH,searchRowNum)
		println "Search REquest Xml.. \n${fileContents_Search}"
		node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
		responseBody = XmlUtil.serialize(node_search)
		logDebug("Search Response Xml..\n${responseBody}")
		if (responseBody.contains("ErrorId")){
			softAssert.assertTrue(false, "Failed : Received Error response")
		}else{
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
				
			 List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
			 if (hList.size() > 0){
				 softAssert.assertTrue(false, "Failed : Response should not return HBG hotels")
			 }
		}
		softAssert.assertAll()
		println "\n\n"
		
		where:
		rowNum 	| testDesc 					| searchRowNum
		24		|"On Sale Flag" 				| 24
	}
	
	
	def getHotelDTO(String searchType, String roomCode, String noOfCots, String noOfRooms){
		Node node_search
		String responseBody
		for(int k=0;k<cities.size();k++){
			def fileContents_Search = null
			if (searchType == null || searchType == ""){
				fileContents_Search = helper.getSearchXml(SHEET_NAME_SEARCH,1,cities[k], roomCode, noOfCots, noOfRooms)
			}else if(searchType=="DateChargeConditionTrue"){
				fileContents_Search = helper.getDateChargeConditionSearchXml(SHEET_NAME_SEARCH,1,cities[k], roomCode, noOfCots, noOfRooms, "true")
			}else if(searchType=="DateChargeConditionFalse"){
				fileContents_Search = helper.getDateChargeConditionSearchXml(SHEET_NAME_SEARCH,1,cities[k], roomCode, noOfCots, noOfRooms, "false")
			}else if(searchType=="DateChargeConditionEmpty"){
				fileContents_Search = helper.getDateChargeConditionEmptySearchXml(SHEET_NAME_SEARCH,1,cities[k], roomCode, noOfCots, noOfRooms)
			}else if (searchType == "PackageRates"){
				fileContents_Search = helper.getSearchXmlWithPackageRates(SHEET_NAME_SEARCH,1,cities[k], roomCode, noOfCots, noOfRooms)
			}
//			logDebug("Pre Search Request Xml.. \n${fileContents_Search}")
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
//			logDebug("Pre Search Response Xml..\n${responseBody}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
						
			if (hotelNode != null){
				List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				if (hList !=null && hList.size()>0){
					return hList.get(0)
				}
			}
		}
		return null
	}
	
	def getHotelDTOWithExtraBed(String roomCode, String noOfCots, String noOfRooms, int noOfExtraBeds){
		Node node_search
		String responseBody
		for(int k=0;k<cities.size();k++){
			def fileContents_Search = null
			if (noOfExtraBeds ==0 ){
				fileContents_Search = helper.getSearchXml(SHEET_NAME_SEARCH,1,cities[k], roomCode, noOfCots, noOfRooms)
			}else if (noOfExtraBeds == 1){
				fileContents_Search = helper.getSearchXmlWith1ExtraBed(SHEET_NAME_SEARCH,1,cities[k], roomCode, noOfCots, noOfRooms)
			}
//			logDebug("Pre Search Request Xml.. \n${fileContents_Search}")
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
//			logDebug("Pre Search Response Xml..\n${responseBody}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
						
			if (hotelNode != null){
				List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
				if (hList !=null && hList.size()>0){
					return hList.get(0)
				}
			}
		}
		return null
	}
	
	def HotelDTO getHotelDTOWithMultipleRoomsInSameCity(String roomCode, String noOfCots, String noOfRooms){
		Node node_search
		String responseBody
		for(int k=0;k<cities.size();k++){
			def fileContents_Search = null
			fileContents_Search = helper.getSearchXml(SHEET_NAME_SEARCH,1,cities[k], roomCode, noOfCots, noOfRooms)
//			logDebug("Pre Search Request Xml.. \n${fileContents_Search}")
			node_search = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents_Search,'search',FITSiteIdHeaderKey,SITEID)
			responseBody = XmlUtil.serialize(node_search)
//			logDebug("Pre Search Response Xml..\n${responseBody}")
			def hotelNode = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
			if (hotelNode != null){
				HotelDTO hDTO = helper.checkIfReponseHasMultipleRooms(hotelNode)
				if (hDTO != null)
					return hDTO
			}
		}
		return null
	}
}

class SearchHotelPriceTest_CitySearch_C1237 extends SearchHotelPriceTest_CitySearch {
	
	@Override
	String getClientId() {
		return "1237"
	}
}

class SearchHotelPriceTest_CitySearch_C1238 extends SearchHotelPriceTest_CitySearch {
	
	@Override
	String getClientId() {
		return "1238"
	}
}

class SearchHotelPriceTest_CitySearch_C1239 extends SearchHotelPriceTest_CitySearch {
	
	@Override
	String getClientId() {
		return "1239"
	}
}

class SearchHotelPriceTest_CitySearch_C1244 extends SearchHotelPriceTest_CitySearch {
	
	@Override
	String getClientId() {
		return "1244"
	}
}

@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class SearchHotelPriceTest_CitySearchSingleClient extends SearchHotelPriceTest_CitySearch {

    @Override
    String getClientId() {
        String clientId = System.getProperty("clientId")
        println "Using client Id $clientId"
        return clientId
    }
}
