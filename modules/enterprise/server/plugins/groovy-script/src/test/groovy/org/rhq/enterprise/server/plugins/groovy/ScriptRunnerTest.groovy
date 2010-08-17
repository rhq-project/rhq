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

    lookupMock.use {
      def result = executeScript('access_mgr.groovy')
      assertScriptResultEquals(
          result,
          fakeMgr.class.name,
          "Expected the script to return the object it received from accessing a dynamic manager property"
      )
    }
  }

  @Test(expectedExceptions = [RHQScriptException])
  void throwExceptionWhenManagerAccessedDoesNotExist() {
    executeScript('access_nonexistent_mgr.groovy')  
  }

  def executeScript(String script) {
    def runner = new ScriptRunner()
    def params = new Configuration()
    params.put(new PropertySimple('script', getScriptPath(script)))
    
    return runner.invoke('execute', params)
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
