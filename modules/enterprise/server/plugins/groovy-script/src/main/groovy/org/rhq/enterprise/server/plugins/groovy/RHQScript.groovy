package org.rhq.enterprise.server.plugins.groovy

import org.rhq.enterprise.server.util.LookupUtil
import org.apache.commons.logging.LogFactory

class RHQScript extends Script {

  static def log = LogFactory.getLog(RHQScript)

  Map entityMap = [:]

  Object run() {
    super.run()
  }

  def propertyMissing(String name) {
    if (name.endsWith("Manager")) {
      def method = "get$name"
      try {
        return LookupUtil."$method"()
      }
      catch (MissingMethodException e) {
        log.warn("Could not find a manager SLSB under the name $name", e)
        throw new RHQScriptException("Unable to locate $name", e)
      }
    }

    if (entityMap.containsKey(name)) {
      return entityMap[name]
    }

    return null  // TODO Should we instead some sort of property not found exception
  }

  def criteria(Class entityClass, Closure specifyCriteria) {
    def criteriaSpec = new CriteriaSpec(entityClass)
    specifyCriteria.resolveStrategy = Closure.DELEGATE_FIRST
    specifyCriteria.delegate = criteriaSpec
    specifyCriteria()

    return new CriteriaGenerator().execute(specifyCriteria.delegate)

  }

}
