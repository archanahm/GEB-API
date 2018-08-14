package com.hbg.helper.RequestHelper

import com.hbg.test.XSellBaseTest

import javax.net.ssl.SSLContext

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.ssl.SSLContexts
import org.apache.http.util.EntityUtils
import org.testng.asserts.SoftAssert

import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import com.kuoni.qa.automation.common.util.ExcelUtil

class RequestNewHelper {

	ExcelUtil excelUtil
	String sheetName
	Map dataMap
	Map validateMap= new HashMap()
	String ExpectedResult
	SoftAssert softAssert= new SoftAssert()
	XMLValidationUtil XmlValidate = new XMLValidationUtil()
	private static def xmlParser = new XmlParser()
	public String getExpectedResult() {
		return ExpectedResult;
	}

	/**
	 * Method for getting data from excel sheet to Map
	 * @return Map dataMap
	 */

	static String postRequestAsynchronously(String httpUrl, String contentType, String xmlBody, String headerName, String headerValue){
		Node node
		HashMap responseMap = getPostResponse(httpUrl, contentType, xmlBody, headerName,headerValue)
		HttpResponse response = (HttpResponse)responseMap.get("Response")
		String responseBody = (String)responseMap.get("ResponseBody")
		node = xmlParser.parseText(responseBody)

		println("-----------------------------------------------------------------------------\n")
		println("Request URL :  ${XSellBaseTest.url} \n")
		println("Response Reference : "+ node.@ResponseReference + "\n\n")
		println("-----------------------------------------------------------------------------\n")
		int statusCode = response.getStatusLine().getStatusCode()
		println "\n\nResponse Status code : ${statusCode}"
		sleep(1000)

		if(statusCode==202 || statusCode==200) {
			if(responseBody.contains("ErrorId")) {
				println "\n\nResponse XMl is ...\n$responseBody"
			}
			else
				responseBody = ""
		}
		else {
			println "\n\nResponse XMl is ...\n$responseBody"
		}
		return responseBody
	}

	static Node getResponses(def xmlName) {
		String url = "http://10.241.102.113/xml/${xmlName}-1.xml"
		System.out.println("Url of source File: " + url);
		def xml = new XmlSlurper().parse(url)

		String responseBody = groovy.xml.XmlUtil.serialize(xml)
		println "\n\nResponse XMl is ...\n$responseBody"
		Node node = xmlParser.parseText(responseBody)

		return node
	}

	static Node getResponses(String responseUrl, def xmlName) {
		String url =""
		if(responseUrl == "")
			url  = "http://10.241.102.113/xml/${xmlName}-1.xml"
		else
			url = responseUrl
		System.out.println("Url of source File: " + url);
		def xml = new XmlSlurper().parse(url)

		String responseBody = groovy.xml.XmlUtil.serialize(xml)
		println "\n\nResponse XMl is ...\n$responseBody"
		Node node = xmlParser.parseText(responseBody)

		return node
	}
	static def getPostResponse(String httpUrl, String contentType, String xmlBody, String headerName, String headerValue)  {

		String[] types = ["TLSv1.2", "TLSv1.1", "TLSv1"]
		SSLContext sslContext = SSLContexts.custom().build()
		SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(sslContext, types, null, new NoopHostnameVerifier())

		Node node

		def timeout = 10 * 60 * 1000
		RequestConfig requestTimeoutConfig = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
		//HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).setDefaultRequestConfig(requestTimeoutConfig).build();

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(f).setRedirectStrategy(new LaxRedirectStrategy()).setDefaultRequestConfig(requestTimeoutConfig).build();

		String responseBody
		HashMap map=new HashMap<HttpResponse,String>()

		RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(timeout)
				.setConnectTimeout(timeout)
				.setConnectionRequestTimeout(timeout)
				.build();

		// HttpClient httpClient = HttpClientBuilder.create().build()

		HttpResponse response
		try {
			HttpPost httpPost = getPostRequest(httpUrl, contentType, xmlBody, headerName, headerValue)
			httpPost.setConfig(requestConfig)
			response = httpClient.execute(httpPost)
			HttpEntity resEntity = response.getEntity()
			responseBody = EntityUtils.toString(resEntity)
			map.put("Response",response)
			map.put("ResponseBody",responseBody)
		}
		finally {
			httpClient.getConnectionManager().shutdown()
		}
		return map
	}


