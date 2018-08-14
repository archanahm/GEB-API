/*
	This is the Geb configuration file.
	
	See: http://www.gebish.org/manual/current/configuration.html
*/





import geb.driver.CachingDriverFactory

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxBinary
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

waiting {
	timeout = 2
}

baseUrl  = "" 
environments {
	
	// run via â€œ./gradlew chromeTestâ€�
	// See: http://code.google.com/p/selenium/wiki/ChromeDriver

	def cachedDriver = CachingDriverFactory.clearCache()
	// run via â€œ./gradlew firefoxTestâ€�
	// See: http://code.google.com/p/selenium/wiki/FirefoxDriver
	firefox {
        if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            driver = { new FirefoxDriver() }
        } else {
			println("Non Windows OS found")
            FirefoxProfile profile = new FirefoxProfile();
            driver = { new FirefoxDriver(new FirefoxBinary(new File("/usr/sbin/firefox")), profile) }
        }
	}
	
	chrome {
	System.setProperty('webdriver.chrome.driver' ,System.getProperty("user.dir") + "\\src\\test\\resources\\chromedriver.exe")
//	System.setProperty('webdriver.chrome.driver' ,System.getProperty("user.dir") + "/src/test/resources/chromedriver.exe")
//	        System.setProperty('webdriver.chrome.driver', "/Users/josephwork/Documents/files/others/jars/chromedriver")
	//        System.setProperty('geb.driver', "org.openqa.selenium.chrome.ChromeDriver")
  
	ChromeOptions options = new ChromeOptions();
	options.addArguments("--start-maximized");
	options.addArguments("--no-sandbox")
	driver = { new ChromeDriver(options) }
   }

	win-ie {
		driver = {
//			new RemoteWebDriver(new URL("http://VUK79018"), DesiredCapabilities.internetExplorer())
			def driverFilePath = new File("src/test/resources/drivers/IEDriverServer_32.exe").absolutePath
			System.setProperty("webdriver.ie.driver", driverFilePath)
			new InternetExplorerDriver();
		}
	}
}

// To run the tests with all browsers just run â€œ./gradlew testâ€