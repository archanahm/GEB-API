package com.hbg.test.RoomTypeBooking

import com.hbg.dto.BookingStatus
import com.hbg.helper.RequestHelper.HotelDTO
import com.hbg.helper.RequestHelper.RequestNewHelper
import com.hbg.helper.RoomTypeBookingHelper.RoomTypeBookingHelper
import com.hbg.test.ClientPack
import com.hbg.test.XSellBaseTest
import com.hbg.util.AS400DatabaseUtil
import com.kuoni.finance.automation.xml.util.ConfigProperties
import com.kuoni.finance.automation.xml.util.XMLValidationUtil
import groovy.xml.XmlUtil
import org.testng.asserts.SoftAssert
import spock.lang.*

abstract class RoomTypeBookingTest_1TB_2extra_1cot_HBG extends XSellBaseTest {

    static final String SHEET_NAME = "1TB_2Extra_1Cot_HBG"
    static final String EXCEL_SHEET = new File(ConfigProperties.getValue("roomTypeBookingSheet"))
    //static final String URL = ConfigProperties.getValue("AuthenticationURL")
    static final String SITEID = ConfigProperties.getValue("SiteId")
    static final String FITSiteIdHeaderKey = ConfigProperties.getValue("FITSiteIdHeaderKey")
    RoomTypeBookingHelper helper = new RoomTypeBookingHelper(EXCEL_SHEET, getClientId())
    int rowNum
    String sheet,testDesc
    SoftAssert softAssert= new SoftAssert()
    @Shared
    def itemRefCount
    @Shared
    List<HotelDTO> hotelListHBG = new ArrayList<HotelDTO>()
    @Shared
    List<HotelDTO> hotelListHBG_TR = new ArrayList<HotelDTO>()
    @Shared
    def bookingDetails = [:]

    BookingStatus bookingStatus
    AS400DatabaseUtil dbconn= new AS400DatabaseUtil()

    /*static String hbgCities = ConfigProperties.getValue("HBG_City_List")
    static final String CITY = "TPE,AKL," + hbgCities
    def cities = CITY.split(",")*/


    /**
     *Search Hotel Price Request HBG
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 1 || #testDesc"(){
        println("Test Running ...$testDesc")
        Node node
        String responseBody
        def cities = numOfCitiesList(3)
        for(k in 0..2){
            def fileContents = helper.searchHotelPriceRequestWith2Extra1Cot(SHEET_NAME, rowNum, cities[k])
            println "Search Request Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'SearchHotelPriceRequest',FITSiteIdHeaderKey,SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("Search Response Xml..\n${responseBody}")
            //println "Search Response Xml..\n${responseBody}"

            // // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            softAssert.assertTrue(!node.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")

            def hotelNode = node.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
            List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
            if (hList.size() > 0){
                hotelListHBG.addAll(hList)
            }
        }

        for (HotelDTO dto : hotelListHBG){
            println ">>>" + dto.toString()
        }
        softAssert.assertTrue(hotelListHBG.size() > 0,"Available hotel not found")

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        1 | "1TB_+2extra+1cot_HBG - SearchHotelPricRequest HBG"
    }

    /**
     * HotelPriceBreakdownRequest
     */
    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 2 || #testDesc"(){
        println("Test Running ...$testDesc")
        Node node
        String responseBody
        def expectedValue
        def responseResult = false