	static HttpPost getPostRequest(String httpUrl, String contentType, String xmlBody, String headerName, String headerValue) {
		def timeout = 10 * 60 * 1000
		RequestConfig requestTimeoutConfig = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
		def xmlParser = new XmlParser()
		def timeoOutMilliSecs = 5 * 1000
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(timeoOutMilliSecs)
				.setConnectTimeout(timeoOutMilliSecs).setSocketTimeout(timeoOutMilliSecs)
				.build()
		new HttpGet(httpUrl)
		HttpPost httpPost = new HttpPost(httpUrl)
		httpPost.setHeader("Content-Type", "")
		httpPost.setHeader(headerName, headerValue)
		httpPost.setConfig(defaultRequestConfig)
		HttpEntity reqEntity = new StringEntity(xmlBody, "UTF-8")
		reqEntity.setContentType(contentType)
		reqEntity.setChunked(true)
		httpPost.setEntity(reqEntity)
		return httpPost
	}

	static Node postHttpsXMLRequestWithHeaderNode(String httpsUrl,String contentType, String xmlBody, String testName, String headerName, String headerValue)
	{
		println "Request Endpoint URL : "+httpsUrl
		String[] types = ["TLSv1.2", "TLSv1.1", "TLSv1"]
		SSLContext sslContext = SSLContexts.custom().build()
		SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(sslContext, types, null, new NoopHostnameVerifier())

		Node node

		def timeout = 10 * 60 * 1000
		RequestConfig requestTimeoutConfig = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
		//HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).setDefaultRequestConfig(requestTimeoutConfig).build();

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(f).setRedirectStrategy(new LaxRedirectStrategy()).setDefaultRequestConfig(requestTimeoutConfig).build();

		HttpResponse response


		try {
			HttpPost httpPost = new HttpPost(httpsUrl)
			httpPost.setHeader("Content-Type", "")
			httpPost.setHeader(headerName, headerValue)
			httpPost.setConfig(requestTimeoutConfig)
			HttpEntity reqEntity = new StringEntity(xmlBody, "UTF-8")
			reqEntity.setContentType(contentType)
			reqEntity.setChunked(true)
			httpPost.setEntity(reqEntity)

			response = httpClient.execute(httpPost)
			HttpEntity resEntity = response.getEntity()
			String responseBody = EntityUtils.toString(resEntity, "UTF-8")
//			println "Response \n$responseBody"
			node = xmlParser.parseText(responseBody)
		}
		finally {
			httpClient.getConnectionManager().shutdown()
		}
		return node
	}


