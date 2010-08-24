package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test

import static org.testng.Assert.*

import org.rhq.core.domain.criteria.TestEntityCriteria
import org.rhq.core.domain.test.TestEntityWithoutCriteria
import org.rhq.core.domain.test.TestEntity
import org.testng.annotations.BeforeMethod
import groovy.mock.interceptor.MockFor
import org.rhq.enterprise.server.util.LookupUtil

class CriteriaGeneratorTest {

  def mockLookupUtil

  @BeforeMethod
  void setup() {
    def fakeMgr = {}

    mockLookupUtil = new MockFor(LookupUtil)
    mockLookupUtil.demand.getTestEntityManager(0..1) { fakeMgr }
  }

  @Test
  void theCriteriaTypeShouldMatchTheSpecifiedEntity() {
    mockLookupUtil.use {
      def spec = new CriteriaSpec(TestEntity)
      def criteria = new CriteriaGenerator().execute(spec)

      assertTrue(criteria instanceof TestEntityCriteria, "Expected an instance of ${TestEntityCriteria.name}")
    }
  }

  @Test(expectedExceptions = [CriteriaGeneratorException])
  void throwExceptionWhenCriteriaTypeDoesNotExist() {
    def spec = new CriteriaSpec(TestEntityWithoutCriteria)
    def criteria = new CriteriaGenerator().execute(spec)    
  }

  @Test
  void setTheFilterFields() {
    def expectedId = 1
    def expectedName = 'Test Entity'

    def spec = new CriteriaSpec(TestEntity)
    spec.filters = [
        id:   expectedId,
        name: expectedName
    ]

    mockLookupUtil.use {
      def criteria = new CriteriaGenerator().execute(spec)

      assertEquals(criteria.id, expectedId, 'The <id> filter is wrong')
      assertEquals(criteria.name, expectedName, 'The <name> filter is wrong')
    }
  }

  @Test
  void setTheFetchFields() {
    def spec = new CriteriaSpec(TestEntity)
    spec.fetch {
      resources
      resourceTypes
    }

    mockLookupUtil.use {
      def criteria = new CriteriaGenerator().execute(spec)

      assertTrue(criteria.fetchResources, 'Expected fetchResources to be <true>')
      assertTrue(criteria.fetchResourceTypes, 'Expected fetchResourceTypes to be <false>')
    }
  }

  @Test
  void setTheSortFields() {
    def spec = new CriteriaSpec(TestEntity)
    spec.sort {
      name.desc
      id.asc
    }

    mockLookupUtil.use {
      def criteria = new CriteriaGenerator().execute(spec)

      assertEquals(criteria.orderingFieldNames, ['sortName', 'sortId'])
    }
  }

  @Test
  void setCaseSensitiveFlag() {
    def spec = new CriteriaSpec(TestEntity)
    spec.caseSensitive = true

    mockLookupUtil.use {
      def criteria = new CriteriaGenerator().execute(spec)

      assertTrue(criteria.caseSensitive, "Expected the <caseSensitive> property to be set to true")
    }
  }

  @Test
  void setStrictFlag() {
    def spec = new CriteriaSpec(TestEntity)
    spec.strict = true

    mockLookupUtil.use {
      def criteria = new CriteriaGenerator().execute(spec)

      assertTrue(criteria.strict, "Expected the <strict> property to be set to true")
    }
  }

}
