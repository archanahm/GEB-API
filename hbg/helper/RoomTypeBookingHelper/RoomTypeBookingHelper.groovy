package com.hbg.helper.RoomTypeBookingHelper

import com.hbg.test.XSellBaseTest
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import com.kuoni.qa.automation.common.util.ExcelUtil
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
import java.text.SimpleDateFormat

class RoomTypeBookingHelper {

    ExcelUtil excelUtil
    String sheetName
    Map dataMap
    Map validateMap= new HashMap()
    String ExpectedResult
    SoftAssert softAssert= new SoftAssert()
    XMLValidationUtil XmlValidate = new XMLValidationUtil()
    private static def xmlParser = new XmlParser()
    static final String URL= ConfigProperties.getValue("AuthenticationURL")
    static final def timeout = 10 * 60 * 1000
    static RequestConfig requestTimeoutConfig = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();
    static final def timeoOutMilliSecs = 5 * 1000
    public String clientId

    public String getExpectedResult() {
        return ExpectedResult;
    }

    RoomTypeBookingHelper(String excelPath, String clientId){
        excelUtil=new ExcelUtil(excelPath)
        println("-----------------------------------------------------------------------------\n")
        println("Request URL :  ${XSellBaseTest.url} \n")
        println("-----------------------------------------------------------------------------\n")
        this.clientId = clientId
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
        //httpClient

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
        println(">>>>"+expectedName)
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

    /*public def validateXmlFileWithExpectedElement(String elementName, String xml){
     def lower = elementName.toLowerCase()
     softAssert.assertTrue(xml.toLowerCase().contains(lower),"Response Body does not have the element "+ elementName)
     }
     public def validateXmlFileWithExpectedValuesAttrs(String attributeName, String attributeValue, String xml){
     softAssert.assertTrue(xml.toLowerCase().contains("$attributeName=\"$attributeValue\"".toLowerCase()),"Response Body does not have the element tag "+ attributeName +" with value "+ attributeValue)
     }
     */

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

    /**
     * @return String CheckInDate = 2 month after today date
     */
    public String getCheckInDate(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 90)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String CheckInDate2 = 7 days after todays date
     */
    public String getCheckInDate2(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 7)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String CheckInDate = 2 month + 2 days after today date
     */
    public String getCheckInNewDate(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 62)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    public String getCheckInDateToday5(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 5)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
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
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String CheckOutDate = 2 month + 2 Day after today date
     */
    public String getCheckOutDate(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 92)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String CheckOutDate = 9 Days after today date
     */
    public String getCheckOutDate2(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 9)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String CheckOutDate = 2 month + 4 Day after today date
     */
    public String getCheckOutNewDate(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 64)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

	/**
	 * @return String CheckInDate = 2 month after today date
	 */
	public String getCheckOutDate3(){
		Calendar cal = Calendar.getInstance()
		cal.add(Calendar.DAY_OF_MONTH, 61)
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
		return DATE_FORMAT.format(cal.getTime())
	}

	
    /**
     * @return String FromDate = 2 + 2 days month after today date
     */
    public String getFromDate(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 62)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String FromDate = 2 + 2 days month after today date
     */
    public String getFromDate2(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 9)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String FromDate = 2 month days after today date
     *
     */
    public String getToDate(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 60)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String FromDate = 2 month days after today date
     *
     */
    public String getToDate2(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 7)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String Departure Date = 28 days after todays date
     *
     */
    public String getDeptDate(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 28)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /**
     * @return String Departure Date = 7 days after todays date
     *
     */
    public String getDeptDate2(){
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 7)
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
        return DATE_FORMAT.format(cal.getTime())
    }

    /*******************************************************************************************************************/

