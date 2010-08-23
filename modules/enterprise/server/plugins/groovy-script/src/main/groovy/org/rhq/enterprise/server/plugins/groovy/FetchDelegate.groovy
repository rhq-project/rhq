package org.rhq.enterprise.server.plugins.groovy

class FetchDelegate {

  List propertiesToFetch = []

  def propertyMissing(String name) {
    propertiesToFetch << name
    return this
  }

}
