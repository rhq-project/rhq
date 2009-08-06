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

function testFindByCriteria() {
    var resourceName = "service-alpha-0";
    var parentResourceName = "server-omega-0";

    criteria = new ResourceCriteria();
    criteria.addFilterName(resourceName);
    criteria.addFilterParentResourceName(parentResourceName);

    resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertNumberEqualsJS(resources.size(), 1, "Expected to get back a single resource named, '" + resourceName +
        "' and having a parent named '" + parentResourceName + "'");

    var description = resourceName + " description";
    var version = "1.0";

    criteria.addFilterDescription(description);
    criteria.addFilterVersion(version);

    resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertNumberEqualsJS(resources.size(), 1, "Expected to get back a single resource with name, '" +
        resourceName + "', parent resource name, '" + parentResourceName + "' and description, '" + description + "'");
}


function assertResourcesFound(msg) {
    if (msg == undefined) {
        msg =  "";
    }
    else {
        msg = msg + " - ";
    }

    Assert.assertNotNull(resources, msg + "Expected findResourcesByCriteria() to return a non-null result");
    Assert.assertTrue(resources.size() > 0, msg + "Expected findResourcesByCriteria() to return a non-empty result set");

    for (i = 0; i < resources.size(); ++i) {
        resource = resources.get(i);

        Assert.assertNotNull(ResourceManager.getResource(resource.id),
            'Expected getResourceTypeById to a return a ResourceType for id ' + resource.id);
    }
}

