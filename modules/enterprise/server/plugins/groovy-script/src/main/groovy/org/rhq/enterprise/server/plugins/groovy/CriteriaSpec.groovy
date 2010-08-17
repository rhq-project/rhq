package org.rhq.enterprise.server.plugins.groovy

class CriteriaSpec {

  Class criteriaType

  Map filters = [:]

  List fetch = []

  CriteriaSpec(Class criteriaType) {
    this.criteriaType = criteriaType
  }

  //def propertyMissing

}