    public def searchHotelPriceRequest(String sheetName,int rowNum, def city) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('DestinationCode', city)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${clientId}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
        <SearchHotelPriceRequest>
        <ItemDestination DestinationCode = "${dataMap.get('DestinationCode')}" DestinationType = "${dataMap.get('DestinationType')}"/>
            <ItemCode>${dataMap.get('ItemCode')}</ItemCode>
            <PeriodOfStay>
                <CheckInDate>${getCheckInDate()}</CheckInDate>
                <Duration>${dataMap.get('Duration')}</Duration>
            </PeriodOfStay>
            <IncludeChargeConditions DateFormatResponse="${dataMap.get('DateFormatResponse')}"/>
            <Rooms>
                <Room
                    Code = "${dataMap.get('RoomCode')}"
                    NumberOfCots = "${dataMap.get('RoomNumberOfCots')}"
                    NumberOfRooms = "${dataMap.get('NumberOfRooms')}">
                    <ExtraBeds/>
                </Room>
            </Rooms>
        </SearchHotelPriceRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def hotelPriceBreakdownRequest(String sheetName,int rowNum, def itemCity, def itemCode, def roomCategory) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('CityCode', itemCity)
        dataMap.put('ItemCode', itemCode)
        dataMap.put('RoomID', roomCategory)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
        <HotelPriceBreakdownRequest>
            <City>${dataMap.get('CityCode')}</City>
            <Item>${dataMap.get('ItemCode')}</Item>
            <PeriodOfStay>
                <CheckInDate>${getCheckInDate()}</CheckInDate>
                <Duration>2</Duration>
            </PeriodOfStay>
            <Rooms>
                <Room 
                    Code = "${dataMap.get('RoomCode')}" 
                    Id = "${dataMap.get('RoomID')}" 
                    NumberOfCots = "${dataMap.get('RoomNumberOfCots')}" 
                    NumberOfRooms = "${dataMap.get('NumberOfRooms')}">
                    <ExtraBeds/>
                </Room>
            </Rooms>
        </HotelPriceBreakdownRequest>
 </RequestDetails>
</Request>"""
        }
    }

    public def addBookingRequest(String sheetName,int rowNum, def bookingRef, def itemCity, def itemCode, def roomCategory) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('BookingItemCityCode', itemCity)
        dataMap.put('BookingItemCode', itemCode)
        dataMap.put('HotelRoomId', roomCategory)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
    <AddBookingRequest Currency = "${dataMap.get('BookingCurrency')}">
        <BookingName>${dataMap.get('BookingName')}</BookingName>
        <BookingReference>${bookingRef}</BookingReference>
        <AgentReference>${dataMap.get('BookingAgentRef')}</AgentReference>
        <BookingDepartureDate>${getCheckInDate()}</BookingDepartureDate>
        <PassengerEmail>${dataMap.get('PassengerEmail')}</PassengerEmail>
        <PaxNames>
            <PaxName PaxId = "1">JESS TEST</PaxName>
            <PaxName PaxId = "2">TETS 2</PaxName>
            <PaxName PaxId = "3">JESS3 TEST</PaxName>
            <PaxName PaxId = "4">TETS 3</PaxName>
            <PaxName PaxId = "5">TETS 4</PaxName>
        </PaxNames>
   <BookingItems>
        <BookingItem ItemType = "${dataMap.get('BookingItemType')}">
        <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
        <ItemCity Code = "${dataMap.get('BookingItemCityCode')}"/>
        <Item Code = "${dataMap.get('BookingItemCode')}"/>
        <ItemRemarks> </ItemRemarks>
        <HotelItem>
            <AlternativesAllowed>${dataMap.get('AlternativesAllowed')}</AlternativesAllowed>
            <PeriodOfStay>
                <CheckInDate>${getCheckInDate()}</CheckInDate>
                <CheckOutDate>${getCheckOutDate()}</CheckOutDate>
            </PeriodOfStay>
            <HotelRooms>
                <HotelRoom
                    Code = "${dataMap.get('HotelRoomCode')}"
                    ExtraBed = "${dataMap.get('HotelRoomExtraBed')}"
                    Id = "${dataMap.get('HotelRoomId')}"
                    SharingBedding = "${dataMap.get('HotelRoomSharingBed')}"
                    NumberOfCots = "${dataMap.get('HotelRoomNumberOfCots')}">
                    <PaxIds>
                        <PaxId>1</PaxId>
                        <PaxId>2</PaxId>
                    </PaxIds>
                </HotelRoom>
            </HotelRooms>
        </HotelItem>
        </BookingItem>
   </BookingItems>
   </AddBookingRequest>
   </RequestDetails>
</Request>"""
        }
    }

    public def searchBookingRequest1(String sheetName,int rowNum) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
        <SearchBookingRequest>
            <BookingDateRange DateType = "${dataMap.get('BookingDateRangeType')}">
                <FromDate>${getToDate()}</FromDate>
                <ToDate>${getFromDate()}</ToDate>
            </BookingDateRange>
            <EchoSearchCriteria>${dataMap.get('EchoSearchCriteria')}</EchoSearchCriteria>
            <ShowPaymentStatus>${dataMap.get('ShowPaymentStatus')}</ShowPaymentStatus>
        </SearchBookingRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def searchBookingRequest2(String sheetName,int rowNum, def bookingId) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
        <SearchBookingRequest>
            <BookingReference ReferenceSource="api">${bookingId}</BookingReference>
            <ShowPaymentStatus>${dataMap.get('ShowPaymentStatus')}</ShowPaymentStatus>
        </SearchBookingRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def AddBookingItemRequest_HBG(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId, def secondItemRefCount) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('BookingItemCityCode', itemCity)
        dataMap.put('BookingItemCode', itemCode)
        dataMap.put('HotelRoomId', roomId)
        dataMap.put('ItemReference', secondItemRefCount)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}" 
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
        <AddBookingItemRequest>
            <BookingReference ReferenceSource="${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
            <BookingItems>
                <BookingItem ItemType="${dataMap.get('BookingItemType')}">
                    <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
                    <ItemCity Code="${dataMap.get('BookingItemCityCode')}"/>
                    <Item Code="${dataMap.get('BookingItemCode')}"/>
                    <ItemRemarks/>
                    <HotelItem>
                        <AlternativesAllowed>${dataMap.get('AlternativesAllowed')}</AlternativesAllowed>
                        <PeriodOfStay>
                            <CheckInDate>${getCheckInDate()}</CheckInDate>
                            <CheckOutDate>${getCheckOutDate()}</CheckOutDate>
                        </PeriodOfStay>
                        <HotelRooms>
                            <HotelRoom 
                                Code="${dataMap.get('HotelRoomCode')}" 
                                ExtraBed="${dataMap.get('HotelRoomExtraBed')}" 
                                Id="${dataMap.get('HotelRoomId')}" 
                                NumberOfCots="${dataMap.get('HotelRoomNumberOfCots')}" >
                                <PaxIds>
                                    <PaxId>3</PaxId> 
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

    public def AddBookingItemRequest_GTA(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId, def thirdItemRefCount) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('BookingItemCityCode', itemCity)
        dataMap.put('BookingItemCode', itemCode)
        dataMap.put('HotelRoomId', roomId)
        dataMap.put('ItemReference', thirdItemRefCount)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
  <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}" 
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
        <AddBookingItemRequest>
            <BookingReference ReferenceSource="${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
            <BookingItems>
                <BookingItem ItemType="${dataMap.get('BookingItemType')}">
                    <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
                    <ItemCity Code="${dataMap.get('BookingItemCityCode')}"/>
                    <Item Code="${dataMap.get('BookingItemCode')}"/>
                    <ItemRemarks/>
                    <HotelItem>
                        <AlternativesAllowed>${dataMap.get('AlternativesAllowed')}</AlternativesAllowed>
                        <PeriodOfStay>
                            <CheckInDate>${getCheckInDate()}</CheckInDate>
                            <CheckOutDate>${getCheckOutDate()}</CheckOutDate>
                        </PeriodOfStay>
                        <HotelRooms>
                            <HotelRoom 
                                Code="${dataMap.get('HotelRoomCode')}" 
                                ExtraBed="${dataMap.get('HotelRoomExtraBed')}" 
                                Id="${dataMap.get('HotelRoomId')}" 
                                NumberOfCots="${dataMap.get('HotelRoomNumberOfCots')}" >
                                <PaxIds>
                                    <PaxId>4</PaxId>
                                    <PaxId>5</PaxId>
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

    public def BookingItemPriceBreakdown(String sheetName,int rowNum, def bookingId, def secondItemRefCount) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('ItemReference', secondItemRefCount)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
        <BookingItemPriceBreakdownRequest>
            <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
            <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
        </BookingItemPriceBreakdownRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def BookingItemPriceBreakdown2(String sheetName,int rowNum, def bookingId) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
        <BookingItemPriceBreakdownRequest>
            <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
            <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
        </BookingItemPriceBreakdownRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def SearchBookingItem(String sheetName,int rowNum, def bookingId) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
    <SearchBookingItemRequest>
      <BookingReference ReferenceSource="${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
       <ShowPaymentStatus>${dataMap.get('ShowPaymentStatus')}</ShowPaymentStatus>
    </SearchBookingItemRequest>
  </RequestDetails>
