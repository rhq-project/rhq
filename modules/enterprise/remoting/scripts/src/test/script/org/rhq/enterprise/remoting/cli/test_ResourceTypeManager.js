rhq.login('rhqadmin', 'rhqadmin');

criteria = new ResourceTypeCriteria();
resourceTypes = null;

for (func in this) {
    if (func.indexOf('test') == 0) {
        this[func]();
    }
}

//testFindUnfiltered();
//testFilterByPluginName();

logout();

function testFindUnfiltered() {
    resourceTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);

    assertResourceTypesFound();

    for (i = 0; i < resourceTypes.size(); ++i) {
        resourceType = resourceTypes.get(i);
    
        assertNotNull(ResourceTypeManager.getResourceTypeById(resourceType.id),
            'Expected getResourceTypeById to a return a ResourceType for id ' + resourceType.id);

        assertNotNull(ResourceTypeManager.getResourceTypeByNameAndPlugin(resourceType.name, resourceType.plugin),
            'Expected getResourceTypeByNameAndPLugin to return a ResourceType for ' + resourceType.name + ', ' +
            resourceType.plugin);
    }
}

function testFilterByPluginName() {
    criteria.addFilterPluginName('PerfTest');
    resourceTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);

    assertResourceTypesFound('Failed to find ResourceTypes when filtering by plugin name');

    for (i = 0; i < resourceTypes.size(); ++i) {
        resourceType = resourceTypes.get(i);
    
        assertEquals(resourceType.plugin, 'PerfTest', 'Expected only ResourceTypes from the PerfTest plugin');
    }
}

function testFilterByNameAndPluginName() {
    criteria = new ResourceTypeCriteria();
    criteria.addFilterPluginName('PerfTest');
    criteria.addFilterName('service-alpha');

    resourceTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);

    assertResourceTypesFound('Failed to find ResourceTypes when filtering by name and by plugin name');
    
    importClass(java.lang.Integer);
    assertEquals(new Integer(resourceTypes.size()), new Integer(1), 'Expected to get back one ResourceType when filtering by name and by plugin name');

    resourceType = resourceTypes.get(0);

    assertEquals(resourceType.name, 'service-alpha');
    assertEquals(resourceType.plugin, 'PerfTest');
}

function assertResourceTypesFound(msg) {
    if (msg == undefined) {
        msg = '';
    }
    else {
        msg = msg + ' - ';
    }
    assertNotNull(resourceTypes, msg + 'Expected findByResourceTypesByCriteria() to a reutrn a non-null result');
    assertTrue(resourceTypes.size() > 0, msg + 'Expected findResourceTypesByCriteria() to return a non-empty results set');
}
