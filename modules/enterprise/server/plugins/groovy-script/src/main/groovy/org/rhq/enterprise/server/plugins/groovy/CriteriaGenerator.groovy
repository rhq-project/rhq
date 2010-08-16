package org.rhq.enterprise.server.plugins.groovy

class CriteriaGenerator {

  def execute(CriteriaSpec spec) {
    def clazz = Class.forName("org.rhq.core.domain.criteria.${spec.criteriaType.simpleName}Criteria")
    def criteria = clazz.newInstance()

    spec.filters.each { key, value ->
      def filterName = "addFilter${capitalize(key)}"
      criteria."$filterName"(value)
    }

    spec.fetch.each { criteria."fetch${capitalize(it)}"(true) }

    return criteria
  }

  String capitalize(String string) {
    if (string.length() == 1) {
      return string.toUpperCase()
    }
    return string[0].toUpperCase() + string[1..string.length() - 1]
  }

}
