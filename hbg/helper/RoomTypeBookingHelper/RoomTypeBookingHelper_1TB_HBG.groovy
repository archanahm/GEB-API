package com.hbg.helper.RoomTypeBookingHelper

class RoomTypeBookingHelper_1TB_HBG extends RoomTypeBookingHelper{

	RoomTypeBookingHelper_1TB_HBG(String excelPath, String clientId){
		super(excelPath, clientId)
	}

	public def searchHotelPriceRequestFor_1TB_HBG(String sheetName,int rowNum, def city) {
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

	public def hotelPriceBreakdownRequestFor_1TB_HBG(String sheetName,int rowNum, def itemCity, def itemCode, def roomCategory) {
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

	public def addBookingRequestFor_1TB_HBG(String sheetName,int rowNum, def bookingRef, def itemCity, def itemCode, def roomCategory) {
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
        <ItemRemarks><ItemRemark Code = "${dataMap.get('BookingItemRemarks')}"/></ItemRemarks>
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

	public def searchBookingRequest1For_1TB_HBG(String sheetName,int rowNum) {
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

	public def searchBookingRequest2For_1TB_HBG(String sheetName,int rowNum, def bookingId) {
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

	public def bookingItemPriceBreakdownFor_1TB_HBG(String sheetName,int rowNum, def bookingId) {
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

	public def addBookingItemRequestFor_1TB_HBG(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId, def secondItemRefCount) {
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

	public def gtaAddBookingItemRequestFor_1TB_HBG(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId, def thirdItemRefCount) {
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

	public def gtaAddBookingItemRequestFor_1TB_HBGNewPAX(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('BookingItemCityCode', itemCity)
		dataMap.put('BookingItemCode', itemCode)
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
                                    <PaxId>4</PaxId>
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

	public def searchBookingItemFor_1TB_HBG(String sheetName,int rowNum, def bookingId) {
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

	public def chargeConditionBookingFor_1TB_HBG(String sheetName,int rowNum, def bookingId) {
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

	public def addBookingItemRequestFor_1TB_HBG_Security(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId, def fourthItemRefCount) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		dataMap.put('BookingItemCityCode', itemCity)
		dataMap.put('BookingItemCode', itemCode)
		dataMap.put('HotelRoomId', roomId)
		dataMap.put('ItemReference', fourthItemRefCount)
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
                                    <PaxId>4</PaxId>
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

	public def modifyBookingRequestFor_1TB_HBG(String sheetName,int rowNum, def bookingId) {
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
        <PaxNames>
            <PaxName PaxId = "1">JESS TEST</PaxName>
            <PaxName PaxId = "2">TETS 2</PaxName>
            <PaxName PaxId = "3">JESS3 TEST</PaxName>
            <PaxName PaxId = "4">TETS 3</PaxName>
            <PaxName PaxId = "5">TETS 4</PaxName>
        </PaxNames>
     </ModifyBookingRequest>
  </RequestDetails>
</Request>"""
		}
	}

	public def modifyBookingItemRequestFor_1TB_HBG(String sheetName,int rowNum, def bookingId, def roomId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
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

	public def cancelBookingItemRequestFor_1TB_HBG(String sheetName,int rowNum, def bookingId) {
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

	public def cancelBookingRequestFor_1TB_HBG(String sheetName,int rowNum, def bookingId) {
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
}