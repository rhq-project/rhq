/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.util;

import static org.rhq.core.domain.resource.ResourceCategory.PLATFORM;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.core.domain.resource.ResourceCategory.SERVICE;
import static org.rhq.core.domain.util.ResourceUtility.getBaseServerOrService;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @author Thomas Segismont
 */
public class ResourceUtilityTest {

    private static final ResourceType TEST_PLATFORM_TYPE = new ResourceType("test platform", "test plugin", PLATFORM,
        null);
    private static final ResourceType TEST_TOP_LEVEL_SERVER_TYPE = new ResourceType("test topLevelServer",
        "test plugin", SERVER, null);

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void getBaseServerOrServiceShouldThrowIllegalArgumentExceptionOnNull() throws Exception {
        getBaseServerOrService(null);
    }

    @Test
    public void getBaseServerOrServiceOfPlatform() throws Exception {
        Resource platform = new Resource();
        platform.setResourceType(TEST_PLATFORM_TYPE);
        assertSame(platform, getBaseServerOrService(platform));
    }

    @Test
    public void getBaseServerOrServiceOfTopLevelServer() throws Exception {
        Resource topLevelServer = new Resource();
        topLevelServer.setResourceType(TEST_TOP_LEVEL_SERVER_TYPE);
        assertSame(topLevelServer, getBaseServerOrService(topLevelServer));
    }

    @Test
    public void getBaseServerOrServiceOfTopLevelService() throws Exception {
        Resource topLevelService = new Resource();
        topLevelService.setResourceType(new ResourceType("test topLevelService", "test plugin", SERVICE, null));
        assertSame(topLevelService, getBaseServerOrService(topLevelService));
    }

    @Test
    public void getBaseServerOrServiceOfNestedService() throws Exception {
        ResourceType testServiceType = new ResourceType("test service", "test plugin", SERVICE,
            TEST_TOP_LEVEL_SERVER_TYPE);
        Resource service = new Resource();
        service.setResourceType(testServiceType);
        Resource topLevelServer = new Resource();
        topLevelServer.setResourceType(TEST_TOP_LEVEL_SERVER_TYPE);
        topLevelServer.addChildResource(service);
        Resource platform = new Resource();
        platform.setResourceType(TEST_PLATFORM_TYPE);
        platform.addChildResource(topLevelServer);
        assertSame(topLevelServer, getBaseServerOrService(service));
    }

}
