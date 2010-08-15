package org.rhq.core.domain.criteria

class TestEntityCriteria extends Criteria {

  Integer id

  String name

  Class<?> getPersistentClass() {
    TestEntityCriteria
  }

  void addFilterId(Integer id) {
    this.id = id
  }

  void addFilterName(String name) {
    this.name = name
  }

}
