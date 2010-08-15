package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test

import static org.testng.Assert.*

import org.rhq.core.domain.criteria.TestEntityCriteria
import org.rhq.org.rhq.core.domain.test.TestEntity

class CriteriaGeneratorTest {

  @Test
  void theCriteriaTypeShouldMatchTheSpecifiedEntity() {
//    def details = new CriteriaDelegate(Resource.class)
    def details = new CriteriaDelegate(TestEntity)
    def criteria = new CriteriaGenerator().execute(details)

    assertTrue(criteria instanceof TestEntityCriteria, "Expected an instance of ${TestEntityCriteria.name}")
  }

  @Test
  void setTheCriteriaFilters() {
    def expectedId = 1
    def expectedName = 'Test Entity'

    def details = new CriteriaDelegate(TestEntity)
    details.filters = [
        id:   expectedId,
        name: expectedName
    ]

    def criteria = new CriteriaGenerator().execute(details)

    assertEquals(criteria.id, expectedId, 'The <id> filter is wrong')
    assertEquals(criteria.name, expectedName, 'The <name> filter is wrong')
  }

}
