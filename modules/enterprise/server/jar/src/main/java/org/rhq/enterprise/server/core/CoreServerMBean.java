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

import java.io.File;
import java.util.Date;

import javax.management.ObjectName;

import org.jboss.mx.util.ObjectNameFactory;
import org.jboss.system.ServiceMBean;
import org.rhq.core.domain.common.ProductInfo;

/**
 * An MBean that exposes various core server global attributes (version, uptime, etc.).
 */
public interface CoreServerMBean extends ServiceMBean {
    /**
     * The name of this MBean when deployed.
     */
    ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=CoreServer");

    /**
     * Returns the version of the core RHQ server.
     *
     * @return the version of the server
     */
    String getVersion();

    /**
     * Returns the build number of the core RHQ Server. Servers that returns the same
     * {@link #getVersion() version} may return different build numbers, depending
     * on the source code stream that built the server binaries. 
     * 
     * @return the build number
     */
    String getBuildNumber();

    /**
     * Returns the time when this server started.
     * 
     * @return the boot time of the server
     */
    Date getBootTime();

    /**
     * Where the RHQ Server is installed.
     * @return RHQ Server install directory
     */
    File getInstallDir();

    /**
     * Where the JBoss Server Home directory is. Typically, this is something like:
     * {@link #getInstallDir() install-dir}/jbossas/server/default
     * @return jboss server home directory
     */
    File getJBossServerHomeDir();

    /**
     * Where the JBoss Server Data directory is. Typically, this is something like:
     * {@link #getInstallDir() install-dir}/jbossas/server/default/data
     * @return jboss server home directory
     */
    File getJBossServerDataDir();

    /**
     * Where the JBoss Server Temp directory is. Typically, this is something like:
     * {@link #getInstallDir() install-dir}/jbossas/server/default/tmp
     * @return jboss server home directory
     */
    File getJBossServerTempDir();

    /**
     * Product information - the product name, homepage URL, docs URL, etc.
     *
     * @return product information - the product name, homepage URL, docs URL, etc.
     */
    ProductInfo getProductInfo();
}