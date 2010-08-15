package org.rhq.enterprise.server.plugins.groovy

class CriteriaDelegate {

  Map filters = [:]

  Class criteriaType

  CriteriaDelegate(Class criteriaType) {
    this.criteriaType = criteriaType
  }

}
