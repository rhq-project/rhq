/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.wildfly10.itest.domain;

import static org.rhq.core.domain.resource.ResourceCategory.SERVICE;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.wildfly10.itest.AbstractDatasourceTest;
import org.rhq.modules.plugins.wildfly10.test.util.Constants;

/**
 * @author Thomas Segismont
 */
@Test(groups = { "integration", "pc", "domain" }, singleThreaded = true)
public class DatasourceDomainTest extends AbstractDatasourceTest {

    private static final String PROFILE_RESOURCE_TYPE_NAME = "Profile";
    private static final String PROFILE_RESOURCE_KEY = "profile=full";
    private static final String DATASOURCES_SUBSYSTEM_RESOURCE_TYPE_NAME = "Datasources (Profile)";
    private static final String DATASOURCES_SUBSYSTEM_RESOURCE_KEY = "profile=full,subsystem=datasources";
    private static final String DATASOURCE_RESOURCE_TYPE_NAME = "DataSource (Profile)";
    private static final String DATASOURCE_TEST_DS = "DatasourceDomainTestDS";

    @Override
    protected String getDatasourceResourceTypeName() {
        return DATASOURCE_RESOURCE_TYPE_NAME;
    }

    @Override
    protected String getTestDatasourceName() {
        return DATASOURCE_TEST_DS;
    }

    @Override
    protected Resource getDatasourcesSubsystemResource() throws Exception {
        Resource platform = validatePlatform();
        Resource server = waitForResourceByTypeAndKey(platform, platform, Constants.DOMAIN_RESOURCE_TYPE, Constants.DOMAIN_RESOURCE_KEY);
        Resource profileFull = waitForResourceByTypeAndKey(platform, server, new ResourceType(
            PROFILE_RESOURCE_TYPE_NAME, getPluginName(), SERVICE, null), PROFILE_RESOURCE_KEY);
        return waitForResourceByTypeAndKey(platform, profileFull, new ResourceType(
            DATASOURCES_SUBSYSTEM_RESOURCE_TYPE_NAME, getPluginName(), SERVICE, null),
            DATASOURCES_SUBSYSTEM_RESOURCE_KEY);
    }
}
