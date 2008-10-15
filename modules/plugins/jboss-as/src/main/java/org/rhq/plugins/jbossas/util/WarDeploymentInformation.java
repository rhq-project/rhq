 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas.util;

/**
 * Simple class to hold the pieces of War Deployment information needed to be set in the
 * plugin configuration for a war resource. Instead of getting each piece of information individually
 * and calling the MBean Server multiple times, getting it all at once will increate performance.
 */
public class WarDeploymentInformation {
    private String fileName;
    private String contextRoot;
    private String jbossWebModuleMBeanObjectName;
    private String vHost;

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getJbossWebModuleMBeanObjectName() {
        return jbossWebModuleMBeanObjectName;
    }

    public void setJbossWebModuleMBeanObjectName(String jbossWebModuleMBeanObjectName) {
        this.jbossWebModuleMBeanObjectName = jbossWebModuleMBeanObjectName;
    }

    public String getVHost() {
        return vHost;
    }

    public void setVHost(String host) {
        vHost = host;
    }
}