        println "Hotel size : " + hotelListHBG.size()
        for (int i = 0; i < hotelListHBG.size(); i++){
            HotelDTO hDTO = hotelListHBG.get(i)

            def fileContents = helper.hotelPriceBreakdownRequestWith2Extra1Cot(SHEET_NAME, rowNum, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
            println "HotelPriceBreakdownRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'HotelPriceBreakdown',FITSiteIdHeaderKey,SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("HotelPriceBreakdownRequest Response Xml..\n${responseBody}")
            //println "HotelPriceBreakdownRequest Response Xml..\n${responseBody}"

            if (responseBody.contains("ErrorId") || !responseBody.contains("<PriceRange>")){
                continue
            }else{
                responseResult = true
                println ">>>hDTO " + hDTO
                expectedValue = hDTO.getItemCity().toString() +"|" + hDTO.getItemCode().toString()
                if(helper.getExpectedResult().equals("Success")){
                    helper.validateSuccessResponse(responseBody,expectedValue)
                    def hotelRoom = node.ResponseDetails.HotelPriceBreakdownResponse.HotelItem.HotelRooms.HotelRoom
                    def roomCat =  hDTO.getCategoryId().toString()
                    softAssert.assertTrue(hotelRoom.@Id.contains(roomCat), "Response does not contains $roomCat")

//                    for (int j = 0; j < hotelRoom.size(); j++){
//                        if(hotelRoom[j].@Code.toString() == "TB" || hotelRoom[j].@Code.toString() == "CH"){
//                            def roomCode = hotelRoom[j].@Code.toString()
//                            softAssert.assertTrue(hotelRoom[j].@Id.toString().equals(roomCat), "Room Id does not match for $roomCode")
//                        }
//                    }

                }else if(helper.getExpectedResult().equals("Failure")){
                    helper.validateFailureResponse(responseBody)
                }else if(helper.getExpectedResult().equals("Empty")){
                    helper.validateXmlWithEmptyResponse(responseBody)
                }

                // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
                println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"

                break
            }
        }

        softAssert.assertTrue(responseResult==true, "HotelPriceBreakdown failed to get valid hotels returned")


        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        2 | "1TB_+2extra+1cot_HBG - HotelPriceBreakdownRequest"
    }

    /**
    * AddBookingRequest
    */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 3 || #testDesc"(){
        println("Test Running ...$testDesc")
        Node node
        String responseBody
        def bookingRef = null
        //bookingDetails.put('randomBookingRefNum',helper.getBookingReference())
        def expectedValue
        def responseResult = false

        println "Hotel size : " + hotelListHBG.size()
        for (int i = 0; i < hotelListHBG.size(); i++){
            HotelDTO hDTO = hotelListHBG.get(i)
            bookingRef = helper.getBookingReference()
            def fileContents = helper.addBookingRequestWith2Extra1Cot(SHEET_NAME, rowNum, bookingRef, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId())
            println "AddBookingRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url,'application/xml',fileContents,'AddBookingRequest',FITSiteIdHeaderKey,SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("AddBookingRequest Response Xml..\n${responseBody}")
            //println "AddBookingRequest Response Xml..\n${responseBody}"

            def bookingStatus = node.ResponseDetails.BookingResponse.BookingStatus.text()

            if ((responseBody.contains("ErrorId") && (responseBody.contains("Hotel is invalid"))) || bookingStatus.equals("Pending Confirmation") || !responseBody.contains("<BookingReference ReferenceSource=")){
                continue
            }else{
                responseResult = true
                bookingDetails.put('randomBookingRefNum',bookingRef)
                bookingDetails.put('itemRef1RoomId', hDTO.getCategoryId())
                println ">>>hDTO " + hDTO
                expectedValue = hDTO.getItemCity().toString() +"|" + hDTO.getItemCode().toString() + "|Confirmed"
                if(helper.getExpectedResult().equals("Success")){
                    helper.validateSuccessResponse(responseBody,expectedValue)
                    def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
                    def respBookingRefsize = respBookingRef.size()
                    def result = false
                    for (j in 0.. respBookingRefsize){
                        if(respBookingRef[j].@ReferenceSource.equals("client")){
                            result = true
                            softAssert.assertTrue(respBookingRef[j].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                        }

                        if(respBookingRef[j].@ReferenceSource.equals("api")){
                            result = true
                            bookingDetails.put('bookingId',respBookingRef[2].text())
                            break
                        }
                    }
                    softAssert.assertTrue(result==true, "ReferenceSource is not client")

                }else if(helper.getExpectedResult().equals("Failure")){
                    helper.validateFailureResponse(responseBody)
                }else if(helper.getExpectedResult().equals("Empty")){
                    helper.validateXmlWithEmptyResponse(responseBody)
                }

                // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
                println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"

                break
            }
        }

        softAssert.assertTrue(responseResult==true, "No valid hotels returned")

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        3 | "1TB_+2extra+1cot_HBG - AddBookingRequest"
    }

