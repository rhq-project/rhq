package org.rhq.enterprise.server.plugins.groovy

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class CriteriaGenerator {

  static Log log = LogFactory.getLog(CriteriaGenerator)

  def execute(CriteriaSpec spec) {
    def className = "org.rhq.core.domain.criteria.${spec.criteriaType.simpleName}Criteria"
    def clazz
    def criteria

    try {
      clazz = Class.forName(className)
    }
    catch (Exception e) {
      def msg = "Failed to load criteria class $className"
      log.warn msg, e
      throw new CriteriaGeneratorException(msg, e)
    }
    try {
      criteria = clazz.newInstance()
    }
    catch (Exception e) {
      def msg = "Failed to create instance of criteria class $className"
      log.warn msg, e
      throw new CriteriaGeneratorException(msg, e)
    }

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
