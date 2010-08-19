package org.rhq.enterprise.server.plugins.groovy

import static org.rhq.core.domain.util.PageOrdering.ASC
import static org.rhq.core.domain.util.PageOrdering.ASC

class CriteriaSpec {

  Class criteriaType

  Map filters = [:]

  List fetch = []

  List sortFields = []

  Boolean caseSensitive = false

  Boolean strict = false

  CriteriaSpec(Class criteriaType) {
    this.criteriaType = criteriaType
  }

  def sort(Closure sortSpec) {
    sortSpec.resolveStrategy = Closure.DELEGATE_ONLY
    sortSpec.delegate = new SortDelegate()
    sortSpec()
    sortFields.addAll(sortSpec.delegate.sortFields)
  }

  def fetch(Closure fetchSpec) {
    fetchSpec.resolveStrategy = Closure.DELEGATE_ONLY
    fetchSpec.delegate = new FetchDelegate()
    fetchSpec()
    fetch.addAll(fetchSpec.delegate.propertiesToFetch)
  }

}
