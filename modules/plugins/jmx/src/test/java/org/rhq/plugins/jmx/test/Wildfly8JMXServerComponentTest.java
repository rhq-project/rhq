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

package org.rhq.plugins.jmx.test;

import static org.rhq.core.util.StringUtil.isNotBlank;
import static org.testng.Assert.assertTrue;

import java.io.File;

import org.testng.annotations.BeforeClass;

import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Thomas Segismont
 */
public class Wildfly8JMXServerComponentTest extends BaseApplicationServerTest {
    private static final String WLFY8_MGMT_PORT_PROPERTY_NAME = "rhq.jmxplugin.wildfly8.standalone.management.port";
    private static final String WLFY8_MGMT_PORT = System.getProperty(WLFY8_MGMT_PORT_PROPERTY_NAME);
    private static final String WLFY8_HOME_PROPERTY_NAME = "rhq.jmxplugin.wildfly8.standalone.home";
    private static final String WLFY8_HOME = System.getProperty(WLFY8_HOME_PROPERTY_NAME);
    private static final String TEMPLATE_NAME = "Wildfly 8";

    @BeforeClass
    public void checkProperties() {
        assertTrue(isNotBlank(WLFY8_MGMT_PORT), "Undefined " + WLFY8_MGMT_PORT_PROPERTY_NAME);
        assertTrue(isNotBlank(WLFY8_HOME), "Undefined " + WLFY8_HOME_PROPERTY_NAME);
    }

    @Override
    protected String getServerTypeName() {
        return TEMPLATE_NAME;
    }

    @Override
    protected String getPluginConfigTemplateName() {
        return TEMPLATE_NAME;
    }

    @Override
    protected void setupTemplatedPluginConfig(Configuration pluginConfig) {
        pluginConfig.setSimpleValue("connectorAddress", "service:jmx:http-remoting-jmx://localhost:" + WLFY8_MGMT_PORT);
        pluginConfig.setSimpleValue("installURI", new File(WLFY8_HOME).getAbsolutePath());
        pluginConfig.setSimpleValue("principal", "rhqadmin");
        pluginConfig.setSimpleValue("credentials", "rhqadmin_1");
    }
}
