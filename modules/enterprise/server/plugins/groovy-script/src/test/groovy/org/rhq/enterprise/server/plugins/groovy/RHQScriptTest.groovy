package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test
import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertTrue
import org.rhq.core.domain.test.TestEntity

class RHQScriptTest {

  @Test
  void buildCriteriaThatMatchesSpec() {
    def script = new RHQScript()
    def criteria = script.criteria(TestEntity) {
      filters = [id: 1]
      fetch = { resourceTypes }
    }

    assertEquals(criteria.id, 1, "The criteria's id property is wrong")
    assertTrue(criteria.fetchResourceTypes)
  }

}
