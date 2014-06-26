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

import javax.management.ObjectName;

import org.rhq.core.util.ObjectNameFactory;

/**
 * MBean interface to the service that is responsible for installing JON's JAAS login handlers.
 */
public interface CustomJaasDeploymentServiceMBean {

    /**
     * The name used to identify the security domain where this service registers the login modules.
     */
    String RHQ_USER_SECURITY_DOMAIN = "RHQUserSecurityDomain";

    /**
     * The name of this service when deployed.
     */
    ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=CustomJaasDeployment");

    /**
     * Installs the JAAS Modules that JON Server uses to allow users to log in.
     */
    void installJaasModules();

    /**
     * Called from the startup bean and will upgrade an existing
     * RHQUserSecurityDomain if needed - that is if the system
     * settings say that LDAP support is enabled, but the underlying
     * modules are not present
     */
    void upgradeRhqUserSecurityDomainIfNeeded();
}