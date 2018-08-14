package com.hbg.helper.RoomTypeBookingHelper

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

class RoomTypeBookingOneExtraOneCotHelper extends RoomTypeBookingHelper {

	RoomTypeBookingOneExtraOneCotHelper(String excelPath, String clientId){
		super(excelPath, clientId)
	}
	
	
	public def searchHotelPriceRequestFor1TB_1Extra1Bed_HBG(String sheetName,int rowNum, def city) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		println dataMap
		dataMap.put('Destination_Code', city)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
				<Request>
				    <Source>
				         <RequestorID 
				            Client = "${clientId}"
				            EMailAddress = "${dataMap.get('Email_address')}"
				            Password = "${dataMap.get('Password')}"/>
				        <RequestorPreferences
				            Language = "${dataMap.get('Language')}">
				            <RequestMode>${dataMap.get('Mode')}</RequestMode>
				        </RequestorPreferences>
				    </Source>
				    <RequestDetails>
				        <SearchHotelPriceRequest>
				        <ItemDestination DestinationCode = "${dataMap.get('Destination_Code')}" DestinationType = "${dataMap.get('Destination_Type')}"/>
				            <PeriodOfStay>
				                <CheckInDate>${getCheckInDate()}</CheckInDate>
				                <Duration>${dataMap.get('Duration')}</Duration>
				            </PeriodOfStay>
							<IncludePriceBreakdown/>
				            <Rooms>
				                <Room
				                    Code = "${dataMap.get('Room1_Code')}"
				                    NumberOfCots = "${dataMap.get('Room1_NumberOfCots')}"
				                    NumberOfRooms = "${dataMap.get('Room1_NumberOfRooms')}">
				                    <ExtraBeds><Age>${dataMap.get('Child1Age')}</Age></ExtraBeds>
				                </Room>
				            </Rooms>
				        </SearchHotelPriceRequest>
				    </RequestDetails>
				</Request>"""
		}
	}
	
	
	
	public def hotelPriceBreakdownRequestFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def itemCity, def itemCode, def roomCategory) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					        <RequestorPreferences
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
					                    Code = "TB" 
					                    Id = "${roomCategory}" 
					                    NumberOfCots = "${dataMap.get('Room1_NumberOfCots')}"
					                    NumberOfRooms = "${dataMap.get('Room1_NumberOfRooms')}">
					                    <ExtraBeds><Age>${dataMap.get('Child1Age')}</Age></ExtraBeds>
					                </Room>
					            </Rooms>
					        </HotelPriceBreakdownRequest>
					 </RequestDetails>
					</Request>"""
		}
	}
	
	public def addBookingRequestFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingRef, def itemCity, def itemCode, def roomCategory) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					        <RequestorPreferences
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
					        </RequestorPreferences>
					    </Source>
						<RequestDetails>
					    <AddBookingRequest Currency = "${dataMap.get('Currency')}">
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
								<PaxName PaxId = "5" ChildAge = "3" PaxType = "child">c 1</PaxName>
								<PaxName PaxId = "6" ChildAge = "6" PaxType = "child">c2 1</PaxName>
								<PaxName PaxId = "7">JESS7 TEST</PaxName>
					            <PaxName PaxId = "8">TETS 8</PaxName>
					        </PaxNames>
					   <BookingItems>
					        <BookingItem ItemType = "hotel">
					        <ItemReference>1</ItemReference>
					        <ItemCity Code = "${itemCity}"/>
					        <Item Code = "${itemCode}"/>
					        <ItemRemarks> </ItemRemarks>
					        <HotelItem>
					            <AlternativesAllowed>false</AlternativesAllowed>
					            <PeriodOfStay>
					                <CheckInDate>${getCheckInDate()}</CheckInDate>
					                <CheckOutDate>${getCheckOutDate()}</CheckOutDate>
					            </PeriodOfStay>
					            <HotelRooms>
					                <HotelRoom
					                    Code = "TB"
					                    ExtraBed = "true"
					                    Id = "${roomCategory}"
					                    NumberOfCots = "1">
					                    <PaxIds>
					                    		<PaxId>1</PaxId>
					                        <PaxId>2</PaxId>
					                        <PaxId>5</PaxId>
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
	
	public def searchBookingRequest1For_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					        <RequestorPreferences
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
	
	public def searchBookingRequest2For_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					        <RequestorPreferences
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
	
	public def bookingItemPriceBreakdownFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					        <RequestorPreferences
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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

	public def addBookingItemRequestFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					  <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					        <RequestorPreferences
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
					                            <HotelRoom 
					                                Code="TB" 
					                                ExtraBed="false" 
					                                Id="${roomId}" 
					                                NumberOfCots = "${dataMap.get('Room1_NumberOfCots')}">
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
	public def gtaAddBookingItemRequestFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId, def itemCity, def itemCode, def roomId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						  <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					         <RequestorPreferences 
					            Country = "GB" 
					            Currency = "GBP"
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
					        </RequestorPreferences>
						  </Source>
						   <RequestDetails>
						        <AddBookingItemRequest>
						            <BookingReference ReferenceSource="api">${bookingId}</BookingReference>
						            <BookingItems>
						                <BookingItem ItemType="hotel">
						                    <ItemReference>8</ItemReference>
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
						                            <HotelRoom Code="DB" ExtraBed="false" Id="${roomId}" 
						                            NumberOfCots="0" >
						                                <PaxIds>
						                                    <PaxId>7</PaxId>
						                                    <PaxId>8</PaxId>
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
	public def searchBookingItemFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					        <RequestorPreferences
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
	public def chargeConditionBookingFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					         <RequestorPreferences 
					            Country = "GB" 
					            Currency = "GBP"
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
	public def modifyBookingRequestFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						  <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
						    <RequestorPreferences Country = "${dataMap.get('Country')}" Currency = "${dataMap.get("Currency")}" Language = "${dataMap.get('Language')}">
						      <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
						                    <PaxName PaxId = "7">JESS7 TEST</PaxName>
						                <PaxName PaxId = "8">TETS 8</PaxName>
						            </PaxNames>
								</ModifyBookingRequest>
							</RequestDetails>
						</Request>
						"""
		}
	}
	public def modifyBookingItemRequestFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId, def roomId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			//return file
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
						        <RequestorPreferences
						            Country = "${dataMap.get('Country')}"
						            Currency = "${dataMap.get("Currency")}"
						            Language = "${dataMap.get('Language')}">
						            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
						                            <CheckInDate>${getDeptDate()}</CheckInDate>
						                            <Duration>3</Duration>
						                        </PeriodOfStay>
						                        <HotelRooms>
						                            <HotelRoom
						                                Code = "TB"
						                                ExtraBed = "true"
						                                Id = "${roomId}"
						                                NumberOfExtraBeds = "1"
						                                NumberOfCots = "1">
						                                <PaxIds>
						                                    <PaxId>3</PaxId>
						                                    <PaxId>4</PaxId>
						                                    <PaxId>6</PaxId>
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

	public def cancelBookingItemRequestFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return	"""<?xml version="1.0" encoding="UTF-8"?>
					<Request>
					    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
					        <RequestorPreferences
					            Language = "${dataMap.get('Language')}">
					            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
	public def cancelBookingRequestFor_1TB_1ExtraBed_1Cot_HBG(String sheetName,int rowNum, def bookingId) {
		dataMap=getSheetDataAsMap(sheetName,rowNum)
		if(dataMap && dataMap.get("Flag")){
			return	"""<?xml version="1.0" encoding="UTF-8"?>
						<Request>
						    <Source>
					         <RequestorID 
					            Client = "${clientId}"
					            EMailAddress = "${dataMap.get('Email_address')}"
					            Password = "${dataMap.get('Password')}"/>
						        <RequestorPreferences
						            Language = "${dataMap.get('Language')}">
						            <RequestMode>${dataMap.get('Mode')}</RequestMode>
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
