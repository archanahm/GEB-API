def tagName = System.getProperty( "tagName")
if (tagName){
	tagName = 'com.kuoni.qa.automation.spock.annotation.' + tagName
	runner {	
		include Class.forName(tagName)
	}
}