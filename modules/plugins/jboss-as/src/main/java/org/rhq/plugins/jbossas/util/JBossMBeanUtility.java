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

import java.util.List;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Utility methods for working with JBoss MBeans.
 * 
 * @author Ian Springer
 */
public class JBossMBeanUtility {
    private static final Log LOG = LogFactory.getLog(JBossMBeanUtility.class);

    private JBossMBeanUtility() {
    }

    public static MBeanServer getJBossMBeanServer() {
        List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        MBeanServer jbossServer = null;
        for (MBeanServer server : servers) {
            if ("jboss".equals(server.getDefaultDomain())) {
                jbossServer = server;
            }
        }
        if (jbossServer == null) {
            jbossServer = ManagementFactory.getPlatformMBeanServer();
        }
        return jbossServer;
    }

    /**
     * Returns true if the specified JBossAS EMS bean is deployed and in the Started state, or false otherwise.
     *
     * @param  emsBean         a JBossAS EMS bean
     * @param  resourceContext the resource context for the JON resource corresponding to the EMS bean
     *
     * @return true if the specified JBoss EMS bean is deployed and in the "Started" state, or false otherwise
     */
    public static boolean isStarted(EmsBean emsBean, @Nullable
    ResourceContext resourceContext) {
        try {
            return ("Started".equals(emsBean.getAttribute("StateString").refresh()));
        } catch (NullPointerException npe) {
            if (resourceContext != null) {
                LOG.warn("Could not determine availability of unknown EMS bean for ["
                    + resourceContext.getResourceType() + ":" + resourceContext.getResourceKey() + "]");
            }
            return false;
        }
    }
}