criteria = new ResourceTypeCriteria();
resourceTyes = ResourceTypeManager.findResourceTypesByCriteria(criteria);

assertNotNull(resourceTypes, 'Expected findResourceTypesByCriteria() to return a non-null result');
assertTrue(resourceTypes.size() > 0, 'Expected findResourceTypesByCriteria() to return a non-empty result set');
