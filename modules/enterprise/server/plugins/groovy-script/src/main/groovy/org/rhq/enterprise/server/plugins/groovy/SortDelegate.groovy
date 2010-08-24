package org.rhq.enterprise.server.plugins.groovy

import static org.rhq.core.domain.util.PageOrdering.ASC
import static org.rhq.core.domain.util.PageOrdering.DESC
import org.rhq.core.domain.util.PageOrdering

class SortDelegate {

  String currentField

  def sortFields = []

  def propertyMissing(String name) {
    if (sortFields.size() > 0 && (name == 'asc' || name == 'desc')) {
      def sortField = sortFields[sortFields.size() - 1]
      sortField.order = getOrder(name)
      currentField = null
    }
    else {
      currentField = name
      sortFields << new SortField(name: currentField, order: ASC)
    }
    return this
  }

  PageOrdering getOrder(String order) {
    if (order == 'asc') {
      return ASC
    }
    else if (order == 'desc') {
      return DESC
    }
    else {
      throw new RHQScriptException("Unrecognized value for sort order: $order - possible values are <asc> and <desc>")
    }
  }

}
