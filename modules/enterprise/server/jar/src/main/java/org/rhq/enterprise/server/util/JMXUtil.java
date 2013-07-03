/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.util;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Thomas Segismont
 */
public class JMXUtil {

    private static final Log LOG = LogFactory.getLog(JMXUtil.class);

    private JMXUtil() {
        // Defensive
    }

    public static void registerMBean(Object bean, ObjectName objectName) {
        try {
            getPlatformMBeanServer().registerMBean(bean, objectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void unregisterMBeanQuietly(ObjectName objectName) {
        try {
            getPlatformMBeanServer().unregisterMBean(objectName);
        } catch (Exception e) {
            LOG.error("Exception on " + objectName + " unregister", e);
        }
    }

    public static MBeanServer getPlatformMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

}
