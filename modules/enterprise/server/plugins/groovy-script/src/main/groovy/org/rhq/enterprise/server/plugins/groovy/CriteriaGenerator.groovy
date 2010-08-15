package org.rhq.enterprise.server.plugins.groovy

class CriteriaGenerator {

  def execute(CriteriaDelegate details) {
    def clazz = Class.forName("org.rhq.core.domain.criteria.${details.criteriaType.simpleName}Criteria")
    def criteria = clazz.newInstance()

    details.filters.each { key, value ->
      def filterName = "addFilter${capitalize(key)}"
      criteria."$filterName"(value)
    }

    return criteria
  }

  String capitalize(String string) {
    if (string.length() == 1) {
      return string.toUpperCase()
    }
    return string[0].toUpperCase() + string[1..string.length() - 1]
  }

}
