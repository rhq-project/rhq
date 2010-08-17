package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test
import groovy.mock.interceptor.MockFor
import org.rhq.enterprise.server.util.LookupUtil
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.PropertySimple

import static org.testng.Assert.*
import org.rhq.enterprise.server.plugin.pc.ControlResults

class ScriptRunnerTest {

  @Test
  void lookupManagerWhenADynamicManagerPropertyIsAccessed() {
    def fakeMgr = {}
    def lookupMock = new MockFor(LookupUtil)
    lookupMock.demand.getSubjectManager { fakeMgr }

    def runner = new ScriptRunner()
    def params = new Configuration()
    params.put(new PropertySimple('script', getScriptPath('access_mgr.groovy')))

    lookupMock.use {
      def result = runner.invoke('execute', params)
      assertScriptResultEquals(
          result,
          fakeMgr.class.name,
          "Expected the script to return the object it received from accessing a dynamic manager property"
      )
    }
  }

  String getScriptPath(String scriptName) {
    this.class.getResource(scriptName).toURI().path
  }

  def assertScriptResultEquals(controlResults, expected, msg) {
    assertEquals(getScriptResult(controlResults), expected, msg)
  }

  def getScriptResult(ControlResults results) {
    results.complexResults.getSimpleValue("results", null)
  }

}
