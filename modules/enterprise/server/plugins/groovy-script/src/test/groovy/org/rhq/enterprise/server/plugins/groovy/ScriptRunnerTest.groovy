package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test
import groovy.mock.interceptor.MockFor
import org.rhq.enterprise.server.util.LookupUtil
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.PropertySimple

import static org.testng.Assert.*
import org.rhq.enterprise.server.plugin.pc.ControlResults
import org.rhq.core.domain.criteria.TestEntityCriteria
import org.testng.annotations.BeforeClass
import org.rhq.core.domain.test.TestEntity
import org.rhq.core.domain.util.PageOrdering
import org.testng.annotations.BeforeMethod
import org.reflections.Reflections
import javax.persistence.Entity
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.scanners.ResourcesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

class ScriptRunnerTest {

  ScriptRunner scriptRunner

  @BeforeMethod
  void setup() {
    scriptRunner = new ScriptRunner()
    scriptRunner.entityMap << [TestEntity: TestEntity.class]
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

  @Test
  void scanClasspath() {
    Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.getUrlsForPackagePrefix("org.rhq.core.domain"))
                .setScanners(new TypeAnnotationsScanner()));
    def classes = reflections.getTypesAnnotatedWith(Entity.class)

    println "Found ${classes.size()} entities"
    assertTrue(classes.size() > 5, "Found ${classes.size()} entities")
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
