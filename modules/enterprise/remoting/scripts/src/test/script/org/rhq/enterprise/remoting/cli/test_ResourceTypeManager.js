rhq.login('rhqadmin', 'rhqadmin');

bindings = context.getBindings(100);
println(bindings.keySet());

//println(ResourceManager);

criteria = new ResourceTypeCriteria();
resourceTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);

assertNotNull(resourceTypes, 'Expected findResourceTypesByCriteria() to return a non-null result');
assertTrue(resourceTypes.size() > 0, 'Expected findResourceTypesByCriteria() to return a non-empty result set');

for (i = 0; i < resourceTypes.size(); ++i) {
    resourceType = resourceTypes.get(i);
    
    assertNotNull(ResourceTypeManager.getResourceTypeById(resourceType.id),
        'Expected getResourceTypeById to a return a ResourceType for id ' + resourceType.id);

    assertNotNull(ResourceTypeManager.getResourceTypeByNameAndPlugin(resourceType.name, resourceType.plugin),
        'Expected getResourceTypeByNameAndPLugin to return a ResourceType for ' + resourceType.name + ', ' +
        resourceType.plugin);
}

logout();
