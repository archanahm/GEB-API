package com.hbg.helper.SearchHotelPricePaxHelper

import com.hbg.test.XSellBaseTest

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.util.EntityUtils
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.testng.asserts.SoftAssert

import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import com.kuoni.qa.automation.common.util.ExcelUtil

class SearchHotelPriceHelper {
	ExcelUtil excelUtil
	String sheetName
	Map dataMap
	Map validateMap= new HashMap()
	String ExpectedResult
	SoftAssert softAssert= new SoftAssert()
	XMLValidationUtil XmlValidate = new XMLValidationUtil()
	private static def xmlParser = new XmlParser()
	static final String URL= ConfigProperties.getValue("AuthenticationURL")
	static final String checkInDaysToAdd = ConfigProperties.getValue("HBGCheckInDaysToAdd")
	static final def timeout = 10 * 60 * 1000
	static RequestConfig requestTimeoutConfig = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
	static final def timeoOutMilliSecs = 5 * 1000
	public String clientId
	SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")


	public String getExpectedResult() {
		return ExpectedResult;
	}

    SearchHotelPriceHelper(String excelPath, String clientId){
		excelUtil=new ExcelUtil(excelPath)
		this.clientId = clientId
		println("-----------------------------------------------------------------------------\n")
		println("Request URL :  ${XSellBaseTest.url} \n")
		println("-----------------------------------------------------------------------------\n")
		
	}
	

	
	static String postHttpXmlRequestWithHeaderNode(String httpUrl, String contentType, String xmlBody, String testName, String headerName, String headerValue) {
		Node node
	   int timeout = 10_000
	   String responseBody=""
	   RequestConfig requestConfig = RequestConfig.custom()
			   .setSocketTimeout(timeout)
			   .setConnectTimeout(timeout)
			   .setConnectionRequestTimeout(timeout)
			   .build();

	   HttpClient httpClient = HttpClientBuilder.create().build()
//        httpClient

	   HttpResponse response
	   try {
		   HttpPost httpPost = getPostRequest(httpUrl, contentType, xmlBody, headerName, headerValue)
		   httpPost.setConfig(requestConfig)
		   response = httpClient.execute(httpPost)
		   HttpEntity resEntity = response.getEntity()
		   responseBody = EntityUtils.toString(resEntity, "UTF-8")
	   }
		  // println "Response \n$responseBody"
		  // node = xmlParser.parseText(responseBody)
	   
	   finally {
		   httpClient.getConnectionManager().shutdown()
	   }
	   return responseBody
   }

