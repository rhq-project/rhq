/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.ws.test;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.InventoryStatus;
import org.rhq.enterprise.server.ws.Resource;
import org.rhq.enterprise.server.ws.ResourceCriteria;
import org.rhq.enterprise.server.ws.Subject;
import org.rhq.enterprise.server.ws.test.util.WsResourceUtility;
import org.rhq.enterprise.server.ws.test.util.WsSubjectUtility;

/**
 * Web service tests for the registration manager services.
 *
 * @author Jason Dobies
 */
@Test(groups = "ws")
public class WsRegistrationManagerTest extends WsUnitTestBase {

    private static final boolean TESTS_ENABLED = true;

    @Test(enabled = TESTS_ENABLED)
    public void importPlatform() throws Exception {

        // Setup
        WsResourceUtility resourceUtil = new WsResourceUtility(service);
        WsSubjectUtility subjectUtil = new WsSubjectUtility(service);
        Subject admin = subjectUtil.admin();

        // Test

        // Note: randomPlatform calls #registerPlatform, which is why there's no explicit test for that
        Resource created = resourceUtil.randomPlatform();
        service.importPlatform(admin, created);

        // Verify and Cleanup
        try {
            ResourceCriteria criteria = objectFactory.createResourceCriteria();
            criteria.setFilterId(created.getId());
            List<Resource> resources = service.findResourcesByCriteria(admin, criteria);

            assert resources.size() == 1 : "Incorrect number of resources returned. Found: " + resources.size();

            Resource retrieved = resources.get(0);

            assert retrieved.getName().equals(created.getName());
            assert retrieved.getInventoryStatus().equals(InventoryStatus.COMMITTED);
        } finally {
            resourceUtil.deleteResource(created.getId());
        }

    }
}
