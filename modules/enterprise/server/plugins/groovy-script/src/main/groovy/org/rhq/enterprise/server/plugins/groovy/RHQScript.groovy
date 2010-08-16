package org.rhq.enterprise.server.plugins.groovy

import org.rhq.enterprise.server.util.LookupUtil
import org.rhq.core.domain.resource.Resource
import org.rhq.core.domain.resource.ResourceType

class RHQScript extends Script {

  def entityMap = [
      Resource:     Resource.class,
      ResourceType: ResourceType.class
  ]

  Object run() {
    super.run()
  }

  def propertyMissing(String name) {
    if (name.endsWith("Manager")) {
      def method = "get$name"
      return LookupUtil."$method"()
    }

    if (entityMap.containsKey(name)) {
      return entityMap[name]
    }

    return null  // TODO Should we instead some sort of property not found exception
  }

  def criteria(Class entityClass, Closure specifyCriteria) {
    def criteriaSpec = new CriteriaSpec(entityClass)
    specifyCriteria.delegate = criteriaSpec
    specifyCriteria()

    return new CriteriaGenerator().execute(criteriaSpec)

  }

}
