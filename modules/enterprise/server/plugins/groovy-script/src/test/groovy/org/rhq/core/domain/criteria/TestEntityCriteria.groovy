package org.rhq.core.domain.criteria

class TestEntityCriteria extends Criteria {

  Integer id

  String name

  boolean fetchResources

  boolean fetchResourceTypes

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

}
