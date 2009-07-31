/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

rhq.login('rhqadmin', 'rhqadmin');

criteria = new ResourceCriteria();
resources = null;

for (func in this) {
    if (func.indexOf('test') == 0) {
        this[func]();
    }
}

function testFindUnfiltered() {
    resources = ResourceManager.findResourcesByCriteria(criteria);

    assertResourcesFound();
}

function testFilterByResourceTypeNameAndPluginName() {
    resourceType = ResourceTypeManager.getResourceTypeByNameAndPlugin("service-a", "PerfTest");

    criteria = new ResourceCriteria();
    criteria.addFilterResourceTypeName(resourceType.name);
    criteria.addFilterPluginName(resourceType.plugin);

    resources = ResourceManager.findResourcesByCriteria(criteria);

    assertResourcesFound("Failed to find resources when filtering by resource type name and plugin name");

    for (i = 0; i < resources.size(); ++i) {
        resource = resources.get(i);

        assertEquals(resource.resourceType, resourceType, "Expected Resource to have ResourceType, " + resourceType);
    }
}

function assertResourcesFound(msg) {
    if (msg == undefined) {
        msg =  "";
    }
    else {
        msg = msg + " - ";
    }

    assertNotNull(resources, msg + "Expected findResourcesByCriteria() to return a non-null result");
    assertTrue(resources.size() > 0, msg + "Expected findResourcesByCriteria() to return a non-empty result set");

    for (i = 0; i < resources.size(); ++i) {
        resource = resources.get(i);

        assertNotNull(ResourceManager.getResource(resource.id),
            'Expected getResourceTypeById to a return a ResourceType for id ' + resource.id);
    }
}

