package com.hbg.helper.RoomTypeBookingHelper

class RoomTypeBookingHelper_1DB_2Cot_HBG extends RoomTypeBookingHelper{

//	public String clientId

	
	RoomTypeBookingHelper_1DB_2Cot_HBG(String excelPath, String clientId){
		super(excelPath, clientId)
//		this.clientId = clientId
	}
	
	
	public def searchHotelPriceRequestFor_1DB_2Cot_HBG(String sheetName,int rowNum, def city) {
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
						        <SearchHotelPriceRequest>
						        <ItemDestination DestinationCode = "${city}" DestinationType = "city"/>
						            <ItemCode/>
						            <PeriodOfStay>
						                <CheckInDate>${getCheckInDate()}</CheckInDate>
						                <Duration>2</Duration>
						            </PeriodOfStay>
						            <IncludeChargeConditions DateFormatResponse="true"/>
						            <Rooms>
						                <Room
						                    Code = "DB"
						                    NumberOfCots = "2"
						                    NumberOfRooms = "1">
						                    <ExtraBeds/>
						                </Room>
						            </Rooms>
						        </SearchHotelPriceRequest>
						    </RequestDetails>
						</Request>"""
		}
	}
	
	public def hotelPriceBreakdownRequestFor_1DB_2Cot_HBG(String sheetName,int rowNum, def itemCity, def itemCode, def roomCategory) {
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
					        <HotelPriceBreakdownRequest>
					            <City>${itemCity}</City>
					            <Item>${itemCode}</Item>
					            <PeriodOfStay>
					                <CheckInDate>${getCheckInDate()}</CheckInDate>
					                <Duration>2</Duration>
					            </PeriodOfStay>
					            <Rooms>
					                <Room 
					                    Code = "DB" 
					                    Id = "${roomCategory}" 
					                    NumberOfCots = "2" 
					                    NumberOfRooms = "1">
					                    <ExtraBeds/>
					                </Room>
					            </Rooms>
					        </HotelPriceBreakdownRequest>
					 </RequestDetails>
					</Request>"""
		}
	}
	public def addBookingRequestFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingRef, def itemCity, def itemCode, def roomCategory) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return """<?xml version="1.0" encoding="UTF-8"?>
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
						        <AddBookingRequest Currency = "EUR">
						            <BookingName>JH_SNAITY</BookingName>
						            <BookingReference>${bookingRef}</BookingReference>
						            <AgentReference>JESSTEST</AgentReference>
						            <BookingDepartureDate>${getCheckInDate()}</BookingDepartureDate>
						            <PassengerEmail>jessie.hampshire@gta-travel.com</PassengerEmail>
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
						                     <PaxName PaxId = "7">TETS 12</PaxName>
						                <PaxName PaxId = "8">JESS8 TEST</PaxName>
						                <PaxName PaxId = "9">TETS 9</PaxName>
						            </PaxNames>
						            <BookingItems>
						                <BookingItem ItemType = "hotel">
						                    <ItemReference>1</ItemReference>
									        <ItemCity Code = "${itemCity}"/>
									        <Item Code = "${itemCode}"/>
						                    <ItemRemarks></ItemRemarks>
						                    <HotelItem>
						                        <AlternativesAllowed>false</AlternativesAllowed>
						                        <PeriodOfStay>
						                            <CheckInDate>${getCheckInDate()}</CheckInDate>
											        <CheckOutDate>${getCheckOutDate()}</CheckOutDate>
						                        </PeriodOfStay>
						                        <HotelRooms>
						                            <HotelRoom
						                                Code = "DB"
						                                ExtraBed = "false"
						                                Id = "${roomCategory}"
						                                NumberOfCots = "2">
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
	
	public def searchBookingRequest1For_1DB_2Cot_HBG(String sheetName,int rowNum) {
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
					        <SearchBookingRequest>
					            <BookingDateRange DateType = "departure">
					                <FromDate>${getToDate()}</FromDate>
					                <ToDate>${getFromDate()}</ToDate>
					            </BookingDateRange>
					            <EchoSearchCriteria>false</EchoSearchCriteria>
					            <ShowPaymentStatus>false</ShowPaymentStatus>
					        </SearchBookingRequest>
					    </RequestDetails>
					</Request>"""
		}
	}
	
	public def searchBookingRequest2For_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId) {
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
					        <SearchBookingRequest>
					            <BookingReference ReferenceSource="api">${bookingId}</BookingReference>
					            <ShowPaymentStatus>false</ShowPaymentStatus>
					        </SearchBookingRequest>
					    </RequestDetails>
					</Request>"""
		}
	}
	
	public def addMultipleBookingItemRequestFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId, 
																def itemCity1, def itemCode1, def roomId1, 
																def itemCity2,  def itemCode2, def roomId2) {
		
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
					return """<?xml version="1.0" encoding="UTF-8"?>
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
					        <AddBookingItemRequest>
					            <BookingReference ReferenceSource="api">${bookingId}</BookingReference>
					            <BookingItems>
					                <BookingItem ItemType="hotel">
					                    <ItemReference>2</ItemReference>
					                    <ItemCity Code="${itemCity1}"/>
					                    <Item Code="${itemCode1}"/>
					                    <ItemRemarks/>
					                    <HotelItem>
					                        <AlternativesAllowed>false</AlternativesAllowed>
					                        <PeriodOfStay>
					                            <CheckInDate>${getCheckInDate()}</CheckInDate>
					                            <CheckOutDate>${getCheckOutDate()}</CheckOutDate>
					                        </PeriodOfStay>
					                        <HotelRooms>
					                            <HotelRoom Code="TB" ExtraBed="true" Id="${roomId1}" 
					                            NumberOfCots="2" >
					                                <PaxIds>
					                                    <PaxId>7</PaxId>
					                                    <PaxId>8</PaxId>
					                                    <PaxId>6</PaxId>
					                                </PaxIds>
					                            </HotelRoom>
					                        </HotelRooms>
					                    </HotelItem>
					                </BookingItem>
					                <BookingItem ItemType="hotel">
					                    <ItemReference>3</ItemReference>
					                    <ItemCity Code="${itemCity2}"/>
					                    <Item Code="${itemCode2}"/>
					                    <ItemRemarks/>
					                    <HotelItem>
					                        <AlternativesAllowed>false</AlternativesAllowed>
					                        <PeriodOfStay>
					                            <CheckInDate>${getCheckInDate()}</CheckInDate>
					                            <CheckOutDate>${getCheckOutDate()}</CheckOutDate>
					                        </PeriodOfStay>
					                        <HotelRooms>
					                            <HotelRoom Code="SB" ExtraBed="false" Id="${roomId2}" 
					                            NumberOfCots="0" >
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
					</Request>
					"""
		}
	}
	public def bookingItemPriceBreakdownFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId) {
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
					        <BookingItemPriceBreakdownRequest>
					            <BookingReference ReferenceSource = "api">${bookingId}</BookingReference>
					            <ItemReference>2</ItemReference>
					        </BookingItemPriceBreakdownRequest>
					    </RequestDetails>
					</Request>"""
		}
	}

	public def searchBookingItemFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId) {
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
							<ItemReference>2</ItemReference>
					       <ShowPaymentStatus>false</ShowPaymentStatus>
					    </SearchBookingItemRequest>
					  </RequestDetails>
					</Request>"""
		}
	}
	public def chargeConditionBookingFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId) {
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
	public def modifyBookingRequestFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
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
									<BookingDepartureDate>${getDeptDate()}</BookingDepartureDate>
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
						</Request>
						"""
		}
	}
	public def modifyBookingItemRequestFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId, def roomId) {
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
						            Country = "IL"
						            Currency = "EUR"
						            Language = "en">
						            <RequestMode>SYNCHRONOUS</RequestMode>
						        </RequestorPreferences>
						    </Source>
						    <RequestDetails>
						        <ModifyBookingItemRequest>
						            <BookingReference ReferenceSource = "api">${bookingId}</BookingReference>
						            <BookingItems>
						                <BookingItem ItemType = "hotel">
						                    <ItemReference>2</ItemReference>
						                    <ItemRemarks/>
						                    <HotelItem>
						                        <AlternativesAllowed>false</AlternativesAllowed>
						                        <PeriodOfStay>
						                            <CheckInDate>${getDeptDate()}</CheckInDate>
						                            <Duration>3</Duration>
						                        </PeriodOfStay>
						                        <HotelRooms>
						                            <HotelRoom
						                                Code = "TB"
						                                ExtraBed = "true"
						                                Id = "${roomId}"
						                                NumberOfCots = "0">
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
						</Request>
						"""
		}
	}

	public def cancelBookingItemRequestFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId) {
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
					        <ItemReference>1</ItemReference>
					      </ItemReferences>
					    </CancelBookingItemRequest>
					  </RequestDetails>
					</Request>"""
		}
	}
	public def cancelBookingRequestFor_1DB_2Cot_HBG(String sheetName,int rowNum, def bookingId) {
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
