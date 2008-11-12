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
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.system.ServiceMBeanSupport;

public class CoreServer extends ServiceMBeanSupport implements CoreServerMBean {
    private final Log log = LogFactory.getLog(CoreServer.class);

    /**
     * The name of the version file as found in this class's classloader
     */
    private static final String VERSION_FILE = "rhq-server-version.properties";

    /**
     * Version property whose value is the product version.
     */
    private static final String PROP_PRODUCT_VERSION = "Product-Version";

    /**
     * Version property whose value is the source code revision number used to make the build.
     */
    private static final String PROP_BUILD_NUMBER = "Build-Number";

    /**
     * Version property whose value is the date when this version of the product was built.
     */
    private static final String PROP_BUILD_DATE = "Build-Date";

    private Properties buildProps;

    private Date bootTime;

    @Override
    protected void createService() throws Exception {
        this.buildProps = loadBuildProperties();
        this.bootTime = new Date();

        // make sure our log file has an indication of the version of this server
        String version = getVersion();
        String buildNumber = getBuildNumber();
        String buildDate = this.buildProps.getProperty(PROP_BUILD_DATE, "?");
        log.info("Version=[" + version + "], Build Number=[" + buildNumber + "], Build Date=[" + buildDate + "]");
    }

    @Override
    public String getName() {
        return "RHQ Server";
    }

    public String getVersion() {
        return this.buildProps.getProperty(PROP_PRODUCT_VERSION, "?");
    }

    public String getBuildNumber() {
        return this.buildProps.getProperty(PROP_BUILD_NUMBER, "?");
    }

    public Date getBootTime() {
        return bootTime;
    }

    private Properties loadBuildProperties() {
        Properties buildProps = new Properties();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try {
            InputStream stream = classLoader.getResourceAsStream(VERSION_FILE);
            buildProps.load(stream);
            stream.close();
        } catch (Exception e) {
            log.fatal("Failed to load [" + VERSION_FILE + "] via class loader [" + classLoader + "]");
        }

        return buildProps;
    }
}