</Request>"""
        }
    }

    public def ChargeConditionBooking(String sheetName,int rowNum, def bookingId, def secondItemRefCount) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('ItemReference', secondItemRefCount)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}"
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
        <SearchChargeConditionsRequest>
        <DateFormatResponse/>   
        <ChargeConditionsBookingItem>
        <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
        <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
        </ChargeConditionsBookingItem>
        </SearchChargeConditionsRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def ModifyBookingRequest(String sheetName,int rowNum, def bookingId) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}" 
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
 <RequestDetails>
    <ModifyBookingRequest>
        <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
        <AgentReference>${dataMap.get('AgentRef')}</AgentReference>
        <BookingDepartureDate>${getDeptDate()}</BookingDepartureDate>
        <PassengerEmail>${dataMap.get('PassengerEmail')}</PassengerEmail>
        <PaxNames>
            <PaxName PaxId = "1" PaxType = "adult">JESS3 TEST</PaxName>
            <PaxName PaxId = "2" PaxType = "adult">TEST3 TEST</PaxName>
            <PaxName PaxId = "3">JESS3 TEST</PaxName>
            <PaxName PaxId = "4">TETS 3</PaxName>
            <PaxName PaxId = "5">TETS 4</PaxName>
        </PaxNames>
     </ModifyBookingRequest>
  </RequestDetails>
</Request>"""
        }
    }

    public def ModifyBookingItemRequest(String sheetName,int rowNum, def bookingId, def roomId, def thirdItemRefCount) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('ItemReference', thirdItemRefCount)
        dataMap.put('HotelRoomId', roomId)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}" 
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
 <RequestDetails>
 <ModifyBookingItemRequest>
    <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
    <BookingItems>
    <BookingItem ItemType = "${dataMap.get('BookingItemType')}">
    <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
    <ItemRemarks/>
    <HotelItem>
        <AlternativesAllowed>${dataMap.get('AlternativesAllowed')}</AlternativesAllowed>
        <PeriodOfStay>
            <CheckInDate>${getCheckInNewDate()}</CheckInDate>
            <CheckOutDate>${getCheckOutNewDate()}</CheckOutDate>
        </PeriodOfStay>
        <HotelRooms>
            <HotelRoom 
                Code = "${dataMap.get('HotelRoomCode')}" 
                Id = "${dataMap.get('HotelRoomId')}" 
                NumberOfCots = "${dataMap.get('HotelRoomNumberOfCots')}">
            <PaxIds>
                <PaxId>1</PaxId>
                <PaxId>2</PaxId>
            </PaxIds>
            </HotelRoom>
        </HotelRooms>
    </HotelItem>
    </BookingItem>
    </BookingItems>
 </ModifyBookingItemRequest>
 </RequestDetails>
