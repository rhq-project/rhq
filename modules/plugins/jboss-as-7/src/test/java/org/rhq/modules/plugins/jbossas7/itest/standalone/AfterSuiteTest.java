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

import java.io.File;
import java.util.List;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.jbossas7.itest.AbstractServerComponentTest;

/**
 * This is to be the last class run in the AS-7 test suite.  It does nothing but kill the
 * server processes as a cleanup operation.
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class AfterSuiteTest extends AbstractServerComponentTest {

    public static final ResourceType DOMAIN_RESOURCE_TYPE = new ResourceType("JBossAS7 Host Controller", PLUGIN_NAME,
        ResourceCategory.SERVER, null);
    // The key is the server host config file
    // hostConfig: /tmp/jboss-as-6.0.0/domain/configuration/host.xml
    public static final String DOMAIN_RESOURCE_KEY = "hostConfig: "
        + new File(JBOSS_HOME, "domain" + File.separator + "configuration" + File.separator + "host.xml")
            .getAbsolutePath();

    public static ResourceType STANDALONE_RESOURCE_TYPE = new ResourceType("JBossAS7 Standalone Server", PLUGIN_NAME,
        ResourceCategory.SERVER, null);
    // The key is the server host config file
    // hostConfig: /tmp/jboss-as-6.0.0/standalone/configuration/standalone-full-ha.xml
    public static final String STANDALONE_RESOURCE_KEY = "hostConfig: "
        + new File(JBOSS_HOME, "standalone" + File.separator + "configuration" + File.separator
            + "standalone-full-ha.xml").getAbsolutePath();

    private boolean killStandalone;

    @Override
    protected Resource getServerResource() {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        return waitForResourceByTypeAndKey(platform, platform, getServerResourceType(), getServerResourceKey());
    }

    @Override
    protected ResourceType getServerResourceType() {
        return killStandalone ? STANDALONE_RESOURCE_TYPE : DOMAIN_RESOURCE_TYPE;
    }

    @Override
    protected String getServerResourceKey() {
        return killStandalone ? STANDALONE_RESOURCE_KEY : DOMAIN_RESOURCE_KEY;
    }

    @Override
    protected String getPortOffsetSystemPropertyName() {
        return killStandalone ? "jboss.standalone.portOffset" : "jboss.domain.portOffset";
    }

    @AfterSuite
    protected void killServerProcesses() {
        killStandalone = true;
        super.killServerProcesses();
        killStandalone = false;
        super.killServerProcesses();
    }

    @Override
    protected String getBindAddressSystemPropertyName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getExpectedStartScriptFileName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<String> getExpectedStartScriptArgs() {
        // TODO Auto-generated method stub
        return null;
    }
}
