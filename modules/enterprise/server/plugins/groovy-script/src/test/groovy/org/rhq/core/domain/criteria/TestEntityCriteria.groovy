package org.rhq.core.domain.criteria

import org.rhq.core.domain.util.PageOrdering

class TestEntityCriteria extends Criteria {

  Integer id

  String name

  boolean fetchResources

  boolean fetchResourceTypes

  PageOrdering sortId

  PageOrdering sortName

  TestEntityCriteria() {
    sortOverrides.put('sortId', 'id')
    sortOverrides.put('sortName', 'name')
  }

  Class<?> getPersistentClass() {
    TestEntityCriteria
  }



  void addFilterId(Integer id) {
    this.id = id
  }

  void addFilterName(String name) {
    this.name = name
  }

  void fetchResources(boolean fetch) {
    fetchResources = fetch
  }

  void fetchResourceTypes(boolean fetch) {
    fetchResourceTypes = fetch
  }

  void addSortId(PageOrdering order) {
    addSortField('id')
    sortId = order
  }

  void addSortName(PageOrdering order) {
    addSortField('name')
    sortName = order
  }

  def String toString() {
    "TestEntityCriteria[id: $id, " +
                       "name: $name, ".toString() +
                       "fetchResources: $fetchResources, " +
                       "fetchResourceTypes: $fetchResourceTypes, " +
                       "caseSensitive: $caseSensitive, " +
                       "strict: $strict, " +
                       "sortFields: $orderingFieldNames]"
  }


}
