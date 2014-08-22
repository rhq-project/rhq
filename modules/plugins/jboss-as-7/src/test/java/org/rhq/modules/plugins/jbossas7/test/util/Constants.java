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

package org.rhq.modules.plugins.jbossas7.test.util;

import java.io.File;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.util.FileUtils;

/**
 * @author Thomas Segismont
 */
public class Constants {

    public static final File JBOSS_HOME = new File(FileUtils.getCanonicalPath(System.getProperty("jboss7.home")));

    public static final String PLUGIN_NAME = "JBossAS7";

    public static final ResourceType STANDALONE_RESOURCE_TYPE = new ResourceType("JBossAS7 Standalone Server",
        PLUGIN_NAME, ResourceCategory.SERVER, null);
    // The key is the server host config file
    // hostConfig: /tmp/jboss-as-6.0.0/standalone/configuration/standalone-full-ha.xml
    public static final String STANDALONE_RESOURCE_KEY = "hostConfig: "
        + new File(JBOSS_HOME, "standalone" + File.separator + "configuration" + File.separator
            + "standalone-full-ha.xml").getAbsolutePath();

    public static final ResourceType DOMAIN_RESOURCE_TYPE = new ResourceType("JBossAS7 Host Controller", PLUGIN_NAME,
        ResourceCategory.SERVER, null);
    // The key is the server host config file
    // hostConfig: /tmp/jboss-as-6.0.0/domain/configuration/host.xml
    public static final String DOMAIN_RESOURCE_KEY = "hostConfig: "
        + new File(JBOSS_HOME, "domain" + File.separator + "configuration" + File.separator + "host.xml")
            .getAbsolutePath();

    public static final String MANAGEMENT_USERNAME = "admin";
    public static final String MANAGEMENT_PASSWORD = "admin";

    public static final String STANDALONE_HOST = System.getProperty("jboss.standalone.bindAddress");
    public static final int STANDALONE_HTTP_PORT = Integer.getInteger("jboss.standalone.portOffset") + 9990;

    public static final String DC_HOST = System.getProperty("jboss.domain.bindAddress");
    public static final int DC_HTTP_PORT = Integer.getInteger("jboss.domain.httpManagementPort");

    private Constants() {
        // Constants class
    }
}