    /**
     *Search Booking Request 1
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 4 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody
            List<String> bookingRefList = new ArrayList<String>()
            List<String> bookingIdList = new ArrayList<String>()

            def fileContents = helper.searchBookingRequest1With2Extra1Cot(SHEET_NAME, rowNum)
            println "SearchBookingRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'SearchBookingRequest', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("SearchBookingRequest Response Xml..\n${responseBody}")
            //println "SearchBookingRequest Response Xml..\n${responseBody}"

            if (helper.getExpectedResult().equals("Success")) {

                def respBookingRef = node.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
                def respBookingRefsize = respBookingRef.size()

                for (i in 0..respBookingRefsize - 1) {

                    if (respBookingRef[i].@ReferenceSource.equals("client")) {
                        bookingRefList.add(respBookingRef[i].text())
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("api")) {
                        bookingIdList.add(respBookingRef[i].text())
                    }

                    if (i == respBookingRefsize - 1) {
                        break
                    }
                }

                softAssert.assertTrue(bookingRefList.contains(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not exist")
                softAssert.assertTrue(bookingIdList.contains(bookingDetails.bookingId.toString()), "Booking Id does not exist")

            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
            softAssert.assertTrue(!node.ResponseDetails.SearchBookingResponse.contains(null), "SearchBookingResponse in response not returned")
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        4 | "1TB_+2extra+1cot_HBG - SearchBookingRequest"
    }

    /**
     *Search Booking Request 2
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 5 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.searchBookingRequest2(SHEET_NAME, rowNum, bookingDetails.bookingId)
            println "SearchBookingRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'SearchBookingRequest', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("SearchBookingRequest Response Xml..\n${responseBody}")
            //println "SearchBookingRequest Response Xml..\n${responseBody}"

            if (helper.getExpectedResult().equals("Success")) {

                def respBookingRef = node.ResponseDetails.SearchBookingResponse.Bookings.Booking.BookingReferences.BookingReference
                def respBookingRefsize = respBookingRef.size()
                def result = false
                for (i in 0..respBookingRefsize) {
                    if (respBookingRef[i].@ReferenceSource.equals("client")) {
                        result = true
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("api")) {
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.bookingId.toString()), "Booking ID does not match")
                        break
                    }
                }
                softAssert.assertTrue(result == true, "ReferenceSource is not client")

            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
            softAssert.assertTrue(!node.ResponseDetails.SearchBookingResponse.contains(null), "SearchBookingResponse is not returned in response")
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        5 | "1TB_+2extra+1cot_HBG - SBR"
    }

    /**
     *SearchHotelPriceRequest HBG - TR
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 6 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody
            def cities = numOfCitiesList(3)
            for (k in 0..2) {
                def fileContents = helper.searchHotelPriceRequestWithDiffDate(SHEET_NAME, rowNum, cities[k])
                println "Search Request Xml.. \n${fileContents}"
                node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'SearchHotelPriceRequest', FITSiteIdHeaderKey, SITEID)
                responseBody = XmlUtil.serialize(node)
                logDebug("Search Response Xml..\n${responseBody}")
                //println "Search Response Xml..\n${responseBody}"

                // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
                softAssert.assertTrue(!node.ResponseDetails.SearchHotelPriceResponse.contains(null), "SearchHotelPriceResponse is empty")

                def hotelNode = node.ResponseDetails.SearchHotelPriceResponse.HotelDetails.Hotel
                List<HotelDTO> hList = RequestNewHelper.getSearchDetails_HBG(hotelNode, 0)
                if (hList.size() > 0) {
                    hotelListHBG_TR.addAll(hList)
                }
            }

            for (HotelDTO dto : hotelListHBG_TR) {
                println ">>>" + dto.toString()
            }
            softAssert.assertTrue(hotelListHBG_TR.size() > 0, "Available hotel not found")
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        6 | "1TB_+2extra+1cot_HBG - SearchHotelPricRequest - TR"
    }

    /**
     * AddBookingItemRequest TR
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 7 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody
            def expectedValue
            def responseResult = false
            itemRefCount = 2

            println "Hotel size : " + hotelListHBG_TR.size()
            for (int i = 0; i < hotelListHBG_TR.size(); i++) {
                HotelDTO hDTO = hotelListHBG_TR.get(i)
                def fileContents = helper.AddBookingItemRequestWith2Extra1Cot(SHEET_NAME, rowNum, bookingDetails.bookingId, hDTO.getItemCity(), hDTO.getItemCode(), hDTO.getCategoryId(), itemRefCount)
                println "AddBookingItemRequest Xml.. \n${fileContents}"
                node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'AddBookingRequest', FITSiteIdHeaderKey, SITEID)
                responseBody = XmlUtil.serialize(node)
                logDebug("AddBookingItemRequest Response Xml..\n${responseBody}")
                //println "AddBookingItemRequest Response Xml..\n${responseBody}"

                def itemStatus = node.ResponseDetails.BookingResponse.BookingItems.BookingItem.ItemStatus.text()

                if ((responseBody.contains("ErrorId") && (responseBody.contains("Hotel is invalid"))) || !responseBody.contains("<BookingReference ReferenceSource=")) {
                    continue
                } else if (itemStatus.equals("Rejected")) {
                    if (i != hotelListHBG_TR.size() - 1) {
                        itemRefCount = itemRefCount + 1
                        println "Item Reference " + itemRefCount
                        continue
                    }
                } else {
                    responseResult = true
                    println ">>>hDTO " + hDTO
                    expectedValue = hDTO.getItemCity().toString() + "|" + hDTO.getItemCode().toString() + "|Confirmed|" + itemRefCount

                    if (helper.getExpectedResult().equals("Success")) {
                        helper.validateSuccessResponse(responseBody, expectedValue)
                        def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
                        def respBookingRefsize = respBookingRef.size()
                        def result = false
                        for (j in 0..respBookingRefsize) {
                            if (respBookingRef[j].@ReferenceSource.equals("client")) {
                                result = true
                                softAssert.assertTrue(respBookingRef[j].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                            }

                            if (respBookingRef[j].@ReferenceSource.equals("api")) {
                                softAssert.assertTrue(respBookingRef[j].text().equals(bookingDetails.bookingId.toString()), "Booking ID does not match")
                                break
                            }
                        }
                        softAssert.assertTrue(result == true, "ReferenceSource is not client")
                    } else if (helper.getExpectedResult().equals("Failure")) {
                        helper.validateFailureResponse(responseBody)
                    } else if (helper.getExpectedResult().equals("Empty")) {
                        helper.validateXmlWithEmptyResponse(responseBody)
                    }

                    // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
                    println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"

                    break
                }
            }

            softAssert.assertTrue(responseResult == true, "No valid hotels returned")
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        7 | "1TB_+2extra+1cot_HBG - ABIR_TR"
    }

    /**
     *BookingItemPriceBreakdown
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 8 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.BookingItemPriceBreakdown2(SHEET_NAME, rowNum, bookingDetails.bookingId)
            println "BookingItemPriceBreakdownRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'BookingItemPriceBreakdown', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("BookingItemPriceBreakdownRequest Response Xml..\n${responseBody}")
            //println "BookingItemPriceBreakdownRequest Response Xml..\n${responseBody}"

            def expectedValue = "true|1"

            if (helper.getExpectedResult().equals("Success")) {
                helper.validateSuccessResponse(responseBody, expectedValue)
                def respBookingRef = node.ResponseDetails.BookingItemPriceBreakdownResponse.BookingReferences.BookingReference
                def respBookingRefsize = respBookingRef.size()
                def result = false
                for (i in 0..respBookingRefsize) {
                    if (respBookingRef[i].@ReferenceSource.equals("client")) {
                        result = true
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("api")) {
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.bookingId.toString()), "Booking ID does not match")
                        break
                    }
                }
                softAssert.assertTrue(result == true, "ReferenceSource is not client")

            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            def bookingItemPriceBreakdown = node.ResponseDetails.BookingItemPriceBreakdownResponse
            softAssert.assertTrue(!bookingItemPriceBreakdown.contains(null), "BookingItemPriceBreakdown is not returned")
            def hotelRoom = bookingItemPriceBreakdown.BookingItem.HotelRooms.HotelRoom
            softAssert.assertTrue(!hotelRoom.RoomPrice.contains(null), "RoomPrice in response is not returned")
            softAssert.assertTrue(!hotelRoom.PriceRanges.PriceRange.contains(null), "PriceRange in response is not returned")

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        8 | "1TB_+2extra+1cot_HBG - BookingItemPriceBreakdown"
    }

    /**
     *Search Booking Item
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 9 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.SearchBookingItemWith2Extra1Cot(SHEET_NAME, rowNum, bookingDetails.bookingId)
            println "SearchBookingItemRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'SearchBookingItemRequest', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("SearchBookingItemRequest Response Xml..\n${responseBody}")
            //println "SearchBookingItemRequest Response Xml..\n${responseBody}"

            def expectedValue = "Confirmed|1"

            if (helper.getExpectedResult().equals("Success")) {
                helper.validateSuccessResponse(responseBody, expectedValue)
                def respBookingRef = node.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
                def respBookingRefsize = respBookingRef.size()
                def result = false
                for (i in 0..respBookingRefsize) {
                    if (respBookingRef[i].@ReferenceSource.equals("client")) {
                        result = true
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("api")) {
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.bookingId.toString()), "Booking ID does not match")
                        break
                    }
                }
                softAssert.assertTrue(result == true, "ReferenceSource is not client")

            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        9 | "1TB_+2extra+1cot_HBG - SearchBookingItem"
    }

    /**
     *Charge Condition Booking
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 10 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.ChargeConditionBookingWith2Extra1Cot(SHEET_NAME, rowNum, bookingDetails.bookingId, itemRefCount)
            println "ChargeConditionBookingRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'ChargeConditions_Booking', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("ChargeConditionBookingRequest Response Xml..\n${responseBody}")
            //println "ChargeConditionBookingRequest Response Xml..\n${responseBody}"

            def expectedValue = "cancellation,amendment"

            if (helper.getExpectedResult().equals("Success")) {
                helper.validateSuccessResponse(responseBody, expectedValue)
            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
            softAssert.assertTrue(!node.ResponseDetails.SearchChargeConditionsResponse.contains(null), "ChargeCondition in response is not returned")
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        10 | "1TB_+2extra+1cot_HBG - ChargeCondition_Booking"
    }

    /**
     *Modify Booking Request
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 11 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.ModifyBookingRequestWith2Extra1Cot(SHEET_NAME, rowNum, bookingDetails.bookingId)
            println "ModifyBookingRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'ModifyBookingRequest', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("ModifyBookingRequest Response Xml..\n${responseBody}")
            //println "ModifyBookingRequest Response Xml..\n${responseBody}"

            def expectedValue = ""

            if (helper.getExpectedResult().equals("Success")) {
                //helper.validateSuccessResponse(responseBody, expectedValue)
                def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
                def respBookingRefsize = respBookingRef.size()
                def result = false
                for (i in 0..respBookingRefsize) {
                    if (respBookingRef[i].@ReferenceSource.equals("client")) {
                        result = true
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("agent")) {
                        softAssert.assertTrue(respBookingRef[i].text().equals("JMB"), "Booking agent ref does not match")
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("api")) {
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.bookingId.toString()), "Booking ID does not match")
                        break
                    }
                }
                softAssert.assertTrue(result == true, "ReferenceSource is not client")

            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        11 | "1TB_+2extra+1cot_HBG - ModifyBookingRequest"
    }

    /**
     *ModifyBookingItemRequest
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 12 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.ModifyBookingItemRequestWith2Extra1Cot(SHEET_NAME, rowNum, bookingDetails.bookingId, bookingDetails.itemRef1RoomId, itemRefCount)
            println "ModifyBookingItemRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'ModifyBookingRequest', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("ModifyBookingItemRequest Response Xml..\n${responseBody}")
            //println "ModifyBookingItemRequest Response Xml..\n${responseBody}"

			softAssert.assertTrue((responseBody.contains("Cannot modify Booking Item") || responseBody.contains("Item is not allowed to be amended")), "Response xml is not matched with the expected xml")
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        12 | "1TB_+2extra+1cot_HBG - MBIR"
    }

    /**
     *CancelBookingItemRequest
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 13 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.CancelBookingItemRequest(SHEET_NAME, rowNum, bookingDetails.bookingId)
            println "CancelBookingItemRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'CancelBookingItemRequest', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("CancelBookingItemRequest Response Xml..\n${responseBody}")
            //println "CancelBookingItemRequest Response Xml..\n${responseBody}"

            def expectedValue = "Confirmed|Cancelled"

            if (helper.getExpectedResult().equals("Success")) {
                helper.validateSuccessResponse(responseBody, expectedValue)
                def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
                def respBookingRefsize = respBookingRef.size()
                def result = false
                for (i in 0..respBookingRefsize) {
                    if (respBookingRef[i].@ReferenceSource.equals("client")) {
                        result = true
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("api")) {
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.bookingId.toString()), "Booking ID does not match")
                        break
                    }
                }
                softAssert.assertTrue(result == true, "ReferenceSource is not client")

            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        13 | "1TB_+2extra+1cot_HBG - CancelBookingItemRequest"
    }

    /**
     *SearchBookingItem _ AfterCXL
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 14 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.SearchBookingItem(SHEET_NAME, rowNum, bookingDetails.bookingId)
            println "SearchBookingItemRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'SearchBookingItemRequest', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("SearchBookingItemRequest Response Xml..\n${responseBody}")
            //println "SearchBookingItemRequest Response Xml..\n${responseBody}"

            def expectedValue = "Confirmed|" + itemRefCount

            if (helper.getExpectedResult().equals("Success")) {
                helper.validateSuccessResponse(responseBody, expectedValue)
                def respBookingRef = node.ResponseDetails.SearchBookingItemResponse.BookingReferences.BookingReference
                def respBookingRefsize = respBookingRef.size()
                def result = false
                for (i in 0..respBookingRefsize) {
                    if (respBookingRef[i].@ReferenceSource.equals("client")) {
                        result = true
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("api")) {
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.bookingId.toString()), "Booking ID does not match")
                        break
                    }
                }
                softAssert.assertTrue(result == true, "ReferenceSource is not client")

            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        14 | "1TB_+2extra+1cot_HBG - SearchBookingItem_AfterCXL"
    }

    /**
     *Cancel Booking Request
     */

