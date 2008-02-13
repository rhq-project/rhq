/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.core;

import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.system.ServiceMBeanSupport;

public class CoreServer extends ServiceMBeanSupport implements CoreServerMBean {
    private static final String SERVER_GROUPID = "org.rhq";
    private static final String SERVER_ARTIFACTID = "rhq-enterprise-server";
    private static final String SERVER_BUILD_PROPERTIES_RESOURCE_NAME = "META-INF/maven/" + SERVER_GROUPID + "/"
        + SERVER_ARTIFACTID + "/pom.properties";
    private static final String VERSION_PROP = "version";

    private final Log log = LogFactory.getLog(CoreServer.class);

    private Properties buildProps;

    @Override
    protected void createService() throws Exception {
        this.buildProps = loadBuildProperties();
    }

    @Override
    public String getName() {
        return "RHQ Server";
    }

    public String getVersion() {
        return this.buildProps.getProperty(VERSION_PROP, "?");
    }

    private Properties loadBuildProperties() {
        Properties buildProps = new Properties();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try {
            InputStream buildPropsInputStream = classLoader.getResourceAsStream(SERVER_BUILD_PROPERTIES_RESOURCE_NAME);
            buildProps.load(buildPropsInputStream);
            buildPropsInputStream.close();
        } catch (Exception e) {
            log.fatal("Failed to load " + SERVER_BUILD_PROPERTIES_RESOURCE_NAME + " via class loader " + classLoader
                + ".");
        }

        return buildProps;
    }
}