</Request>"""
        }
    }

    public def CancelBookingItemRequest(String sheetName,int rowNum, def bookingId) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
    <CancelBookingItemRequest>
      <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
      <ItemReferences>
        <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
      </ItemReferences>
    </CancelBookingItemRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def CancelBookingRequest(String sheetName,int rowNum, def bookingId) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
   <CancelBookingRequest>
      <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
    </CancelBookingRequest>
  </RequestDetails>
</Request>"""
        }
    }


    /**
     *
     * With2Extra1Cot Requests
     *
     */

    public def searchHotelPriceRequestWith2Extra1Cot(String sheetName,int rowNum, def city) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('DestinationCode', city)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}"
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
        <SearchHotelPriceRequest>
        <ItemDestination DestinationCode = "${dataMap.get('DestinationCode')}" DestinationType = "${dataMap.get('DestinationType')}"/>
            <ItemCode></ItemCode>
            <PeriodOfStay>
                <CheckInDate>${getCheckInDate2()}</CheckInDate>
                <Duration>${dataMap.get('Duration')}</Duration>
            </PeriodOfStay>
            <IncludePriceBreakdown/>
            <!--<IncludeChargeConditions DateFormatResponse="${dataMap.get('DateFormatResponse')}"/>-->
            <Rooms>
                <Room
                    Code = "${dataMap.get('RoomCode')}"
                    NumberOfCots = "${dataMap.get('RoomNumberOfCots')}"
                    NumberOfRooms = "${dataMap.get('NumberOfRooms')}">
                    <ExtraBeds><Age>7</Age><Age>9</Age></ExtraBeds>
                </Room>
            </Rooms>
            <OrderBy>pricelowtohigh</OrderBy> 
        </SearchHotelPriceRequest>
        
    </RequestDetails>
