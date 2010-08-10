package org.rhq.enterprise.server.plugins.groovy

import org.rhq.enterprise.server.util.LookupUtil

class RHQScript extends Script {

  Object run() {
    super.run()
  }

  def propertyMissing(String name) {
    if (name.endsWith("Manager")) {
      def method = "get$name"
      return LookupUtil."$method"()
    }
    return null  // TODO Should we instead some sort of property not found exception
  }

}
