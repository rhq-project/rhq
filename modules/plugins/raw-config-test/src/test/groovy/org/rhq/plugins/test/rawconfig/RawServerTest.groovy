package org.rhq.plugins.test.rawconfig

import org.testng.annotations.Test
import org.rhq.core.domain.configuration.RawConfiguration

class RawServerTest {

  @Test
  void testPersistRawConfiguration() {
    def rawFileName = "raw-test.txt"
    def rawFile = new File(confDir, rawFileName)

    def contents = "hello world"

    rawFile << contents

    def updatedContents = "hello world from red hat"

    def rawConfig = new RawConfiguration(path: rawFile.absolutePath, contents: updatedContents.bytes)

    def rawServer = new RawServer()
    rawServer.persistRawConfiguration(rawConfig) 
  }

  String getConfDir() {
    getClass().getResource(".").toURI().path
  }

}