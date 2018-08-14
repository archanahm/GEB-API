package com.hbg.helper.RequestHelper

class HotelDTO {

	def itemCode
	def itemCity
	def itemCityName
	def itemName
	def categoryId
	def bookingId
	def bookingReference
	def mealBasisCode
	def mealBasisText
	def starRating
	def sharingBedding
	def roomConfirmationCode
	def roomConfirmationText
	def roomDescription
	
	def hotelNode
	def roomCategoryNode
	
	@Override
	public String toString() {
		return "HotelDTO [itemCode=" + itemCode + ", itemCity=" + itemCity + ", itemCityName=" + itemCityName+ ", itemName=" + itemName + ", categoryId=" + categoryId + "]";
	}
	
	
}