    @Unroll
    //@IgnoreRest
    def "Verify RoomTypeBooking test 15 || #testDesc"(){
        println("Test Running ...$testDesc")

        println "Booking Id: " + bookingDetails.bookingId
        softAssert.assertTrue(!bookingDetails.bookingId.equals(null), "Booking Id does not exist")

        if (!bookingDetails.bookingId.equals(null)) {
            Node node
            String responseBody

            def fileContents = helper.CancelBookingRequest(SHEET_NAME, rowNum, bookingDetails.bookingId)
            println "CancelBookingRequest Xml.. \n${fileContents}"
            node = RequestNewHelper.postHttpsXMLRequestWithHeaderNode(url, 'application/xml', fileContents, 'CancelBookingRequest', FITSiteIdHeaderKey, SITEID)
            responseBody = XmlUtil.serialize(node)
            logDebug("CancelBookingRequest Response Xml..\n${responseBody}")
            //println "CancelBookingRequest Response Xml..\n${responseBody}"

            def bookingItem = node.ResponseDetails.BookingResponse.BookingItems.BookingItem
            def itemRefCountString = Integer.toString(itemRefCount)

            //item city code, item code and number of cots
            def expectedValue = "Cancelled|0.00|0.00"

            if (helper.getExpectedResult().equals("Success")) {
                helper.validateSuccessResponse(responseBody, expectedValue)

                for (int i = 0; i <= bookingItem.size() - 1; i++) {
                    println "Item Reference - " + bookingItem[i].ItemReference.text()
                    if (bookingItem[i].ItemReference.text().equals(itemRefCountString)) {
                        softAssert.assertTrue(bookingItem[i].ItemStatus.text().equals("Cancelled"), "ItemStatus for ItemReference $itemRefCount is not cancelled")
                    }
                }

                def respBookingRef = node.ResponseDetails.BookingResponse.BookingReferences.BookingReference
                def respBookingRefsize = respBookingRef.size()
                def result = false
                for (i in 0..respBookingRefsize) {
                    if (respBookingRef[i].@ReferenceSource.equals("client")) {
                        result = true
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.randomBookingRefNum.toString()), "Booking reference number does not match")
                    }

                    if (respBookingRef[i].@ReferenceSource.equals("api")) {
                        softAssert.assertTrue(respBookingRef[i].text().equals(bookingDetails.bookingId.toString()), "Booking ID does not match")
                        break
                    }
                }

                softAssert.assertTrue(result == true, "ReferenceSource is not client")

            } else if (helper.getExpectedResult().equals("Failure")) {
                helper.validateFailureResponse(responseBody)
            } else if (helper.getExpectedResult().equals("Empty")) {
                helper.validateXmlWithEmptyResponse(responseBody)
            }

