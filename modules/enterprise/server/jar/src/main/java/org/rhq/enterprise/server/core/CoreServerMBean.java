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
import org.jboss.mx.util.ObjectNameFactory;
import org.jboss.system.ServiceMBean;

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
}