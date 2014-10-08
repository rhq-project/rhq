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

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import static org.rhq.core.domain.resource.ResourceCategory.SERVICE;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_TYPE;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.jbossas7.itest.AbstractDatasourceTest;

/**
 * @author Thomas Segismont
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class DatasourceStandaloneTest extends AbstractDatasourceTest {

    private static final String DATASOURCES_SUBSYSTEM_RESOURCE_TYPE_NAME = "Datasources (Standalone)";
    private static final String DATASOURCES_SUBSYSTEM_RESOURCE_KEY = "subsystem=datasources";
    private static final String DATASOURCE_RESOURCE_TYPE_NAME = "DataSource (Standalone)";
    private static final String DATASOURCE_TEST_DS = "DatasourceTestDS";

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
        Resource server = waitForResourceByTypeAndKey(platform, platform, STANDALONE_RESOURCE_TYPE,
            STANDALONE_RESOURCE_KEY);
        return waitForResourceByTypeAndKey(platform, server, new ResourceType(DATASOURCES_SUBSYSTEM_RESOURCE_TYPE_NAME,
            getPluginName(), SERVICE, null), DATASOURCES_SUBSYSTEM_RESOURCE_KEY);
    }
}
