package com.hbg.test

import spock.lang.Specification

import com.kuoni.finance.automation.xml.util.ConfigProperties

abstract class XSellBaseTest extends Specification implements ClientPack{

	
	static def logDebug = System.getProperty("logDebug")
	static def platform = System.getProperty("platform","nova")
	static def env = System.getProperty("testEnv")
	static final String url = ConfigProperties.getValue("AuthenticationURL_"+platform+"_"+env)
	static final String CITY = ConfigProperties.getValue("HBG_City_List")
	static final String legacyCities = ConfigProperties.getValue("HBG_City_List_Legacy")
	List hbgCities = CITY.split(",")
	List hbgCitiesLegacy = legacyCities.split(",")
	
	static final String AREA = ConfigProperties.getValue("HBG_Area_List")
	static final String legacyAreas = ConfigProperties.getValue("Legacy_Area_List")
	List hbgAreas = AREA.split(",")
	List hbgAreasLegacy = legacyAreas.split(",")

	def static logDebug(def str){
		if (logDebug != null && logDebug == "true"){
			println str
		}
	}

	def List numOfCitiesList(int requiredNumCity){

		def citylist=[]
		def r = new Random()
		List cities = hbgCities
		if(platform.equalsIgnoreCase("legacy")) {
			cities = hbgCitiesLegacy
		}

		if(requiredNumCity<=cities.size()){
			requiredNumCity.times {
				//citylist.add(hbgCities.get(r.nextInt(cnt)))
				int randomIndex = r.nextInt(cities.size())
				citylist.add(cities.get(randomIndex))
				cities.remove(randomIndex)
			}
		} else {
			citylist.addAll(cities)
		}
		
		printList(citylist, "City")
		return citylist
	}
	
	def List numOfCitiesListForSearch(int requiredNumCity){
		def citylist=[]
		def r = new Random()
		List cities = hbgCities
		if(platform.equalsIgnoreCase("legacy")) {
			cities = hbgCitiesLegacy
		}
		requiredNumCity = cities.size()
		if(requiredNumCity<=cities.size()){
			requiredNumCity.times {
				//citylist.add(hbgCities.get(r.nextInt(cnt)))
				int randomIndex = r.nextInt(cities.size())
				citylist.add(cities.get(randomIndex))
				cities.remove(randomIndex)
			}
		} else {
			citylist.addAll(cities)
		}
		
		def finalList = []
		finalList.add("LON")
		finalList.add("PAR")
		finalList.add("ROM")
		finalList.add("KOWL")

		
		for (int i=0;i<citylist.size();i++){
			if (!finalList.contains(citylist[i])){
				finalList.add(citylist[i])
			}
		}

		printList(finalList, "City")
		return finalList
	}
	
	def List numOfAreasList(int requiredNumArea){
		def arealist=[]
		def r = new Random()
		List areas = hbgAreas
		if(platform.equalsIgnoreCase("legacy")) {
			areas = hbgAreasLegacy
		}

		if(requiredNumArea<=areas.size()){
			requiredNumArea.times {
				int randomIndex = r.nextInt(areas.size())
				arealist.add(areas.get(randomIndex))
				areas.remove(randomIndex)
			}
		} else {
			arealist.addAll(areas)
		}
		printList(arealist, "Area")
		return arealist
	}
	
	def String printList(List list, String entityName){
		String entitiesStr = null
		for (int i=0;i<list.size();i++){
			if (entitiesStr == null){
				entitiesStr = list.get(i)
			}else{
				entitiesStr = entitiesStr + "," + list.get(i)
			}
		}
		
		println entityName + " Codes : "+entitiesStr
	}
}