            BookingStatus bookingStatus = dbconn.getBookingStatusDetails(bookingDetails.bookingId, SITEID.toString(), "X", 3)
            softAssert.assertTrue(bookingStatus.getBookingstatus().equalsIgnoreCase("X"), "Booking status is not Cancelled in CBS after 5 retries")

            // softAssert.assertTrue(node.@ResponseReference.startsWith('XA'))
            println "\n	Valid response ... \nResponseReference : ${node.@ResponseReference}"
        }

        softAssert.assertAll()
        println "\n\n"

        where:
        rowNum | testDesc
        15 | "1TB_GTA - CancelBookingRequest"
    }
}

class RoomTypeBookingTest_1TB_2extra_1cot_HBG_C1237 extends RoomTypeBookingTest_1TB_2extra_1cot_HBG {
    @Override
    String getClientId() {
        return "1237"
    }
}

class RoomTypeBookingTest_1TB_2extra_1cot_HBG_C1238 extends RoomTypeBookingTest_1TB_2extra_1cot_HBG {
    @Override
    String getClientId() {
        return "1238"
    }
}

class RoomTypeBookingTest_1TB_2extra_1cot_HBG_C1239 extends RoomTypeBookingTest_1TB_2extra_1cot_HBG {
    @Override
    String getClientId() {
        return "1239"
    }
}

class RoomTypeBookingTest_1TB_2extra_1cot_HBG_C1244 extends RoomTypeBookingTest_1TB_2extra_1cot_HBG {
    @Override
    String getClientId() {
        return "1244"
    }
}
@IgnoreIf({!(Boolean.valueOf(null != System.getProperty("clientId")))})
class RoomTypeBookingTest_1TB_2extra_1cot_HBGSingleClient extends RoomTypeBookingTest_1TB_2extra_1cot_HBG {

    @Override
    String getClientId() {
        String clientId = System.getProperty("clientId")
        println "Using client Id $clientId"
        return clientId
    }
}
