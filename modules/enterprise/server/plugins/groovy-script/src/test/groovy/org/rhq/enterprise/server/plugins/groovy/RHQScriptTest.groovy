package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test
import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertTrue
import org.rhq.core.domain.test.TestEntity
import org.rhq.enterprise.server.util.LookupUtil
import groovy.mock.interceptor.MockFor
import org.rhq.enterprise.server.resource.ResourceManagerLocal
import org.rhq.core.domain.auth.Subject
import org.rhq.core.domain.criteria.ResourceCriteria
import org.rhq.core.domain.resource.Resource

class RHQScriptTest {

  @Test
  void buildCriteriaThatMatchesSpec() {
    def fakeMgr = {}
    def lookupUtil = new MockFor(LookupUtil)
    lookupUtil.demand.getTestEntityManager(1..1) { fakeMgr }

    lookupUtil.use {
      def script = new RHQScript()
      def criteria = script.criteria(TestEntity) {
        filters = [id: 1]
        fetch { resourceTypes }
      }

      assertEquals(criteria.id, 1, "The criteria's id property is wrong")
      assertTrue(criteria.fetchResourceTypes)  
    }
  }

  @Test(enabled = false)
  void addExecMethodToCriteria() {
    def resourceMgr = new MockFor(ResourceManagerLocal)
    def mockLookupUtil = new MockFor(LookupUtil)

    mockLookupUtil.demand.getResourceManager(1..1) { resourceMgr }
    resourceMgr.demand.findResourcesByCriteria { subject, resourceCriteria -> [] }

    def criteria

    mockLookupUtil.use {
      def script = new RHQScript()
      criteria = script.criteria(Resource) {
        filters = [id: 1]
        fetch { resourceType }
      }
    }

    resourceMgr.use {
      criteria.exec(new Subject()) { }
    }
  }

}