	static Map<String,String> getSearchDetails(Node node_search, Map<String,String> fields)
	{
		def flag=false
		def itemCode = ""
		def roomCategory = ""
		def itemCity = ""
		def itemCityName = ""
		def itemName = ""

		Map<String,String> itemMap = new HashMap<String,String>();
		def hotels = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				def roomCategories = hotels[i].RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(roomCategories[j].Confirmation.@Code.contains("IM")&& checkCondition(fields,roomCategories[j]))
					{
						itemCode =  hotels[i].Item.@Code[0]
						itemName = hotels[i].Item.text()
						itemCity = hotels[i].City.@Code[0]
						itemCityName = hotels[i].City.text()
						roomCategory = roomCategories[j].@Id
						flag = true
						break
					}

				}
				if(flag == true)
				{
					itemMap.put("itemCode",itemCode)
					itemMap.put("itemName",itemName)
					itemMap.put("itemCity",itemCity)
					itemMap.put("itemCityName",itemCityName)
					itemMap.put("roomCategory",roomCategory)
					break
				}
			}

		}
		if(flag==false)
			itemMap=null

		return itemMap

	}
	static Map<String,String> getSearchPaxDetails_GTA(Node node_search, Map<String,String> fields, itemCount=1)
	{
		def flag=false
		def itemCode = []
		def roomCategory = []
		def itemCity = []
		def itemCityName = []
		def itemName = []
		def hotelCount = 0

		Map<String,List<String>> itemMap = new HashMap<String,List<String>>();

		def hotels = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				def paxRoom = hotels[i].PaxRoomSearchResults.PaxRoom
				def roomCategories = paxRoom.RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(roomCategories[j].@Id.toString().startsWith("001") && roomCategories[j].Confirmation.@Code.contains("IM") && checkCondition(fields,roomCategories[j]))
					{
						itemCode.add(hotels[i].Item.@Code[0])
						itemName.add(hotels[i].Item.text())
						itemCity.add(hotels[i].City.@Code[0])
						itemCityName.add(hotels[i].City.text())
						roomCategory.add(roomCategories[j].@Id)

						itemMap.put("itemCode",itemCode)
						itemMap.put("itemName",itemName)
						itemMap.put("itemCity",itemCity)
						itemMap.put("itemCityName",itemCityName)
						itemMap.put("roomCategory",roomCategory)

						hotelCount = hotelCount+1
						break
					}
				}
				if(hotelCount ==itemCount){
					flag = true
					break
				}

			}
		}
		if(flag==false)
			itemMap=null

		return itemMap

	}


	static Map<String,String> getSearchPaxDetails_HBG(Node node_search, Map<String,String> fields, itemCount=1)
	{
		def flag=false
		def itemCode = []
		def roomCategory = []
		def itemCity = []
		def itemCityName = []
		def itemName = []
		def hotelCount = 0
		int hotelNumber =0

		Map<String,List<String>> itemMap = new HashMap<String,List<String>>();
		def hotels = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				hotelNumber = hotelNumber+1
				def paxRoom = hotels[i].PaxRoomSearchResults.PaxRoom
				def roomCategories = paxRoom.RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					
				if(!(roomCategories[j].@Id.toString().contains("ID_B2B")) && roomCategories[j].@Id.toString().startsWith("007") && roomCategories[j].Confirmation.@Code.contains("IM") &&  checkCondition(fields,roomCategories[j]))
					//if( roomCategories[j].Confirmation.@Code.contains("IM") &&  checkCondition(fields,roomCategories[j]))
					{
						itemCode.add(hotels[i].Item.@Code[0])
						itemName.add(hotels[i].Item.text())
						itemCity.add(hotels[i].City.@Code[0])
						itemCityName.add(hotels[i].City.text())
						roomCategory.add(roomCategories[j].@Id)

						itemMap.put("itemCode",itemCode)
						itemMap.put("itemName",itemName)
						itemMap.put("itemCity",itemCity)
						itemMap.put("itemCityName",itemCityName)
						itemMap.put("roomCategory",roomCategory)
						hotelCount = hotelCount+1
						break
//						if(hotelCount==itemCount){
//						   break
//						}
					}
				
					if(hotelCount==itemCount){
					   break
					}
				}
				
				
				if(hotelCount==itemCount ||( hotelNumber==hotels.size() && itemCode.size()!=0))
				{
					flag = true
					break
				}
			}
		}
		itemMap.put("hotelCount", hotelCount)
		if(flag==false) 
			itemMap=null

		return itemMap

	}

	static Map<String,String> getSearchDetailsGTA(Node node_search, Map<String,String> fields)
	{
		def flag=false
		def itemCode = ""
		def roomCategory = ""
		def itemCity = ""
		def itemCityName = ""
		def itemName = ""

		Map<String,String> itemMap = new HashMap<String,String>();
		def hotels = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				def roomCategories = hotels[i].RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(roomCategories[j].@Id.toString().startsWith("001") && roomCategories[j].Confirmation.@Code.contains("IM")&& checkCondition(fields,roomCategories[j]))
					{
						itemCode =  hotels[i].Item.@Code[0]
						itemName = hotels[i].Item.text()
						itemCity = hotels[i].City.@Code[0]
						itemCityName = hotels[i].City.text()
						roomCategory = roomCategories[j].@Id
						flag = true
						break
					}

				}
				if(flag == true)
				{
					itemMap.put("itemCode",itemCode)
					itemMap.put("itemName",itemName)
					itemMap.put("itemCity",itemCity)
					itemMap.put("itemCityName",itemCityName)
					itemMap.put("roomCategory",roomCategory)
					break
				}
			}

		}
		if(flag==false)
			itemMap=null

		return itemMap

	}

	static Map<String,String> getSearchDetailsHBG(Node node_search, Map<String,String> fields)
	{
		def flag=false
		def itemCode = ""
		def roomCategory = ""
		def itemCity = ""
		def itemCityName = ""
		def itemName = ""

		Map<String,String> itemMap = new HashMap<String,String>();
		def hotels = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				def roomCategories = hotels[i].RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(roomCategories[j].@Id.toString().startsWith("007") && roomCategories[j].Confirmation.@Code.contains("IM")&& checkCondition(fields,roomCategories[j]))
					{
						itemCode =  hotels[i].Item.@Code[0]
						itemName = hotels[i].Item.text()
						itemCity = hotels[i].City.@Code[0]
						itemCityName = hotels[i].City.text()
						roomCategory = roomCategories[j].@Id
						flag = true
						break
					}

				}
				if(flag == true)
				{
					itemMap.put("itemCode",itemCode)
					itemMap.put("itemName",itemName)
					itemMap.put("itemCity",itemCity)
					itemMap.put("itemCityName",itemCityName)
					itemMap.put("roomCategory",roomCategory)
					break
				}
			}

		}
		if(flag==false)
			itemMap=null

		return itemMap

	}

	static Map<String,String> getSearchDetailsHBGWithCount(Node node_search, Map<String,String> fields,itemCount=1)
	{
		def flag=false
		def itemCode = []
		def roomCategory = []
		def itemCity = []
		def itemCityName = []
		def itemName = []
		def hotelCount = 0

		Map<String,List<String>> itemMap = new HashMap<String,List<String>>();
		def hotels = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				def roomCategories = hotels[i].RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(roomCategories[j].@Id.toString().startsWith("007") && roomCategories[j].Confirmation.@Code.contains("IM")&& checkCondition(fields,roomCategories[j]))
					{
						itemCode.add(hotels[i].Item.@Code[0])
						itemName.add(hotels[i].Item.text())
						itemCity.add(hotels[i].City.@Code[0])
						itemCityName.add(hotels[i].City.text())
						roomCategory.add(roomCategories[j].@Id)

						itemMap.put("itemCode",itemCode)
						itemMap.put("itemName",itemName)
						itemMap.put("itemCity",itemCity)
						itemMap.put("itemCityName",itemCityName)
						itemMap.put("roomCategory",roomCategory)
						hotelCount = hotelCount+1
						break

					}
				}
				if(hotelCount ==itemCount){
					flag = true
					break
				}
			}
	}
		if(flag==false)
			itemMap=null

		return itemMap

	}
	
	static boolean isMoreThanOneCheapestHotel(Node node_search)
	{
		def flag=false
		def itemCode = []
		def roomCategory = []
		def itemCity = []
		def itemCityName = []
		def itemName = []
		def hotelCount = 0

		Map<String,List<String>> itemMap = new HashMap<String,List<String>>();
		def hotels = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			return false
		else
		{
			for(i in 0.. hotels.size()-1)
			{
				def gtaHotelCount = 0
				def hbgHotelCount = 0
				def roomCategories = hotels[i].RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					println hotels[i].Item.@Code[0] + ":" + roomCategories[j].@Id.toString()
					if(roomCategories[j].@Id.toString().startsWith("007") )
					{
						hbgHotelCount = hbgHotelCount + 1
					}else if(roomCategories[j].@Id.toString().startsWith("001") ) {
						gtaHotelCount = gtaHotelCount + 1
					}
				}
				if(hbgHotelCount > 1 || gtaHotelCount > 1) {
					return false
				}
			}
	}
		return true

	}
	
	static boolean isMoreThanOneCheapestHotelPax(Node node_search)
	{
		def flag=false
		def itemCode = []
		def roomCategory = []
		def itemCity = []
		def itemCityName = []
		def itemName = []
		def hotelCount = 0

		Map<String,List<String>> itemMap = new HashMap<String,List<String>>();
		def hotels = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			return false
		else
		{
			for(i in 0.. hotels.size()-1)
			{
				def gtaHotelCount = 0
				def hbgHotelCount = 0
				def roomCategories = hotels[i].RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					println hotels[i].Item.@Code[0] + ":" + roomCategories[j].@Id.toString()
					if(roomCategories[j].@Id.toString().startsWith("007") )
					{
						hbgHotelCount = hbgHotelCount + 1
					}else if(roomCategories[j].@Id.toString().startsWith("001") ) {
						gtaHotelCount = gtaHotelCount + 1
					}
				}
				if(hbgHotelCount > 1 || gtaHotelCount > 1) {
					return false
				}
			}
	}
		return true

	}

	static boolean checkCondition(Map<String,String> fields,Node roomCategory)
	{
		boolean offer = false
		boolean offerFlag = false
		boolean sharedBedding = false
		boolean sharedBeddingFlag = false

		boolean result = false

		if(fields==null)
		{
			return true
		}
		if(fields.keySet().contains("Offer"))
		{
			offerFlag = true;
			if(roomCategory.Offer.@Code.contains(fields.get("Offer").toString()))
			{
				offer=true
			}
		}

		else if(fields.keySet().contains("SharedBedding"))
		{
			sharedBeddingFlag = true;
			if(roomCategory.SharingBedding.text().contains(fields.get("SharedBedding").toString()))
			{
				sharedBedding=true
			}
		}

		if(offerFlag==true && offer==true)
		{
			result = true
		}

		else if(sharedBeddingFlag==true && sharedBedding==true)
		{
			result = true
		}

		return result

	}
	static def retry(int times = 12, Closure body) {
		int retries = 0
		while(retries++ < times) {
			try {
				sleep(10000)
				println("Retry: ${retries}")
				return body.call()

			}
			catch(Exception e) {
				//do nothing
			}
		}

		throw new GroovyRuntimeException("Failed to get response after ${retries-1} Retries")
	}
	static Map<String,String> getSearchPaxDetails_HBG_SHPPR(Node node_search, Map<String,String> fields, itemCount=1)
	{
		def flag=false
		def itemCode = []
		def roomCategory = []
		def itemCity = []
		def itemCityName = []
		def itemName = []
		def hotelCount = 0
		int hotelNumber =0

		Map<String,List<String>> itemMap = new HashMap<String,List<String>>();
		def hotels = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				hotelNumber = hotelNumber+1
				def paxRoom = hotels[i].PaxRoomSearchResults.PaxRoom
				def roomCategories = paxRoom.RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					
					if(!(roomCategories[j].@Id.toString().contains("ID_B2B")) && roomCategories[j].@Id.toString().startsWith("007") && roomCategories[j].Confirmation.@Code.contains("IM") &&  checkCondition(fields,roomCategories[j]))
					{
						itemCode.add(hotels[i].Item.@Code[0])
						itemName.add(hotels[i].Item.text())
						itemCity.add(hotels[i].City.@Code[0])
						itemCityName.add(hotels[i].City.text())
						roomCategory.add(roomCategories[j].@Id)

						itemMap.put("itemCode",itemCode)
						itemMap.put("itemName",itemName)
						itemMap.put("itemCity",itemCity)
						itemMap.put("itemCityName",itemCityName)
						itemMap.put("roomCategory",roomCategory)
						hotelCount = hotelCount+1
						break
//						if(hotelCount==itemCount){
//						   break
//						}
					}
				
					if(hotelCount==itemCount){
					   break
					}
				}
				
				
				if(hotelCount==itemCount ||( hotelNumber==hotels.size() && itemCode.size()!=0))
				{
					flag = true
					break
				}
			}
		}
		//itemMap.put("hotelCount", hotelCount)
		if(flag==false)
			itemMap=null

		return itemMap

	}

	
	static List<HotelDTO> getSearchDetails_HBG(def hotels, itemCount=1)
	{
		List<HotelDTO> hList = new ArrayList<HotelDTO>()
		if(hotels != null && hotels.size() > 0){
			for(i in 0.. hotels.size()-1){
				def rCategory = hotels[i].RoomCategories.RoomCategory
				for (j in 0..rCategory.size()-1){
					if(!(rCategory[j].@Id.toString().contains("ID_B2B")) && rCategory[j].@Id.toString().startsWith("007") && rCategory[j].Confirmation.@Code.contains("IM")){
						HotelDTO hDTO = new HotelDTO()
						hDTO.setItemCode(hotels[i].Item.@Code[0])
						hDTO.setItemName(hotels[i].Item.text())
						hDTO.setItemCity(hotels[i].City.@Code[0])
						hDTO.setItemCityName(hotels[i].City.text())
						hDTO.setCategoryId(rCategory[j].@Id)
						hDTO.setHotelNode(hotels[i])
						hDTO.setRoomCategoryNode(rCategory[j])
						hDTO.setRoomDescription(rCategory[j].Description.text())
						
						hDTO.setStarRating(hotels[i].StarRating.text())
						
						hDTO.setRoomConfirmationCode(rCategory[j].Confirmation.@Code[0])
						hDTO.setRoomConfirmationText(rCategory[j].Confirmation.text())
						
						if (rCategory[j].Meals.Basis.@Code != null){
							hDTO.setMealBasisCode(rCategory[j].Meals.Basis.@Code[0])
							hDTO.setMealBasisText(rCategory[j].Meals.Basis.text())
						}
						hDTO.setSharingBedding(rCategory[j].SharingBedding.toString())
						
						hList.add(hDTO)
						
						if(itemCount > 0 && hList.size()==itemCount){
						   break
						}
					}
				}
				if(itemCount > 0 && hList.size()==itemCount){
				   break
				}
			}
		}
		return hList
	}
	
	static List<HotelDTO> getSearchAmandableHBGHotels(def hotels, itemCount=1)
	{
		List<HotelDTO> hList = new ArrayList<HotelDTO>()
		if(hotels != null && hotels.size() > 0){
			for(i in 0.. hotels.size()-1){
				def rCategory = hotels[i].RoomCategories.RoomCategory
				for (j in 0..rCategory.size()-1){
					if(!(rCategory[j].@Id.toString().contains("ID_B2B")) && rCategory[j].@Id.toString().startsWith("007") && rCategory[j].Confirmation.@Code.contains("IM")){
						boolean isAmandable = true
						def chargeCondition = rCategory[j].ChargeConditions.ChargeCondition
						for (int l=0;l<chargeCondition.size();l++){
							def type = chargeCondition[l].@Type[0]
							if (type == "amendment"){
								def condition = chargeCondition[l].Condition
								for (int m=0; m<condition.size(); m++){
									if (condition[m].@Allowable != null && condition.@Allowable.toString()=="false"){
										isAmandable = false
										println ">>> "+rCategory[j].@Id
										break
									}
								}
								if (!isAmandable){
									break
								}
							}
						}
						
						if (!isAmandable){
							continue
						}
						
						HotelDTO hDTO = new HotelDTO()
						hDTO.setItemCode(hotels[i].Item.@Code[0])
						hDTO.setItemName(hotels[i].Item.text())
						hDTO.setItemCity(hotels[i].City.@Code[0])
						hDTO.setItemCityName(hotels[i].City.text())
						hDTO.setCategoryId(rCategory[j].@Id)
						hDTO.setHotelNode(hotels[i])
						hDTO.setRoomCategoryNode(rCategory[j])
						hDTO.setRoomDescription(rCategory[j].Description.text())
						
						hDTO.setStarRating(hotels[i].StarRating.text())
						
						hDTO.setRoomConfirmationCode(rCategory[j].Confirmation.@Code[0])
						hDTO.setRoomConfirmationText(rCategory[j].Confirmation.text())
						
						if (rCategory[j].Meals.Basis.@Code != null){
							hDTO.setMealBasisCode(rCategory[j].Meals.Basis.@Code[0])
							hDTO.setMealBasisText(rCategory[j].Meals.Basis.text())
						}
						hDTO.setSharingBedding(rCategory[j].SharingBedding.toString())
						
						hList.add(hDTO)
						
						if(itemCount > 0 && hList.size()==itemCount){
						   break
						}
					}
				}
				if(itemCount > 0 && hList.size()==itemCount){
				   break
				}
			}
		}
		return hList
	}
	
	static List<HotelDTO> getSearchDetails_GTA(def hotels, itemCount=1)
	{
		List<HotelDTO> hList = new ArrayList<HotelDTO>()
		if(hotels != null && hotels.size() > 0){
			for(i in 0.. hotels.size()-1){
				def rCategory = hotels[i].RoomCategories.RoomCategory
				for (j in 0..rCategory.size()-1){
					if(rCategory[j].@Id.toString().startsWith("001") && rCategory[j].Confirmation.@Code.contains("IM")){
						HotelDTO hDTO = new HotelDTO()
						hDTO.setItemCode(hotels[i].Item.@Code[0])
						hDTO.setItemName(hotels[i].Item.text())
						hDTO.setItemCity(hotels[i].City.@Code[0])
						hDTO.setItemCityName(hotels[i].City.text())
						hDTO.setCategoryId(rCategory[j].@Id)
						hDTO.setHotelNode(hotels[i])
						hDTO.setRoomCategoryNode(rCategory[j])
						hDTO.setRoomDescription(rCategory[j].Description.text())
						hDTO.setStarRating(hotels[i].StarRating.text())
						
						hDTO.setRoomConfirmationCode(rCategory[j].Confirmation.@Code[0])
						hDTO.setRoomConfirmationText(rCategory[j].Confirmation.text())
						
						if (rCategory[j].Meals.Basis.@Code != null){
							hDTO.setMealBasisCode(rCategory[j].Meals.Basis.@Code[0])
							hDTO.setMealBasisText(rCategory[j].Meals.Basis.text())
						}
						hDTO.setSharingBedding(rCategory[j].SharingBedding.toString())
						hList.add(hDTO)
						
						if(itemCount > 0 && hList.size()==itemCount){
						   break
						}
					}
				}
				if(itemCount > 0 && hList.size()==itemCount){
				   break
				}
			}
		}
		return hList
	}

	static List<HotelDTO> getSearchPaxDetails_HBG(def hotels, itemCount=1)
	{
		List<HotelDTO> hList = new ArrayList<HotelDTO>()
		if(hotels == null || hotels.size() == 0){
			return hList
		} else {
			for(i in 0.. hotels.size()-1){
				def paxRoom = hotels[i].PaxRoomSearchResults.PaxRoom

				def roomCategories = paxRoom.RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(!(roomCategories[j].@Id.toString().contains("ID_B2B")) && roomCategories[j].@Id.toString().startsWith("007") && roomCategories[j].Confirmation.@Code.contains("IM")){
						HotelDTO hDTO = new HotelDTO()
						hDTO.setItemCode(hotels[i].Item.@Code[0])
						hDTO.setItemName(hotels[i].Item.text())
						hDTO.setItemCity(hotels[i].City.@Code[0])
						hDTO.setRoomCategoryNode(roomCategories[j])
						hDTO.setItemCityName(hotels[i].City.text())
						hDTO.setCategoryId(roomCategories[j].@Id)
						hDTO.setHotelNode(hotels[i])

						hList.add(hDTO)

						if(itemCount > 0 && hList.size()==itemCount){
							break
						}
					}
				}
				if(itemCount > 0 && hList.size()==itemCount){
					break
				}
			}
		}
		return hList
	}

	static List<HotelDTO> getSearchPaxDetails_GTA(def hotels, itemCount=1)
	{
		List<HotelDTO> hList = new ArrayList<HotelDTO>()
		if(hotels == null || hotels.size() == 0){
			return hList
		} else {
			for(i in 0.. hotels.size()-1){
				def paxRoom = hotels[i].PaxRoomSearchResults.PaxRoom
				def roomCategories = paxRoom.RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(roomCategories[j].@Id.toString().startsWith("001") && roomCategories[j].Confirmation.@Code.contains("IM")){
						HotelDTO hDTO = new HotelDTO()
						hDTO.setItemCode(hotels[i].Item.@Code[0])
						hDTO.setItemName(hotels[i].Item.text())
						hDTO.setItemCity(hotels[i].City.@Code[0])
						hDTO.setRoomCategoryNode(roomCategories[j])
						hDTO.setItemCityName(hotels[i].City.text())
						hDTO.setCategoryId(roomCategories[j].@Id)
						hDTO.setHotelNode(hotels[i])

						hList.add(hDTO)

						if(itemCount > 0 && hList.size()==itemCount){
							break
						}
					}
				}
				if(itemCount > 0 && hList.size()==itemCount){
					break
				}
			}
		}
		return hList
	}
}
