package org.rhq.enterprise.server.plugins.groovy

import groovy.mock.interceptor.MockFor
import javax.persistence.Entity
import org.reflections.Reflections
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.PropertySimple
import org.rhq.core.domain.criteria.TestEntityCriteria
import org.rhq.core.domain.util.PageOrdering
import org.rhq.enterprise.server.plugin.pc.ControlResults
import org.rhq.enterprise.server.util.LookupUtil
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertTrue

class ScriptRunnerTest {

  ScriptRunner scriptRunner

  @BeforeMethod
  void setup() {
    scriptRunner = new ScriptRunner()
  }

  @Test
  void populateEntityMap() {
    scriptRunner.entityPackagePrefix = 'org.rhq.core.domain.test'
    scriptRunner.initialize(null)

    assertEquals(scriptRunner.entityMap.size(), 2, "Expected to entityMap to contain two entries")
    assertTrue(scriptRunner.entityMap.containsKey('TestEntity'), "Expected to find <TestEntity> in entity map")
    assertTrue(scriptRunner.entityMap.containsKey('TestEntityWithoutCriteria'),
        "Expected to find <TestEntityWithoutCriteria> in entity map")
  }

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

  @Test(enabled = false)
  void createCriteriaAccordingToSpec() {
    def expectedCriteria =  new TestEntityCriteria()
    expectedCriteria.id = 1
    expectedCriteria.name = 'Test'
    expectedCriteria.strict = true
    expectedCriteria.caseSensitive = true
    expectedCriteria.fetchResources = true
    expectedCriteria.addSortId(PageOrdering.DESC)
    expectedCriteria.addSortName(PageOrdering.DESC)

    def result = executeScript('create_criteria.groovy')

    assertScriptResultEquals(result, expectedCriteria.toString(), 'Failed to generate criteria correctly')
  }

  def executeScript(String script) {
    def params = new Configuration()
    params.put(new PropertySimple('script', getScriptPath(script)))
    
    return scriptRunner.invoke('execute', params)
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
