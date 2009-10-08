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
package org.rhq.enterprise.server.core.plugin;

import java.io.File;

import javax.management.ObjectName;

import org.rhq.core.util.ObjectNameFactory;

public interface AgentPluginDeploymentScannerMBean {
    /**
     * The name this service will be registered as.
     */
    ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=AgentPluginDeploymentScanner");

    void start() throws Exception;

    void stop();

    /**
     * This is called separately from the start/stop lifecycle methods. It should only be called
     * when we know the EJB/SLSB layer is fully initialized and ready to accept requests.
     */
    void startDeployment();

    /**
     * This will scan the database for new/updated plugins and if it finds
     * any, will write the content as plugin files in the file system. This
     * will also delete old, obsolete plugins.
     * 
     * @see #scanAndRegister()
     */
    void scan() throws Exception;

    /**
     * This will scan the database for new/updated plugins and if it finds
     * any, will write the content as plugin files in the file system. This
     * will also delete old, obsolete plugins. Once finished, this will register
     * agent plugins and update the database as appropriate.
     */
    void scanAndRegister() throws Exception;

    /**
     * Sets the amount of time (in milliseconds) between scans.
     *
     * @param ms time in millis between each scans
     */
    void setScanPeriod(Long ms);

    /**
     * Gets the amount of time (in milliseconds) between scans.
     *
     * @return the time in millis between each scans
     */
    Long getScanPeriod();

    /**
     * Sets the directory where the plugin jars are located.
     *
     * @param name the name of the plugins dir
     */
    void setPluginDir(File name);

    /**
     * Gets the directory name where the plugin jars are located.
     *
     * @return plugin directory name
     */
    File getPluginDir();

    void setLicenseFile(File name);

    File getLicenseFile();
}
