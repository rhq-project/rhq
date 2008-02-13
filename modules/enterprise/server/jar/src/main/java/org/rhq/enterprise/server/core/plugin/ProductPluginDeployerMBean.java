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

import java.util.List;
import javax.management.ObjectName;
import org.jboss.mx.util.ObjectNameFactory;

/**
 * MBean interface to the deployer responsible for detecting plugins jars getting deployed.
 */
public interface ProductPluginDeployerMBean extends org.jboss.deployment.SubDeployerMBean {
    /**
     * The name this service will be registered as.
     */
    ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=AgentPluginDeployer");

    /**
     * Sets the directory where the plugin jars are located.
     *
     * @param name
     */
    void setPluginDir(String name);

    /**
     * Gets the directory name where the plugin jars are located.
     *
     * @return plugin directory name
     */
    String getPluginDir();

    /**
     * Get a list of names of all plugins that are currently deployed.
     *
     * @return list of plugin names
     */
    List<String> getRegisteredPluginNames();

    /**
     * Start the deployer process. This is a separate method from the mbean's start() lifecycle method because we only
     * invoke this method when we are assured all EJBs are available.
     */
    void startDeployer();
}