</Request>"""
        }
    }

    public def searchHotelPriceRequestWithDiffDate(String sheetName,int rowNum, def city) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('DestinationCode', city)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
        <SearchHotelPriceRequest>
        <ItemDestination DestinationCode = "${dataMap.get('DestinationCode')}" DestinationType = "${dataMap.get('DestinationType')}"/>
            <ItemCode>${dataMap.get('ItemCode')}</ItemCode>
            <PeriodOfStay>
                <CheckInDate>${getCheckInDate2()}</CheckInDate>
                <Duration>${dataMap.get('Duration')}</Duration>
            </PeriodOfStay>
            <IncludeChargeConditions DateFormatResponse="${dataMap.get('DateFormatResponse')}"/>
            <Rooms>
                <Room
                    Code = "${dataMap.get('RoomCode')}"
                    NumberOfCots = "${dataMap.get('RoomNumberOfCots')}"
                    NumberOfRooms = "${dataMap.get('NumberOfRooms')}">
                    <ExtraBeds/>
                </Room>
            </Rooms>
        </SearchHotelPriceRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def hotelPriceBreakdownRequestWith2Extra1Cot(String sheetName,int rowNum, def itemCity, def itemCode, def roomCategory) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('CityCode', itemCity)
        dataMap.put('ItemCode', itemCode)
        dataMap.put('RoomID', roomCategory)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
        <HotelPriceBreakdownRequest>
            <City>${dataMap.get('CityCode')}</City>
            <Item>${dataMap.get('ItemCode')}</Item>
            <PeriodOfStay>
                <CheckInDate>${getCheckInDate2()}</CheckInDate>
                <Duration>${dataMap.get('Duration')}</Duration>
            </PeriodOfStay>
            <Rooms>
                <Room 
                    Code = "${dataMap.get('RoomCode')}" 
                    Id = "${dataMap.get('RoomID')}" 
                    NumberOfCots = "${dataMap.get('RoomNumberOfCots')}" 
                    NumberOfRooms = "${dataMap.get('NumberOfRooms')}">
                    <ExtraBeds><Age>7</Age><Age>9</Age></ExtraBeds>
                </Room>
            </Rooms>
        </HotelPriceBreakdownRequest>
 </RequestDetails>
</Request>"""
        }
    }

    public def addBookingRequestWith2Extra1Cot(String sheetName,int rowNum, def bookingRef, def itemCity, def itemCode, def roomCategory) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('BookingItemCityCode', itemCity)
        dataMap.put('BookingItemCode', itemCode)
        dataMap.put('HotelRoomId', roomCategory)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
    <AddBookingRequest Currency = "${dataMap.get('BookingCurrency')}">
        <BookingName>${dataMap.get('BookingName')}</BookingName>
        <BookingReference>${bookingRef}</BookingReference>
        <AgentReference>${dataMap.get('BookingAgentRef')}</AgentReference>
        <BookingDepartureDate>${getCheckInDate2()}</BookingDepartureDate>
        <PassengerEmail>${dataMap.get('PassengerEmail')}</PassengerEmail>
        <PaxNames>
            <PaxName PaxId = "1">JESS TEST</PaxName>
                <PaxName PaxId = "2">TETS 2</PaxName>
                <PaxName PaxId = "3">JESS3 TEST</PaxName>
                <PaxName PaxId = "4">TETS 3</PaxName>
                <PaxName ChildAge = "7" PaxId = "5" PaxType = "child">c 1</PaxName>
                <PaxName ChildAge = "9" PaxId = "6" PaxType = "child">c2 1</PaxName>
                <PaxName PaxId = "7">TETS 8</PaxName>   
        </PaxNames>
   <BookingItems>
        <BookingItem ItemType = "${dataMap.get('BookingItemType')}">
        <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
        <ItemCity Code = "${dataMap.get('BookingItemCityCode')}"/>
        <Item Code = "${dataMap.get('BookingItemCode')}"/>
        <ItemRemarks> </ItemRemarks>
        <HotelItem>
            <AlternativesAllowed>${dataMap.get('AlternativesAllowed')}</AlternativesAllowed>
            <PeriodOfStay>
                <CheckInDate>${getCheckInDate2()}</CheckInDate>
                <CheckOutDate>${getCheckOutDate2()}</CheckOutDate>
            </PeriodOfStay>
            <HotelRooms>
                <HotelRoom
                    Code = "${dataMap.get('HotelRoomCode')}"
                    ExtraBed = "${dataMap.get('HotelRoomExtraBed')}"
                    Id = "${dataMap.get('HotelRoomId')}"
                    SharingBedding = "${dataMap.get('HotelRoomSharingBed')}"
                    NumberOfCots = "${dataMap.get('HotelRoomNumberOfCots')}"
                    NumberOfExtraBeds = "${dataMap.get('HotelRoomNumberOfExtraBeds')}">
                    <PaxIds>
                        <PaxId>1</PaxId>
                        <PaxId>2</PaxId>
                        <PaxId>5</PaxId>
                        <PaxId>6</PaxId>
                    </PaxIds>
                </HotelRoom>
            </HotelRooms>
        </HotelItem>
        </BookingItem>
   </BookingItems>
   </AddBookingRequest>
   </RequestDetails>
