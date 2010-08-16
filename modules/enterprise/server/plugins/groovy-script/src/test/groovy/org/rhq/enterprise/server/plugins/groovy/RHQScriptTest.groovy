package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test
import org.rhq.org.rhq.core.domain.test.TestEntity

import static org.testng.Assert.*

class RHQScriptTest {

  @Test
  void buildCriteriaThatMatchesSpec() {
    def script = new RHQScript()
    def criteria = script.criteria(TestEntity) {
      filters = [id: 1]
      fetch = ['resourceTypes']
    }

    assertEquals(criteria.id, 1, "The criteria's id property is wrong")
    assertTrue(criteria.fetchResourceTypes)
  }

}
