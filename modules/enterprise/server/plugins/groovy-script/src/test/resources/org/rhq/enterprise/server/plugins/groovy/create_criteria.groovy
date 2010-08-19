package org.rhq.enterprise.server.plugins.groovy

testCriteria = criteria(TestEntity) {
  filters = [
      id:   1,
      name: 'Test'
  ]
  fetch = ['resources']
  sort {
    id.desc
    name.desc
  }
  caseSensitive = true
  strict = true
}

return testCriteria.toString()