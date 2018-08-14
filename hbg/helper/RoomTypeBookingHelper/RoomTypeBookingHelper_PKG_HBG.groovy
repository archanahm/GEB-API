package com.hbg.helper.RoomTypeBookingHelper

class RoomTypeBookingHelper_PKG_HBG extends RoomTypeBookingHelper{

	RoomTypeBookingHelper_PKG_HBG(String excelPath, String clientId){
		super(excelPath, clientId)
	}
	
	public def searchHotelPriceRequestFor_PKG_HBG(String sheetName,int rowNum, def city) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						    <Source>
						         <RequestorID 
						            Client = "${clientId}"
						            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
						            Password = "${dataMap.get('REQUEST.Password')}"/>
						        <RequestorPreferences
						            Currency ="jpy"
						            Language = "en">
						            <RequestMode>SYNCHRONOUS</RequestMode>
						        </RequestorPreferences>
						    </Source>
						    <RequestDetails>
						        <SearchHotelPriceRequest>
						        <ItemDestination DestinationCode = "${city}" DestinationType = "city"/>
						            <ItemCode></ItemCode>
						            <PeriodOfStay>
						                <CheckInDate>${getCheckInDate()}</CheckInDate>
						                <Duration><![CDATA[1]]></Duration>
						            </PeriodOfStay>
						            <IncludePriceBreakdown />
						            <IncludeChargeConditions DateFormatResponse="true"/>
						            <ShowPackageRates/>
						            <Rooms>
						                <Room
						                    Code = "TB"
						                    NumberOfCots = "0"
						                    NumberOfRooms = "1">
						                    <ExtraBeds/>
						                </Room>
						            </Rooms>
						        </SearchHotelPriceRequest>
						    </RequestDetails>
						</Request>"""
		}
	}
	
	public def hotelPriceBreakdownRequestFor_PKG_HBG(String sheetName,int rowNum, def itemCity, def itemCode, def roomCategory) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						    <Source>
						         <RequestorID 
						            Client = "${clientId}"
						            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
						            Password = "${dataMap.get('REQUEST.Password')}"/>
						        <RequestorPreferences
						            Currency ="EUR"
						            Language = "en">
						            <RequestMode>SYNCHRONOUS</RequestMode>
						        </RequestorPreferences>
						    </Source>
						    <RequestDetails>
						  <HotelPriceBreakdownRequest>
						   <City>${itemCity}</City>
					            <Item>${itemCode}</Item>
					            <PeriodOfStay>
					                <CheckInDate>${getCheckInDate()}</CheckInDate>
					                <Duration>1</Duration>
					            </PeriodOfStay>
						   <Rooms>
						    <Room 
								Code = "TB" 
								Id = "${roomCategory}" 
								NumberOfCots = "0" 
								NumberOfRooms = "1">
						     <ExtraBeds/>
						    </Room>
						   </Rooms>
						  </HotelPriceBreakdownRequest>
						 </RequestDetails>
						</Request>"""
		}
	}
	
	public def addBookingRequestFor_PKG_HBG(String sheetName,int rowNum, def bookingRef, def itemCity, def itemCode, def roomCategory) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						    <Source>
						         <RequestorID 
						            Client = "${clientId}"
						            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
						            Password = "${dataMap.get('REQUEST.Password')}"/>
						        <RequestorPreferences
						            Language = "en">
						            <RequestMode>SYNCHRONOUS</RequestMode>
						        </RequestorPreferences>
						    </Source>
						    <RequestDetails>
						  <AddBookingRequest Currency = "JPY">
						   <BookingName>JH_XSELL</BookingName>
					        <BookingReference>${bookingRef}</BookingReference>
					        <AgentReference>JESSTEST</AgentReference>
					        <BookingDepartureDate>${getCheckInDate()}</BookingDepartureDate>
						    <PassengerEmail>jessie.hampshire@gta-travel.com</PassengerEmail>
						   <PaxNames>
						    <PaxName PaxId = "1">JESS TEST</PaxName>
						    <PaxName PaxId = "2">TETS 2</PaxName>
						    <PaxName PaxId = "3">JESS3 TEST</PaxName>
						    <PaxName PaxId = "4">TETS 3</PaxName>
						    <PaxName PaxId = "5">TETS 4</PaxName>
						   </PaxNames>
						   <BookingItems>
						    <BookingItem ItemType = "hotel">
						     <ItemReference>1</ItemReference>
					        <ItemCity Code = "${itemCity}"/>
					        <Item Code = "${itemCode}"/>
						     <ItemRemarks><ItemRemark Code = "LA"/></ItemRemarks>
						     <HotelItem>
						      <AlternativesAllowed>false</AlternativesAllowed>
						      <PeriodOfStay>
    					       <CheckInDate>${getCheckInDate()}</CheckInDate>
						       <Duration>1</Duration>
						      </PeriodOfStay>
						      <HotelRooms>
						       <HotelRoom
						        Code = "TB"
						        ExtraBed = "false"
						        Id = "${roomCategory}"
						        SharingBedding = "false"
						        NumberOfCots = "0">
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
						</Request>
						"""
		}
	}
	
	public def searchBookingRequest1For_PKG_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						    <Source>
						         <RequestorID 
						            Client = "${clientId}"
						            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
						            Password = "${dataMap.get('REQUEST.Password')}"/>
						        <RequestorPreferences Language="en">
						            <RequestMode>SYNCHRONOUS</RequestMode>
						        </RequestorPreferences>
						    </Source>
						   <RequestDetails>
						        <SearchBookingRequest>
						            <BookingReference ReferenceSource="api">${bookingId}</BookingReference>
						            <ShowPaymentStatus>false</ShowPaymentStatus>
						        </SearchBookingRequest>
						    </RequestDetails>
						</Request>"""
		}
	}
	
	public def bookingItemPriceBreakdownFor_PKG_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
					            Password = "${dataMap.get('REQUEST.Password')}"/>
					        <RequestorPreferences
					            Language = "en">
					            <RequestMode>SYNCHRONOUS</RequestMode>
					        </RequestorPreferences>
					    </Source>
					   <RequestDetails>
					        <BookingItemPriceBreakdownRequest>
					            <BookingReference ReferenceSource = "api">${bookingId}</BookingReference>
					            <ItemReference>1</ItemReference>
					        </BookingItemPriceBreakdownRequest>
					    </RequestDetails>
					</Request>"""
		}
	}
	
	public def addBookingItemRequestFor_PKG_HBG(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						  <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
					            Password = "${dataMap.get('REQUEST.Password')}"/>
						    <RequestorPreferences Country = "IL" Currency = "EUR" Language = "en">
								<RequestMode>SYNCHRONOUS</RequestMode>
								</RequestorPreferences>
						  </Source>
						   <RequestDetails>
						        <AddBookingItemRequest>
						            <BookingReference ReferenceSource="api">${bookingId}</BookingReference>
						            <BookingItems>
						                <BookingItem ItemType="hotel">
						                    <ItemReference>2</ItemReference>
						                    <ItemCity Code="${itemCity}"/>
						                    <Item Code="${itemCode}"/>
						                    <ItemRemarks/>
						                    <HotelItem>
						                        <AlternativesAllowed>false</AlternativesAllowed>
						                        <PeriodOfStay>
						                            <CheckInDate>${getCheckInDate()}</CheckInDate>
						                            <CheckOutDate>${getCheckOutDate()}</CheckOutDate>
						                        </PeriodOfStay>
						                        <HotelRooms>
						                            <HotelRoom Code="TB" ExtraBed="false" Id="${roomId}" 
						                            NumberOfCots="0" >
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
	
	public def searchBookingItemFor_PKG_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
					            Password = "${dataMap.get('REQUEST.Password')}"/>
					        <RequestorPreferences
					            Language = "en">
					            <RequestMode>SYNCHRONOUS</RequestMode>
					        </RequestorPreferences>
					    </Source>
					   <RequestDetails>
					    <SearchBookingItemRequest>
					      <BookingReference ReferenceSource="api">${bookingId}</BookingReference>
					       <ShowPaymentStatus>false</ShowPaymentStatus>
					    </SearchBookingItemRequest>
					  </RequestDetails>
					</Request>"""
		}
	}
	
	public def chargeConditionBookingFor_PKG_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
					            Password = "${dataMap.get('REQUEST.Password')}"/>
					         <RequestorPreferences 
					            Country = "GB" 
					            Currency = "GBP"
					            Language = "en">
					            <RequestMode>SYNCHRONOUS</RequestMode>
					        </RequestorPreferences>
					    </Source>
					    <RequestDetails>
					        <SearchChargeConditionsRequest>
							   <DateFormatResponse/>   
							   <ChargeConditionsBookingItem>
							    <BookingReference ReferenceSource = "api">${bookingId}</BookingReference>
							    <ItemReference>1</ItemReference>
							   </ChargeConditionsBookingItem>
							 </SearchChargeConditionsRequest>
					    </RequestDetails>
					</Request>"""
		}
	}
	
	public def modifyBookingRequestFor_PKG_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						  <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
					            Password = "${dataMap.get('REQUEST.Password')}"/>
						    <RequestorPreferences Country = "IL" Currency = "EUR" Language = "en">
						      <RequestMode>SYNCHRONOUS</RequestMode>
						    </RequestorPreferences>
						  </Source>
						  <RequestDetails>
								<ModifyBookingRequest>
									<BookingReference ReferenceSource="api">${bookingId}</BookingReference>
									<AgentReference>JMB</AgentReference>
									<BookingDepartureDate>${getCheckInDate()}</BookingDepartureDate>
									<PaxNames>
									    <PaxName PaxId = "1">JESS TEST</PaxName>
									    <PaxName PaxId = "2">TETS 2</PaxName>
									    <PaxName PaxId = "3">JESS3 TEST</PaxName>
									    <PaxName PaxId = "4">TETS 3</PaxName>
									    <PaxName PaxId = "5">TETS 4</PaxName>
									  </PaxNames>
								</ModifyBookingRequest>
							</RequestDetails>
						</Request>
						"""
		}
	}
	
	public def modifyBookingItemRequestFor_PKG_HBG(String sheetName,int rowNum, def bookingId, def roomId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						 <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
					            Password = "${dataMap.get('REQUEST.Password')}"/>
						  <RequestorPreferences Country = "IL" Currency = "EUR" Language = "en">
						   <RequestMode>SYNCHRONOUS</RequestMode>
						  </RequestorPreferences>
						 </Source>
						 <RequestDetails>
						  
						  <ModifyBookingItemRequest>
						   <BookingReference ReferenceSource = "api">${bookingId}</BookingReference>
						   <BookingItems>
						    <BookingItem ItemType = "hotel">
						     <ItemReference>1</ItemReference>
						     <ItemRemarks/>
						     <HotelItem>
						      <AlternativesAllowed>false</AlternativesAllowed>
						      <PeriodOfStay>
						       <CheckInDate>${getCheckInNewDate()}</CheckInDate>
						       <CheckOutDate>${getCheckOutNewDate()}</CheckOutDate>
						      </PeriodOfStay>
						      <HotelRooms>
						       <HotelRoom Code = "TB" Id="${roomId}"  NumberOfCots = "0">
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
	
	public def cancelBookingItemRequestFor_PKG_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
					            Password = "${dataMap.get('REQUEST.Password')}"/>
					        <RequestorPreferences
					            Language = "en">
					            <RequestMode>SYNCHRONOUS</RequestMode>
					        </RequestorPreferences>
					    </Source>
					    <RequestDetails>
					   <CancelBookingItemRequest>
					      <BookingReference ReferenceSource = "api">${bookingId}</BookingReference>
					      <ItemReferences>
					        <ItemReference>2</ItemReference>
					      </ItemReferences>
					    </CancelBookingItemRequest>
					  </RequestDetails>
					</Request>"""
		}
	}
	public def cancelBookingRequestFor_PKG_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('REQUEST.Email_address')}"
					            Password = "${dataMap.get('REQUEST.Password')}"/>
						        <RequestorPreferences
						            Language = "en">
						            <RequestMode>SYNCHRONOUS</RequestMode>
						        </RequestorPreferences>
						    </Source>
						    <RequestDetails>
						   <CancelBookingRequest>
						      <BookingReference ReferenceSource = "api">${bookingId}</BookingReference>
						    </CancelBookingRequest>
						  </RequestDetails>
						</Request>
						"""
		}
	}
}
