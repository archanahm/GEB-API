package com.hbg.logger

public class XSellLogger {

	static def logDebug = System.getProperty("logDebug")
	
	def static logDebug(def str){
		if (logDebug != null && logDebug == "true"){
			println str
		}
	}
}