</Request>"""
        }
    }

    public def searchBookingRequest1With2Extra1Cot(String sheetName,int rowNum) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
        <SearchBookingRequest>
            <BookingDateRange DateType = "${dataMap.get('BookingDateRangeType')}">
                <FromDate>${getToDate2()}</FromDate>
                <ToDate>${getFromDate2()}</ToDate>
            </BookingDateRange>
            <EchoSearchCriteria>${dataMap.get('EchoSearchCriteria')}</EchoSearchCriteria>
            <ShowPaymentStatus>${dataMap.get('ShowPaymentStatus')}</ShowPaymentStatus>
        </SearchBookingRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def AddBookingItemRequestWith2Extra1Cot(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId, def itemRefCount) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('BookingItemCityCode', itemCity)
        dataMap.put('BookingItemCode', itemCode)
        dataMap.put('HotelRoomId', roomId)
        dataMap.put('ItemReference', itemRefCount)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}" 
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
        <AddBookingItemRequest>
            <BookingReference ReferenceSource="${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
            <BookingItems>
                <BookingItem ItemType="${dataMap.get('BookingItemType')}">
                    <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
                    <ItemCity Code="${dataMap.get('BookingItemCityCode')}"/>
                    <Item Code="${dataMap.get('BookingItemCode')}"/>
                    <ItemRemarks/>
                    <HotelItem>
                        <AlternativesAllowed>${dataMap.get('AlternativesAllowed')}</AlternativesAllowed>
                        <PeriodOfStay>
                            <CheckInDate>${getCheckInDate2()}</CheckInDate>
                            <CheckOutDate>${getCheckOutDate2()}</CheckOutDate>
                        </PeriodOfStay>
                        <HotelRooms>
                            <HotelRoom 
                                Code="${dataMap.get('HotelRoomCode')}" 
                                ExtraBed="${dataMap.get('HotelRoomExtraBed')}" 
                                Id="${dataMap.get('HotelRoomId')}" 
                                NumberOfCots="${dataMap.get('HotelRoomNumberOfCots')}" >
                                <PaxIds>
                                    <PaxId>3</PaxId>
                                    <PaxId>4</PaxId>
                                    <PaxId>7</PaxId>
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

    public def SearchBookingItemWith2Extra1Cot(String sheetName,int rowNum, def bookingId) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
   <RequestDetails>
    <SearchBookingItemRequest>
      <BookingReference ReferenceSource="${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
        <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
        <ShowPaymentStatus>${dataMap.get('ShowPaymentStatus')}</ShowPaymentStatus>
    </SearchBookingItemRequest>
  </RequestDetails>
</Request>"""
        }
    }

    public def ChargeConditionBookingWith2Extra1Cot(String sheetName,int rowNum, def bookingId, def itemRefCount) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('ItemReference', itemRefCount)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}"
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
    <RequestDetails>
        <SearchChargeConditionsRequest>
        <DateFormatResponse/>   
        <ChargeConditionsBookingItem>
        <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
        <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
        </ChargeConditionsBookingItem>
        </SearchChargeConditionsRequest>
    </RequestDetails>
