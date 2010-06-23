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

import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.Distribution;
import org.rhq.enterprise.server.ws.DistributionType;
import org.rhq.enterprise.server.ws.Subject;

/**
 * Web service tests for the distribution manager service.
 *
 * @author Jason Dobies
 */
@Test(groups = "ws")
public class WsDistributionManagerTest extends WsUnitTestBase {

// Currently no WS interface
    
//    private static final boolean TESTS_ENABLED = true;
//
//    @Test(enabled = TESTS_ENABLED)
//    public void createGetDeleteDistribution() throws Exception {
//
//        // Setup
//        Subject admin = subjectUtil.admin();
//        String label = "WsDistributionManagerTest.createGetDeleteDistribution.label";
//        String path = "WsDistributionManagerTest.createGetDeleteDistribution.path";
//
//        DistributionType type = objectFactory.createDistributionType();
//        type.setName("WsDistributionManagerTest.createGetDeleteDistribution.type");
//        type.setDescription("WsDistributionManagerTest.createGetDeleteDistribution.desc");
//
//        // Test
//        Distribution created = service.createDistribution(admin, label, path, type);
//
//        assert created != null;
//
//        Distribution byLabel = service.getDistributionByLabel(label);
//        Distribution byPath = service.getDistributionByPath(path);
//
//        DistributionType typeByName = service.getDistributionTypeByName(type.getName());
//
//        service.deleteDistributionByDistId(admin, created.getId());
//
//        // Verify
//        assert byLabel != null;
//        assert label.equals(byLabel.getLabel());
//        assert path.equals(byLabel.getBasePath());
//        assert type.getName().equals(byLabel.getDistributionType().getName());
//
//        assert byPath != null;
//        assert label.equals(byPath.getLabel());
//        assert path.equals(byPath.getBasePath());
//        assert type.getName().equals(byPath.getDistributionType().getName());
//
//        assert typeByName != null;
//        assert type.getName().equals(typeByName.getName());
//        assert type.getDescription().equals(typeByName.getDescription());
//
//        Distribution afterDelete = service.getDistributionByLabel(label);
//        assert afterDelete == null;
//
//        // Cleanup
//        service.deleteDistributionTypeByName(admin, type.getName());
//        DistributionType typeAfterDelete = service.getDistributionTypeByName(type.getName());
//
//        assert typeAfterDelete == null;
//    }

}
