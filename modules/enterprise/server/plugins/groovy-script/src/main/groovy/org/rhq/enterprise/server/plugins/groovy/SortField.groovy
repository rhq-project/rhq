package org.rhq.enterprise.server.plugins.groovy

import org.rhq.core.domain.util.PageOrdering

class SortField {

  String name

  PageOrdering order

  String toString() {
    "SortField[name: $name, order: $order]"
  }

}