</Request>"""
        }
    }

    public def ModifyBookingRequestWith2Extra1Cot(String sheetName,int rowNum, def bookingId) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}" 
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
 <RequestDetails>
    <ModifyBookingRequest>
        <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
        <AgentReference>${dataMap.get('AgentRef')}</AgentReference>
        <BookingDepartureDate>${getDeptDate2()}</BookingDepartureDate>
        <PaxNames>
            <PaxName PaxId = "1">JESS TEST</PaxName>
                <PaxName PaxId = "2">TETS 2</PaxName>
                <PaxName PaxId = "3">JESS3 TEST</PaxName>
                <PaxName PaxId = "4">TETS 3</PaxName>
                <PaxName
                    ChildAge = "3"
                    PaxId = "5"
                    PaxType = "child">c 1</PaxName>
                <PaxName
                    ChildAge = "6"
                    PaxId = "6"
                    PaxType = "child">c2 1</PaxName>
        </PaxNames>
     </ModifyBookingRequest>
  </RequestDetails>
</Request>"""
        }
    }

    public def ModifyBookingItemRequestWith2Extra1Cot(String sheetName,int rowNum, def bookingId, def roomId, def itemRefCount) {
        dataMap=getSheetDataAsMap(sheetName,rowNum)
        dataMap.put('HotelRoomId', roomId)
        dataMap.put('ItemReference', itemRefCount)
        if(clientId)
            dataMap.put('ClientID', clientId)
        if(dataMap && dataMap.get("Flag")){
            //return file
            return	"""<?xml version="1.0" encoding="UTF-8"?>
<Request>
    <Source>
         <RequestorID 
            Client = "${dataMap.get('ClientID')}"
            EMailAddress = "${dataMap.get('EmailAddress')}"
            Password = "${dataMap.get('Password')}"/>
        <RequestorPreferences
            Country = "${dataMap.get('Country')}" 
            Currency = "${dataMap.get('Currency')}"
            Language = "${dataMap.get('Language')}">
            <RequestMode>${dataMap.get('Mode')}</RequestMode>
        </RequestorPreferences>
    </Source>
 <RequestDetails>
 <ModifyBookingItemRequest>
    <BookingReference ReferenceSource = "${dataMap.get('BookingReferenceSource')}">${bookingId}</BookingReference>
    <BookingItems>
    <BookingItem ItemType = "${dataMap.get('BookingItemType')}">
    <ItemReference>${dataMap.get('ItemReference')}</ItemReference>
    <ItemRemarks/>
    <HotelItem>
        <AlternativesAllowed>${dataMap.get('AlternativesAllowed')}</AlternativesAllowed>
        <PeriodOfStay>
            <CheckInDate>${getCheckInNewDate()}</CheckInDate>
            <CheckOutDate>${getCheckOutNewDate()}</CheckOutDate>
        </PeriodOfStay>
        <HotelRooms>
            <HotelRoom 
                Code = "${dataMap.get('HotelRoomCode')}" 
                Id = "${dataMap.get('HotelRoomId')}" 
                NumberOfCots = "${dataMap.get('HotelRoomNumberOfCots')}">
            <PaxIds>
                <PaxId>3</PaxId>
                <PaxId>4</PaxId>
                <PaxId>7</PaxId>
            </PaxIds>
            </HotelRoom>
        </HotelRooms>
    </HotelItem>
    </BookingItem>
    </BookingItems>
 </ModifyBookingItemRequest>
 </RequestDetails>
</Request>"""
        }
    }
}