	static HttpPost getPostRequest(String httpUrl, String contentType, String xmlBody, String headerName, String headerValue) {
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
	/**Method to post https request with header*/
	def postHttpsXMLRequestWithHeaderNode(String httpsUrl,String contentType, String xmlBody, String testName, String headerName, String headerValue)
	{

	Node node
	
	   def timeout = 10 * 60 * 1000
	   RequestConfig requestTimeoutConfig = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
	   HttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).setDefaultRequestConfig(requestTimeoutConfig).build();
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
		   String responseBody = EntityUtils.toString(resEntity)
		   println "Response \n$responseBody"
		   node = xmlParser.parseText(responseBody)
	   }
	   finally {
		   httpClient.getConnectionManager().shutdown()
	   }
	   return node
}

	/**
	 * Method for getting data from excel sheet to Map
	 * @return Map dataMap
	 */
	public def getSheetDataAsMap(String sheetName, int rowNum){
		this.sheetName=sheetName
		dataMap = new HashMap<String, String>()
		Sheet sheet= excelUtil.getSheet(sheetName)
		if (!sheet){
			return null;
		}
		Row headerRow=sheet.getRow(0)
		Row row1=sheet.getRow(rowNum)
		short minColIx = headerRow.getFirstCellNum();
		short maxColIx = headerRow.getLastCellNum()-1;
		for(int i in minColIx..maxColIx ){
			Cell key=headerRow.getCell(i)
			Cell value=row1.getCell(i)
			if(key && value){
				dataMap.put(key.getStringCellValue(),value.getStringCellValue())
			}
		}
		setExpectedResult(dataMap.get('ExpectedResult'))
		return dataMap;
	}

	public def validateSuccessResponse(String ResponseBody, String expectedValues){

		StringTokenizer st;
		int count = 0;
		String[] expectedName;
		String[] expectedValue;

		st = new StringTokenizer(dataMap.get('ExpectedName'), '|');
		expectedName = new String[st.countTokens()];
		while (st.hasMoreElements()) {
			expectedName[count] = st.nextToken();
			++count;
		}

		st = new StringTokenizer(expectedValues, '|');
		expectedValue = new String[st.countTokens()];
		count = 0;
		while (st.hasMoreElements()) {
			expectedValue[count] = st.nextToken();
			++count;
		}
		println(expectedName)
		assert expectedName.length==expectedValue.length,"Expected name and expected value are not matching"

		Map validateAttrMultiValue =  new HashMap<String,List>()
		Map validateAttrValue =  new HashMap<String, String>()
		Map validateTagMultiValueMap =  new HashMap<String,List>()
		Map validateTagValueMap =  new HashMap<String,String>()
		Map validateTagValueRangeMap =  new HashMap<String,String>()
		Map validateSingleAttrValue =  new HashMap<String, String>()

		List<String> li;

		for (int i = 0; i < expectedValue.length; i++) {

			if (expectedName[i].contains(",") && expectedValue[i].contains(",")) {
				// Attribute with Multiple value

				li = new ArrayList<String>();
				st = new StringTokenizer(expectedValue[i], ",");
				while (st.hasMoreElements()) {
					li.add(st.nextToken());
				}
				validateAttrMultiValue.put(expectedName[i], li);

			} else if (expectedName[i].contains(",") && !(expectedValue[i].contains(",")) && !(expectedName[i].contains("["))) {
				// Attribute with single value
				validateAttrValue.put(expectedName[i], expectedValue[i]);

			} else if (!(expectedName[i].contains(",")) && expectedValue[i].contains(",")) {
				// Tag with Multiple value

				li = new ArrayList<String>();
				st = new StringTokenizer(expectedValue[i], ",");
				while (st.hasMoreElements()) {
					li.add(st.nextToken());
				}
				validateTagMultiValueMap.put(expectedName[i], li);

			} else if (!(expectedName[i].contains(",")) && !(expectedValue[i].contains(","))) {

				// Tag with Range of values
				if(expectedValue[i].contains("-")){
					validateTagValueRangeMap.put(expectedName[i], expectedValue[i]);
				}
				// Tag with Single value
				else{
					validateTagValueMap.put(expectedName[i], expectedValue[i]);
				}
			}
			else if (expectedName[i].contains(",") && !(expectedValue[i].contains(",")) && expectedName[i].contains("[")) {
				// Attribute with single value and searching for at least one time

				expectedName[i] = expectedName[i].replace("[", "")
				expectedName[i] = expectedName[i].replace("]", "")
				expectedValue[i] = expectedValue[i].replace("[", "")
				expectedValue[i] = expectedValue[i].replace("]", "")
				validateSingleAttrValue.put(expectedName[i], expectedValue[i]);

			}
		}
		if(!validateAttrMultiValue.isEmpty()){
			XmlValidate.validateAttrMultiValue(validateAttrMultiValue, ResponseBody)
		}

		if(!validateAttrValue.isEmpty()){
			XmlValidate.validateAttrValue(validateAttrValue, ResponseBody)
		}
		if(!validateTagMultiValueMap.isEmpty()){
			XmlValidate.validateTagMultiValue(validateTagMultiValueMap, ResponseBody)
		}
		if(!validateTagValueMap.isEmpty()){
			XmlValidate.validateTagValue(validateTagValueMap, ResponseBody)
		}
		if(!validateTagValueRangeMap.isEmpty()){
			XmlValidate.validateTagRangeValue(validateTagValueRangeMap, ResponseBody)
		}
		if(!validateSingleAttrValue.isEmpty()){
			XmlValidate.validateSingleAttrValue(validateSingleAttrValue, ResponseBody)
		}
	}



	public def validateFailureResponse(String ResponseBody){
		validateXmlWithExpectedElementValuesPair(dataMap.get('ExpectedName'), dataMap.get('ExpectedValue'), ResponseBody)
	}
	public def validateXmlWithEmptyResponse(String xml){
		String elementName =dataMap.get('ExpectedName')
		assert xml.toLowerCase().contains("<$elementName/>".toLowerCase()),"Response Body does not have the element "+ elementName
	}

	public def validateXmlWithExpectedElementValuesPair(String elementName, String elementValue, String xml){
		assert xml.toLowerCase().contains("<$elementName>$elementValue</$elementName>".toLowerCase()), "Response Body does not have the element tag "+ elementName +" with value "+ elementValue
	}

	public def traverseResponseXml(Node node, String testDesc){

		println "\n\nTest Runnning : ${testDesc}"
		def isErrorNode = node.depthFirst().findAll{ it.name() == "Errors" }
		def isEmptyNode = node.depthFirst().findAll{ it.name() == "Hotel" }
		def isResponseNode = node.depthFirst().findAll{ it.name() == "ResponseDetails" }
		if(!isEmptyNode && !isErrorNode){
			if(isResponseNode){
				println "\nResponseReference : ${node.@ResponseReference}"
			}
			println "\nEmpty Response ... "
		}else if(isErrorNode){
			println "\nError Response ... "
			if(isResponseNode){
				def errorNode
				// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
				println "\nResponseReference : ${node.@ResponseReference}"
				def isBookingResponse = node.depthFirst().findAll{ it.name() == "BookingResponse" }
				def isSearchHotelPriceResponse = node.depthFirst().findAll{ it.name() == "SearchHotelPricePaxResponse" }
				if(isBookingResponse){
					errorNode=node.ResponseDetails.BookingResponse.Errors.Error
				}else if(isSearchHotelPriceResponse){
					errorNode=node.ResponseDetails.SearchHotelPriceResponse.Errors.Error
				}else{
					errorNode=node.ResponseDetails.Errors.Error
				}
				println "ErrorId : ${errorNode.ErrorId.text()} \nErrorText : ${errorNode.ErrorText.text()}"
			}else{
				println "ErrorId : ${node.Errors.Error.ErrorId.text()} \nErrorText : ${node.Errors.Error.ErrorText.text()}"
			}
		}else{
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"

			def hotelDetails=node.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails
			def allHotel= hotelDetails.Hotel.size()
			def isRoomPrice = node.depthFirst().findAll{ it.name() == "RoomPrice" }
			for(int i in 0 .. (allHotel-1)){

				String cityCode=hotelDetails.Hotel[i].City.@Code
				softAssert.assertTrue(cityCode==dataMap.get('ExpectedValues'))

				println "Hotel City Code : ${hotelDetails.Hotel[i].City.@Code}\nHotel City Name : ${hotelDetails.Hotel[i].City.text()}"
				println "Hotel Item Code : ${hotelDetails.Hotel[i].Item.@Code}\nHotel Item Name : ${hotelDetails.Hotel[i].Item.text()}"
				println "Hotel Star Rating : ${hotelDetails.Hotel[i].StarRating.text()}\nHotel Location Code : ${hotelDetails.Hotel[i].LocationDetails.Location.@Code}"
				if(isRoomPrice){
					def allRoomCategory=hotelDetails.Hotel[i].RoomCategories.RoomCategory.size()
					for(int j in 0 .. (allRoomCategory-1)){
						println "Room Discription : ${hotelDetails.Hotel[i].RoomCategories.RoomCategory[j].Description.text()}"
						println "Room Price : ${hotelDetails.Hotel[i].RoomCategories.RoomCategory[j].HotelRoomPrices.HotelRoom.RoomPrice.@Gross}"
					}
					println "--------------------------------"
				}
			}
		}
	}
	public def traverseDayFormateResponseXml(Node node,String testDesc){

		println "\n\nTest Runnning : ${testDesc}"
		def isResponseNode = node.depthFirst().findAll{ it.name() == "ResponseDetails" }
		if(isResponseNode){
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\nResponseReference : ${node.@ResponseReference}"
		}
		Iterator ittr = node.depthFirst().iterator()
		while(ittr.hasNext()){
			Node node1=ittr.next()
			if(node1.name().equals("Item")){
				println"Hotel Code : ${node1.@Code} \nHotel Name : ${node1.text()}"
			}
			if(node1.name().equals("RoomCategory")){
				println"RoomCategory Id : ${node1.@Id} \nRoomCategory Description : ${node1.Description.text()}"
			}
			if(node1.name().equals("ChargeCondition")){
				println"Type : ${node1.@Type}"
				Iterator ittr1 = node1.children().iterator()
				while(ittr1.hasNext()){
					Node node2=ittr1.next()
					if(node2.@Charge.equals("true")){
						println "Charge Condition : ${node2.@Charge} \tCharge Amount : ${node2.@ChargeAmount} \tCurrency : ${node2.@Currency} \tFrom Day : ${node2.@FromDay} \tTo Day : ${node2.@ToDay}"
					}
					else{
						println "Charge Condition : ${node2.@Charge} \tFrom Day : ${node2.@FromDay}"
					}
				}
			}
		}
	}
	public def traverseDateFormateResponseXml(Node node, String testDesc){

		println "\n\nTest Runnning : ${testDesc}"
		def isResponseNode = node.depthFirst().findAll{ it.name() == "ResponseDetails" }
		if(isResponseNode){
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\nResponseReference : ${node.@ResponseReference}"
		}
		Iterator ittr = node.depthFirst().iterator()
		while(ittr.hasNext()){
			Node node1=ittr.next()
			if(node1.name().equals("Item")){
				println"Hotel Code : ${node1.@Code} \nHotel Name : ${node1.text()}"
			}
			if(node1.name().equals("RoomCategory")){
				println"RoomCategory Id : ${node1.@Id} \nRoomCategory Description : ${node1.Description.text()}"
			}
			if(node1.name().equals("ChargeCondition")){
				println"Type : ${node1.@Type}"
				Iterator ittr1 = node1.children().iterator()
				while(ittr1.hasNext()){
					Node node2=ittr1.next()
					if(node2.@Charge.equals("true")){
						println "Charge Condition : ${node2.@Charge} \tCharge Amount : ${node2.@ChargeAmount} \tCurrency : ${node2.@Currency} \tFrom Date : ${node2.@FromDate} \tTo Date : ${node2.@ToDate}"
					}
					else{
						println "Charge Condition : ${node2.@Charge} \tFrom Date : ${node2.@FromDate}"
					}
				}
			}
		}
	}

	public def traverseOfferResponseXml(Node node, String testDesc){

		println "\n\nTest Runnning : ${testDesc}"
		def isResponseNode = node.depthFirst().findAll{ it.name() == "ResponseDetails" }
		if(isResponseNode){
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\nResponseReference : ${node.@ResponseReference}"
		}
		Iterator<Node> itr= node.depthFirst().iterator()
		while(itr.hasNext()){
			boolean flag=false
			Node node1= itr.next()
			if(node1.name().equals("Hotel")){
				println "Hotel Code : ${node1.Item.@Code}\nHotel Name : ${node1.Item.text()}"
			}
			if(node1.name().equals("RoomCategories")){
				//Node node3=node1.children().get(0)
				Iterator<Node> ittr = node1.depthFirst().iterator()
				while(ittr.hasNext()){
					Node node2=ittr.next()
					if(node2.name().equals("Offer")){
						flag=true
						println "Offer Code : ${node2.@Code} \nOffer Description : ${node2.text()}\n"
						break;
					}
				}
				if(!flag){
					println "No Offer associated with this Hotel\n"
				}
			}
		}
	}

	public def traverseDIResponseXml(Node node , String testDesc){

		println "\n\nTest Runnning : ${testDesc}"
		def isResponseNode = node.depthFirst().findAll{ it.name() == "ResponseDetails" }
		if(isResponseNode){
			// // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
			println "\nResponseReference : ${node.@ResponseReference}"
		}
		def isEmptyNode = node.depthFirst().findAll{ it.name() == "Hotel" }
		if(!isEmptyNode){
			println "\nEmpty Response ... "
		}
		Iterator<Node> itr= node.depthFirst().iterator()
		while(itr.hasNext()){
			Node node1=itr.next()
			if(node1.name().equals("Item")){
				println"\nHotel Code : ${node1.@Code} \nHotel Name : ${node1.text()}"
			}
			if(node1.name().equals("RoomCategory")){
				println"RoomCategory Id : ${node1.@Id} \nRoomCategory Description : ${node1.Description.text()}"
				if(node1.@Id.contains("CAESARS") || node1.@Id.contains("BONOTEL")){
					println " NO Offer Discount == true"
				}
			}
		}
	}
	
	static boolean getEssentialInformationDetailsHBG(Node node_search)
	{
		def flag=true

		Map<String,String> itemMap = new HashMap<String,String>();
		def hotels = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				println "Item code: "+hotels[i].Item.@Code[0] + ": City Code: "+hotels[i].City.@Code[0]

				
				def roomCategories = hotels[i].RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(roomCategories[j].@Id.toString().startsWith("007")) {
						println "Room Category Id: "+roomCategories[j].@Id
						def informations = roomCategories[j].EssentialInformation.Information
						println "Information data:"+informations
						
						if(informations != null && informations.size() > 0) {
							for(k in 0..informations.size()-1) {
								if(informations[k].Text.text().toLowerCase().contains("complimentary") || informations[k].Text.text().toLowerCase().contains("discount")) {
									flag = false
									println "Invalid Essential Information:"+informations[k].Text.text()
								}
							}
						}
						
					}
					

				}

				
			}

		}
		return flag
	}
	
	static boolean getPaxEssentialInformationDetailsHBG(Node node_search)
	{
		def flag=true

		Map<String,String> itemMap = new HashMap<String,String>();
		def hotels = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				println "Item code: "+hotels[i].Item.@Code[0] + ": City Code: "+hotels[i].City.@Code[0]

				
				def roomCategories = hotels[i].RoomCategories.RoomCategory
				for (j in 0..roomCategories.size()-1)
				{
					if(roomCategories[j].@Id.toString().startsWith("007")) {
						println "Room Category Id: "+roomCategories[j].@Id
						def informations = roomCategories[j].EssentialInformation.Information
						println "Information data:"+informations
						
						if(informations != null && informations.size() > 0) {
							for(k in 0..informations.size()-1) {
								if(informations[k].Text.text().toLowerCase().contains("complimentary") || informations[k].Text.text().toLowerCase().contains("discount")) {
									flag = false
									println "Invalid Essential Information:"+informations[k].Text.text()
								}
							}
						}
						
					}
					

				}

				
			}

		}
		return flag
	}
	
	static boolean getChargeConditionDetailsHBG(Node node_search, String itemCode, boolean isDateFormatResponseAvailable, String responseDateOrDay)
	{
		def flag=false

		Map<String,String> itemMap = new HashMap<String,String>();
		def hotels = node_search.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				println "Item code"+hotels[i].Item.@Code[0]
				if(hotels[i].Item.@Code[0].contentEquals(itemCode)) {
					
					def roomCategories = hotels[i].RoomCategories.RoomCategory
					for (j in 0..roomCategories.size()-1)
					{
						def chargeConditions = roomCategories[j].ChargeConditions.ChargeCondition
						for(k in 0..chargeConditions.size()-1) {
							def conditions = chargeConditions.Condition
							for(l in 0..conditions.size()-1) {
								if(responseDateOrDay.contentEquals("date") && conditions[l].@FromDate == null) {
									return false
								}
								if(responseDateOrDay.contentEquals("day") && conditions[l].@FromDay == null) {
									return false
								}
							}
							
							
						}
	
					}

				}
				
			}

		}
		return true
	}
	
	public boolean getPaxChargeConditionDetailsHBG(Node node_search, String itemCode, boolean isDateFormatResponseAvailable, String responseDateOrDay)
	{
		def flag=false

		Map<String,String> itemMap = new HashMap<String,String>();
		def hotels = node_search.ResponseDetails.SearchHotelPricePaxResponse.HotelDetails.Hotel
		if(hotels[0] == null)
			itemMap=null

		else
		{
			for(i in 0.. hotels.size()-1)
			{
				println "Item code"+hotels[i].Item.@Code[0]
				if(hotels[i].Item.@Code[0].contentEquals(itemCode)) {
					
					def roomCategories =hotels[i].PaxRoomSearchResults.PaxRoom.RoomCategories.RoomCategory
					for (j in 0..roomCategories.size()-1)
					{
						def chargeConditions = roomCategories[j].ChargeConditions.ChargeCondition

						for(k in 0..chargeConditions.size()-1) {
							def conditions = chargeConditions.Condition
							for(l in 0..conditions.size()-1) {
								if(responseDateOrDay.contentEquals("date") && conditions[l].@FromDate != null && isThisDateValid(conditions[l].@ToDate) ) {
									return true
								}
								if(responseDateOrDay.contentEquals("day") && conditions[l].@FromDay != null) {
									return true
								}
							}
							
							
						}
	
					}

				}
				
			}

		}
		return false
	}


	
	

	/**
	 * @return String CheckInDate = 1 month after today date
	 */
	public String getCheckInDate(){
		Calendar cal = Calendar.getInstance()
		cal.add(Calendar.DAY_OF_MONTH, 31)
		return DATE_FORMAT.format(cal.getTime())
	}
	
	public String getCheckInDatePlus(){
		Calendar cal = Calendar.getInstance()
		cal.add(Calendar.DAY_OF_MONTH, 40)
		return DATE_FORMAT.format(cal.getTime())
	}
	
	public String getCheckInDateToday5(){
		Calendar cal = Calendar.getInstance()
		cal.add(Calendar.DAY_OF_MONTH, 5)
		return DATE_FORMAT.format(cal.getTime())
	}

	/**
	 * @return Random Booking Reference
	 */
	def getBookingReference(){
		Random random= new Random()
		return Math.abs(random.nextLong())
	}

	/**
	 * @return String CheckInDate = today's date
	 */
	public String getCheckInTodayDate(){
		Calendar cal = Calendar.getInstance()
		return DATE_FORMAT.format(cal.getTime())
	}
	/**
	 * @return String CheckInDate = today's date
	 */
	public String getCheckInDatePlusDays(String date, int daysToAdd){
		Calendar cal = DATE_FORMAT.parse(date).toCalendar()
        cal.add(Calendar.DAY_OF_MONTH, daysToAdd)
		return DATE_FORMAT.format(cal.getTime())
	}

	/**
	 * @return String CheckOutDate = 1 month + 1 Day after today date
	 */
	public String getCheckOutDate(){
		Calendar cal = Calendar.getInstance()
		cal.add(Calendar.DAY_OF_MONTH, 31)
		return DATE_FORMAT.format(cal.getTime())
	}

	/** validate date format **/

	public isThisDateValid(String dateToValidate){

		if(dateToValidate == null){
			return false;
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
		sdf.setLenient(false);

		try {

			//if not valid, it will throw ParseException
			Date date = sdf.parse(dateToValidate)
			System.out.println(date)

		} catch (ParseException e) {

			e.printStackTrace()
			return false
		}

		return true
	}


	public def getSHPRForMultiRoom(String sheetName,int rowNum,def city, Map inputData) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code',city)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
			                <Duration>${inputData.staydays}</Duration>
			            </PeriodOfStay>
			            <IncludeChargeConditions DateFormatResponse="${inputData.dateformat}"/>
			            <Rooms>
			                <Room Code = "${inputData.room}"
							NumberOfCots = "${inputData.cots}" 
							NumberOfRooms = "${inputData.rooms}">
								<ExtraBeds/>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}

	public def getSHPRForMultiRoomWithAge(String sheetName,int rowNum,def city, Map inputData) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code',city)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
			                <Duration>${inputData.staydays}</Duration>
			            </PeriodOfStay>
			            <IncludeChargeConditions DateFormatResponse="${inputData.dateformat}"/>
			            <Rooms>
			                <Room Code = "${inputData.room}"
							NumberOfCots = "${inputData.cots}" 
							NumberOfRooms = "${inputData.rooms}">
								<ExtraBeds><Age>${inputData.age}</Age><Age>${inputData.age}</Age></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}

	public def getSHPRFor2RoomWithAge(String sheetName,int rowNum,def city, Map inputData) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code',city)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
			                <Duration>${inputData.staydays}</Duration>
			            </PeriodOfStay>
			            <IncludeChargeConditions DateFormatResponse="${inputData.dateformat}"/>
			            <Rooms>
			                <Room Code = "${inputData.room}"
							NumberOfCots = "${inputData.cots}" 
							NumberOfRooms = "${inputData.rooms}">
								<ExtraBeds><Age>${inputData.age}</Age></ExtraBeds>
						</Room>
							  <Room   Code = "${inputData.room}"
                    			NumberOfCots = "${inputData.cots}"
                   				NumberOfRooms = "${inputData.rooms}">
                   				 <ExtraBeds><Age>${inputData.age}</Age></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getCitySearchDurationXml(String sheetName,int rowNum,def city) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code',city)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
			                <Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds/>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	public def getCitySearchDurationWithPkgRatesXml(String sheetName,int rowNum,def city) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code',city)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
			                <Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
						<ShowPackageRates/>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds/>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}

	public def getCitySearchWithChildXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
			                <Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds><Age>${dataMap.get('ExtraBed1.Child_Age')}</Age></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	
	public def getCitySearchWithOnlyItemCodeXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDateToday5()}</CheckInDate>
			                <Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	
	
	public def getCitySearchWithDateFormatResponseXml(String sheetName,int rowNum,def city, def itemCode, def isDateFormatResponseAvailable) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		def requestXml = """<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDateToday5()}</CheckInDate>
			                <Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>"""
						if(isDateFormatResponseAvailable) {
							requestXml = requestXml + """<IncludeChargeConditions DateFormatResponse="${dataMap.get('DateFormateResponse')}"/>"""
						}else {
							requestXml = requestXml + """<IncludeChargeConditions/>"""
						}
						requestXml = requestXml + """
						<Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
		
		return requestXml
	}
	
	public def getCitySearchWithoutDateFormatResponseXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDateToday5()}</CheckInDate>
			                <Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
						<IncludeChargeConditions/>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getAreaSearchXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<CheckOutDate>${getCheckInDatePlusDays(getCheckOutDate(),1)}</CheckOutDate>
			            </PeriodOfStay>
						<IncludeChargeConditions/>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getGeocodeSearchXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}" Latitude ="${dataMap.get('ItemDestination.Latitude')}" 
							Longitude ="${dataMap.get('ItemDestination.Longitude')}" RadiusKm ="${dataMap.get('ItemDestination.RadiusKm')}"/>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
						    <Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getEssentialInformationXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
						<IncludeChargeConditions/>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	
	public def getIncludePriceBreakDownXml(String sheetName,int rowNum,def city, def itemCode, boolean cots) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		def requestXml = """<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
						<IncludePriceBreakdown/>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">"""
		if(cots) {
			requestXml = requestXml + """<ExtraBeds><Age>${dataMap.get('ExtraBed1.Child_Age')}</Age></ExtraBeds>"""
		}else {
			requestXml = requestXml + """<ExtraBeds></ExtraBeds>"""
		}
		requestXml = requestXml + """
								
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
		
		return requestXml
	}
	
	public def getSearchRequestWithIdXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<CheckOutDate>${getCheckInDatePlusDays(getCheckOutDate(),1)}</CheckOutDate>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}" Id=" ">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getSearchRequestWithPackageXml(String sheetName,int rowNum,def city, def itemCode) { 
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
						<ShowPackageRates/>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getSearchMultiRoomsRequestXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getCheapestRoomRateRequestXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
						<CheapestRateOnly/> 
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getMealBasisRequestXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<CheckOutDate>${getCheckInDatePlusDays(getCheckOutDate(),1)}</CheckOutDate>
			            </PeriodOfStay>
						<MealBasisCodes><MealBasis>N</MealBasis></MealBasisCodes>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getStarRatingRequestXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
						<StarRating>${dataMap.get('StarRating')}</StarRating>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getLocationCodeRequestXml(String sheetName,int rowNum,def city, def itemCode) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode></ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
						<LocationCode>${dataMap.get('LocationCode')}</LocationCode>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getSearchOnSaleFlagRequestXml(String sheetName,int rowNum) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "${dataMap.get('SEARCH_HOTEL.Destination_code')}" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>${dataMap.get('SEARCH_HOTEL.ItemCode')}</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>${getCheckInDate()}</CheckInDate>
							<Duration>${dataMap.get('SEARCH_HOTEL.Duration')}</Duration>
			            </PeriodOfStay>
			            <Rooms>
			                <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	//SearchChargeConditions with Date Format Response
	public def getsearchargeconditions(String sheetName,int rowNum,def city, def itemCode, String roomcategoryId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			         <SearchChargeConditionsRequest>
						<DateFormatResponse/>
							<ChargeConditionsHotel>
									<City>$city</City>
									<Item>$itemCode</Item>
									<PeriodOfStay>
         								 <CheckInDate>${getCheckInDate()}</CheckInDate>
         								 <Duration>2</Duration>
									</PeriodOfStay>
									<Rooms>
         								 <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" Id = "$roomcategoryId" NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
         				  					 <ExtraBeds/>
          								</Room>
									</Rooms>
			            	</ChargeConditionsHotel>
			         </SearchChargeConditionsRequest>
			    </RequestDetails>
			</Request>"""
	}
	//SearchChargeConditions without  Date Format Response
	public def getsearchargeconditionswithoutdates(String sheetName,int rowNum,def city, def itemCode, String roomcategoryId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			         <SearchChargeConditionsRequest>
		
							<ChargeConditionsHotel>
									<City>$city</City>
									<Item>$itemCode</Item>
									<PeriodOfStay>
         								 <CheckInDate>${getCheckInDate()}</CheckInDate>
         								 <CheckOutDate>${getCheckInDatePlusDays(getCheckOutDate(), 2)}</CheckOutDate>
									</PeriodOfStay>
									<Rooms>
         								 <Room Code = "${dataMap.get('SEARCH_HOTEL.Room_code')}" 
												Id = "$roomcategoryId" 
												NumberOfCots = "${dataMap.get('SEARCH_HOTEL.Room_numberOfCots')}" 
												NumberOfRooms = "${dataMap.get('SEARCH_HOTEL.Room_numberOfRooms')}">
         				  					 <ExtraBeds/>
          								</Room>
									</Rooms>
			            	</ChargeConditionsHotel>
			         </SearchChargeConditionsRequest>
			    </RequestDetails>
			</Request>"""
	}

	public def getSCCRWithDurationDayResponse(String sheetName,int rowNum,def city, def itemCode, String roomcategoryId, Map inputData) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			         <SearchChargeConditionsRequest>
		
							<ChargeConditionsHotel>
									<City>$city</City>
									<Item>$itemCode</Item>
									<PeriodOfStay>
         								 <CheckInDate>${getCheckInDate()}</CheckInDate>
         								 <Duration>${inputData.staydays}</Duration>
									</PeriodOfStay>
									<Rooms>
         								 <Room Code = "${inputData.room}" 
												Id = "$roomcategoryId" 
												NumberOfCots = "${inputData.cots}" 
												NumberOfRooms = "${inputData.rooms}">
         				  					 <ExtraBeds/>
          								</Room>
									</Rooms>
			            	</ChargeConditionsHotel>
			         </SearchChargeConditionsRequest>
			    </RequestDetails>
			</Request>"""
	}
	public def getSCCRWithAge(String sheetName,int rowNum,def city, def itemCode, String roomcategoryId, Map inputData) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			         <SearchChargeConditionsRequest>
		
							<ChargeConditionsHotel>
									<City>$city</City>
									<Item>$itemCode</Item>
									<PeriodOfStay>
         								 <CheckInDate>${getCheckInDate()}</CheckInDate>
         								  <CheckOutDate>${getCheckInDatePlusDays(getCheckOutDate(),2)}</CheckOutDate>
									</PeriodOfStay>
									<Rooms>
         								 <Room Code = "${inputData.room}" 
												Id = "$roomcategoryId" 
												NumberOfCots = "${inputData.cots}" 
												NumberOfRooms = "${inputData.rooms}">
         				  					 <ExtraBeds>
         				  					    <Age>${inputData.age}</Age><Age>${inputData.age}</Age>
                        					</ExtraBeds>
          								</Room>
									</Rooms>
			            	</ChargeConditionsHotel>
			         </SearchChargeConditionsRequest>
			    </RequestDetails>
			</Request>"""
	}

	public def getSCCRDateFormatWithAge(String sheetName,int rowNum,def city, def itemCode, String roomcategoryId, Map inputData) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			         <SearchChargeConditionsRequest>
						 <DateFormatResponse/>
							<ChargeConditionsHotel>
									<City>$city</City>
									<Item>$itemCode</Item>
									<PeriodOfStay>
         								 <CheckInDate>${getCheckInDate()}</CheckInDate>
         								  <CheckOutDate>${getCheckInDatePlusDays(getCheckOutDate(),2)}</CheckOutDate>
									</PeriodOfStay>
									<Rooms>
         								 <Room Code = "${inputData.room}" 
												Id = "$roomcategoryId" 
												NumberOfCots = "${inputData.cots}" 
												NumberOfRooms = "${inputData.rooms}">
         				  					 <ExtraBeds>
         				  					    <Age>${inputData.age}</Age><Age>${inputData.age}</Age>
                        					</ExtraBeds>
          								</Room>
									</Rooms>
			            	</ChargeConditionsHotel>
			         </SearchChargeConditionsRequest>
			    </RequestDetails>
			</Request>"""
	}
	public def getSCCRWithDayResponse2RateKeys(String sheetName,int rowNum,def city, def itemCode, List roomcategoryId, Map inputData) {

		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		int cnt=roomcategoryId.size()
		def roomcategoryId1= roomcategoryId[0]
		def roomcategoryId2= roomcategoryId[1]

		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			         <SearchChargeConditionsRequest>
		
							<ChargeConditionsHotel>
									<City>$city</City>
									<Item>$itemCode</Item>
									<PeriodOfStay>
         								 <CheckInDate>${getCheckInDate()}</CheckInDate>
 											<CheckOutDate>${getCheckInDatePlusDays(getCheckOutDate(),2)}</CheckOutDate>
         							
									</PeriodOfStay>
									<Rooms>
         								 <Room Code = "${inputData.room}" 
												Id = "$roomcategoryId1" NumberOfCots = "${inputData.cots}" NumberOfRooms = "${inputData.rooms}">
  											 <ExtraBeds>
                           								<Age>${inputData.age}</Age>
         				  					 </ExtraBeds>
         				  					</Room>
										<Room Code = "${inputData.room}" 
												Id = "$roomcategoryId2" NumberOfCots = "${inputData.cots}" NumberOfRooms = "${inputData.rooms}">
   											<ExtraBeds>
                            					<Age>${inputData.age}</Age>
											 </ExtraBeds>
											 </Room>
											
									</Rooms>
			            	</ChargeConditionsHotel>
			         </SearchChargeConditionsRequest>
			    </RequestDetails>
			</Request>"""
	}

	public def getSCCRWithDateResponse2RateKeys(String sheetName,int rowNum,def city, def itemCode, List roomcategoryId, Map inputData) {

		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		int cnt=roomcategoryId.size()
		def roomcategoryId1= roomcategoryId[0]
		def roomcategoryId2= roomcategoryId[1]

		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			         <SearchChargeConditionsRequest>
							<DateFormatResponse/>
							<ChargeConditionsHotel>
									<City>$city</City>
									<Item>$itemCode</Item>
									<PeriodOfStay>
         								 <CheckInDate>${getCheckInDate()}</CheckInDate>
 											<CheckOutDate>${getCheckInDatePlusDays(getCheckOutDate(),2)}</CheckOutDate>
         							
									</PeriodOfStay>
									<Rooms>
         								 <Room Code = "${inputData.room}" 
												Id = "$roomcategoryId1" NumberOfCots = "${inputData.cots}" NumberOfRooms = "${inputData.rooms}">
  											 <ExtraBeds>
                           								<Age>${inputData.age}</Age>
         				  					 </ExtraBeds>
         				  					</Room>
										<Room Code = "${inputData.room}" 
												Id = "$roomcategoryId2" NumberOfCots = "${inputData.cots}" NumberOfRooms = "${inputData.rooms}">
   											<ExtraBeds>
                            					<Age>${inputData.age}</Age>
											 </ExtraBeds>
											 </Room>
											
									</Rooms>
			            	</ChargeConditionsHotel>
			         </SearchChargeConditionsRequest>
			    </RequestDetails>
			</Request>"""
	}
	public def getSCCRWithDurationDayResponse3RateKeys(String sheetName,int rowNum,def city,def itemCode, List roomcategoryId, Map inputData) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code', city)
		dataMap.put('SEARCH_HOTEL.ItemCode', itemCode)
		int cnt=roomcategoryId.size()
		def roomcategoryId1= roomcategoryId[0]
		def roomcategoryId2= roomcategoryId[1]
		def roomcategoryId3= roomcategoryId[2]

		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			         <SearchChargeConditionsRequest>
		
							<ChargeConditionsHotel>
									<City>$city</City>
									<Item>$itemCode</Item>
									<PeriodOfStay>
         								 <CheckInDate>${getCheckInDate()}</CheckInDate>
         								 <Duration>${inputData.staydays}</Duration>
									</PeriodOfStay>
									<Rooms>
         								 <Room Code = "${inputData.room}" 
												Id = "$roomcategoryId1" NumberOfCots = "${inputData.cots}" NumberOfRooms = "${inputData.rooms}">
         				  					 <ExtraBeds/>
         				  					</Room>
										<Room Code = "${inputData.room}" 
												Id = "$roomcategoryId2" NumberOfCots = "${inputData.cots}" NumberOfRooms = "${inputData.rooms}">
											 <ExtraBeds/>
											 </Room>
											<Room Code = "${inputData.room}" 
												Id = "$roomcategoryId3" NumberOfCots = "${inputData.cots}" NumberOfRooms = "${inputData.rooms}">
											 <ExtraBeds/>
          								</Room>
									</Rooms>
			            	</ChargeConditionsHotel>
			         </SearchChargeConditionsRequest>
			    </RequestDetails>
			</Request>"""
	}
	//Payment - RoomTypeBooking - SHPR
	public def getSHPRforPaymnetRoomTypeBooking(String sheetName,int rowNum,String city) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		//println "Client id:"+ clientId
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			   <Source>
			       <RequestorID
			           Client = "${clientId}"
			           EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			           Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences		            
			           
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <SearchHotelPriceRequest>
			            <ItemDestination DestinationCode = "$city" DestinationType ="${dataMap.get('SEARCH_HOTEL.Destination_type')}"/>
						<ItemCode>ABA</ItemCode>
			            <PeriodOfStay>
			                <CheckInDate>2018-06-24</CheckInDate>
							<Duration>2</Duration>
			            </PeriodOfStay>
						<IncludePriceBreakdown />
						<IncludeChargeConditions DateFormatResponse="true"/>
			            <Rooms>
			                <Room Code = "TB" NumberOfCots = "0" NumberOfRooms = "1">
								<ExtraBeds></ExtraBeds>
							</Room>
			            </Rooms>
			        </SearchHotelPriceRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	/*RoomType Booking - Payment - SingleItemWithPayment Details*/
	public def getSingleItemPaymentwithPay(String sheetName,int rowNum,def bookingRef,String City,String ItemCode, String roomCategoryId) {

		dataMap=getSheetDataAsMap(sheetName,rowNum)
		//dataMap.put("SEARCH_HOTEL.Destination_code",City)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			    <Source>
			        <RequestorID
			            Client = "${clientId}"
			            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			            Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <AddBookingRequest Currency = "${dataMap.get('REQUEST.Currency')}">
			            <BookingName/>
			            <BookingReference>$bookingRef</BookingReference>
			            <BookingDepartureDate>${getCheckInDate()}</BookingDepartureDate>
			            <PassengerEmail>jessie.hampshire@gta-travel.com</PassengerEmail>
			            <PaxNames>
			                <PaxName PaxId = "1" PaxType = "adult">${dataMap.get('PaxName1')}</PaxName>
			                <PaxName PaxId = "2" PaxType = "adult">${dataMap.get('PaxName')}</PaxName>
			            </PaxNames>
			            <BookingItems>
			                <BookingItem ItemType = "hotel">
			                    <ItemReference>1</ItemReference>
			                    <ItemCity Code = "$City"/>
			                    <Item Code = "$ItemCode"/>
			                    <ItemRemarks/>
			                    <HotelItem>
			                        <AlternativesAllowed>false</AlternativesAllowed>
			                        <PeriodOfStay>
			                            <CheckInDate>${getCheckInDate()}</CheckInDate>
			                            <Duration>2</Duration>
			                        </PeriodOfStay>
									<HotelRooms>
			                        <HotelRoom Code = "DB" ExtraBed = "false" Id = "$roomCategoryId" NumberOfCots = "0">
			                            <PaxIds>
			                                <PaxId>1</PaxId>
			                                <PaxId>2</PaxId>
			                            </PaxIds>
			                        </HotelRoom>
									</HotelRooms>
			                    </HotelItem>
			                </BookingItem>
			            </BookingItems>
			            <CreditCard CardType = "${dataMap.get('Card.Type')}">
			                <CardHolderName>${dataMap.get('Card.HolderName')}</CardHolderName>
			                <AddressLine1>${dataMap.get('Card.Address')}</AddressLine1>
			                <CityName>${dataMap.get('Card.City')}</CityName>
			                <Zip>${dataMap.get('Card.Zip')}</Zip>
			                <Country>${dataMap.get('Card.Country')}</Country>
			                <CardNumber>${dataMap.get('Card.Number')}</CardNumber>
			                <CardCV2>${dataMap.get('Card.Cvv')}</CardCV2>
			                <ExpiryMonth>${dataMap.get('Card.ExpMonth')}</ExpiryMonth>
			                <ExpiryYear>${dataMap.get('Card.ExpYear')}</ExpiryYear>
			            </CreditCard>
			        </AddBookingRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getSingleItemPaymentwithMasterCard(String sheetName,int rowNum,def bookingRef,String City,String ItemCode, String roomCategoryId, def cardNumber) {
		
				dataMap=getSheetDataAsMap(sheetName,rowNum)
				//dataMap.put("SEARCH_HOTEL.Destination_code",City)
				return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			    <Source>
			        <RequestorID
			            Client = "${clientId}"
			            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			            Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <AddBookingRequest Currency = "${dataMap.get('REQUEST.Currency')}">
			            <BookingName/>
			            <BookingReference>$bookingRef</BookingReference>
			            <BookingDepartureDate>${getCheckInDate()}</BookingDepartureDate>
			            <PassengerEmail>jessie.hampshire@gta-travel.com</PassengerEmail>
			            <PaxNames>
			                <PaxName PaxId = "1" PaxType = "adult">${dataMap.get('PaxName1')}</PaxName>
			                <PaxName PaxId = "2" PaxType = "adult">${dataMap.get('PaxName')}</PaxName>
			            </PaxNames>
			            <BookingItems>
			                <BookingItem ItemType = "hotel">
			                    <ItemReference>1</ItemReference>
			                    <ItemCity Code = "$City"/>
			                    <Item Code = "$ItemCode"/>
			                    <ItemRemarks/>
			                    <HotelItem>
			                        <AlternativesAllowed>false</AlternativesAllowed>
			                        <PeriodOfStay>
			                            <CheckInDate>${getCheckInDate()}</CheckInDate>
			                            <Duration>2</Duration>
			                        </PeriodOfStay>
									<HotelRooms>
			                        <HotelRoom Code = "DB" ExtraBed = "false" Id = "$roomCategoryId" NumberOfCots = "0">
			                            <PaxIds>
			                                <PaxId>1</PaxId>
			                                <PaxId>2</PaxId>
			                            </PaxIds>
			                        </HotelRoom>
									</HotelRooms>
			                    </HotelItem>
			                </BookingItem>
			            </BookingItems>
			            <CreditCard CardType = "MC">
			                <CardHolderName>${dataMap.get('Card.HolderName')}</CardHolderName>
			                <AddressLine1>${dataMap.get('Card.Address')}</AddressLine1>
			                <CityName>${dataMap.get('Card.City')}</CityName>
			                <Zip>${dataMap.get('Card.Zip')}</Zip>
			                <Country>${dataMap.get('Card.Country')}</Country>
			                <CardNumber>${cardNumber}</CardNumber>
			                <CardCV2>${dataMap.get('Card.Cvv')}</CardCV2>
			                <ExpiryMonth>${dataMap.get('Card.ExpMonth')}</ExpiryMonth>
			                <ExpiryYear>${dataMap.get('Card.ExpYear')}</ExpiryYear>
			            </CreditCard>
			        </AddBookingRequest>
			    </RequestDetails>
			</Request>"""
			}
	
	/*RoomType Booking -Payment - AddBooking Payment*/
	public def getAddBookingtwithPay(String sheetName,int rowNum,String bookingId,String City,String ItemCode, String roomCategoryId) {

		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code',City)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			    <Source>
			        <RequestorID
			            Client = "${clientId}"
			            EMailAddress = "${dataMap.get('REQUEST.EmailAddress')}"
			            Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <AddBookingItemRequest>
			            <BookingReference ReferenceSource="api">$bookingId</BookingReference>            
			            
			            <BookingItems>
			                <BookingItem ItemType = "hotel">
			                    <ItemReference>2</ItemReference>
			                    <ItemCity Code = "$City"/>
			                    <Item Code = "$ItemCode"/>
			                    <ItemRemarks/>
			                    <HotelItem>
			                        <AlternativesAllowed>false</AlternativesAllowed>
			                        <PeriodOfStay>
			                            <CheckInDate>${getCheckInDate()}</CheckInDate>
			                            <Duration>1</Duration>
			                        </PeriodOfStay>
			                        <HotelRoom
			                            Code="SB"                           
			                            Id = "$roomCategoryId"
										NumberOfCots="0">
			                            <PaxIds>
			                                <PaxId>1</PaxId>
			                            </PaxIds>
			                        </HotelRoom>
			                    </HotelItem>
			                </BookingItem>
			            </BookingItems>
			        </AddBookingItemRequest>
			    </RequestDetails>
			</Request>"""
	}
	
	public def getAddBookingtItem(String sheetName,int rowNum,String bookingId,String City,String ItemCode, String roomCategoryId, def checkInDate) {

		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('SEARCH_HOTEL.Destination_code',City)
		return	"""<?xml version="1.0" encoding="UTF-8"?>
			<Request>
			    <Source>
			        <RequestorID
			            Client = "${clientId}"
			            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
			            Password = "${dataMap.get('REQUEST.Password')}"/>
			        <RequestorPreferences
			            Country = "${dataMap.get('REQUEST.Country')}"
			            Currency = "${dataMap.get('REQUEST.Currency')}"
			            Language = "${dataMap.get('REQUEST.Language')}">
			            <RequestMode>${dataMap.get('REQUEST.Mode')}</RequestMode>
			        </RequestorPreferences>
			    </Source>
			    <RequestDetails>
			        <AddBookingItemRequest>
			            <BookingReference ReferenceSource="api">$bookingId</BookingReference>            
			            
			            <BookingItems>
			                <BookingItem ItemType = "hotel">
			                    <ItemReference>2</ItemReference>
			                    <ItemCity Code = "$City"/>
			                    <Item Code = "$ItemCode"/>
			                    <ItemRemarks/>
			                    <HotelItem>
			                        <AlternativesAllowed>false</AlternativesAllowed>
			                        <PeriodOfStay>
			                            <CheckInDate>${checkInDate}</CheckInDate>
			                            <Duration>1</Duration>
			                        </PeriodOfStay>
			                        <HotelRooms>
										<HotelRoom Code="SB" Id = "$roomCategoryId" NumberOfCots="0">
				                            <PaxIds>
				                                <PaxId>1</PaxId>
				                            </PaxIds>
			                        	</HotelRoom>
									</HotelRooms>
			                    </HotelItem>
			                </BookingItem>
			            </BookingItems>
			        </AddBookingItemRequest>
			    </RequestDetails>
			</Request>"""
	}

}
