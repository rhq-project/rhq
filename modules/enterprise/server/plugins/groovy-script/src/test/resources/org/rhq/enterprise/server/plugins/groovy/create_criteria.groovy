package org.rhq.enterprise.server.plugins.groovy

testCriteria = criteria(TestEntity) {
  filters = [
      id:   1,
      name: 'Test'
  ]
  fetch = ['resources']
}

return testCriteria.toString()