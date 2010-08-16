package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test

import static org.testng.Assert.*

import org.rhq.core.domain.criteria.TestEntityCriteria
import org.rhq.org.rhq.core.domain.test.TestEntity
import org.rhq.org.rhq.core.domain.test.TestEntityWithoutCriteria

class CriteriaGeneratorTest {

  @Test
  void theCriteriaTypeShouldMatchTheSpecifiedEntity() {
    def spec = new CriteriaSpec(TestEntity)
    def criteria = new CriteriaGenerator().execute(spec)

    assertTrue(criteria instanceof TestEntityCriteria, "Expected an instance of ${TestEntityCriteria.name}")
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

    def criteria = new CriteriaGenerator().execute(spec)

    assertEquals(criteria.id, expectedId, 'The <id> filter is wrong')
    assertEquals(criteria.name, expectedName, 'The <name> filter is wrong')
  }

  @Test
  void setTheFetchFields() {
    def spec = new CriteriaSpec(TestEntity)
    spec.fetch = ['resources', 'resourceTypes']

    def criteria = new CriteriaGenerator().execute(spec)

    assertTrue(criteria.fetchResources, 'Expected fetchResources to be <true>')
    assertTrue(criteria.fetchResourceTypes, 'Expected fetchResourceTypes to be <false>')
  }

}
