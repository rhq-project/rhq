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
package org.rhq.enterprise.server.content;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import org.jboss.mx.util.MBeanServerLocator;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginContainer;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginServiceManagement;

/**
 * ContentManagerHelper - Helper class to contain common methods needed by the Content managers.
 */
public class ContentManagerHelper {
    public static ContentSourcePluginContainer getPluginContainer() throws Exception {
        ContentSourcePluginContainer pc = null;

        try {
            ContentSourcePluginServiceManagement mbean;
            MBeanServer mbs = MBeanServerLocator.locateJBoss();
            ObjectName name = ObjectNameFactory.create(ContentSourcePluginServiceManagement.OBJECT_NAME_STR);
            Class<?> iface = ContentSourcePluginServiceManagement.class;
            mbean = (ContentSourcePluginServiceManagement) MBeanServerInvocationHandler.newProxyInstance(mbs, name,
                iface, false);
            if (!mbean.isPluginContainerStarted()) {
                throw new IllegalStateException("The content source plugin container is not started!");
            }

            pc = mbean.getPluginContainer();
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            throw new Exception("Cannot obtain the content source plugin container", e);
        }

        if (pc == null) {
            throw new Exception("Content source plugin container is null!");
        }

        return pc;
    }
}