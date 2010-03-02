package org.guest

import org.testng.xml.XmlSuite
import org.testng.TestNG

scriptsClassLoader.addURL(new File(scriptsDir).toURI().toURL())

def testNG = new TestNG()
testNG.commandLineSuite = new XmlSuite(fileName: suiteFile)
testNG.outputDirectory = outputDir
testNG.run()