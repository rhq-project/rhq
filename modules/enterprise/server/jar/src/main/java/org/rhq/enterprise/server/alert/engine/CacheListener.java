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
package org.rhq.enterprise.server.alert.engine;

import java.io.Serializable;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.interceptors.CacheMgmtInterceptor;

/**
 * Listens to cache events and dumps them to the log. Mostly for debugging purposes.
 *
 * @author Heiko W. Rupp
 */
public class CacheListener {
    private static Log log = LogFactory.getLog(CacheListener.class);

    private static CacheListener myself;

    private CacheListener() {
        // Singleton pattern
    }

    public static void startUp() {
        if (myself == null) {
            myself = new CacheListener();
            myself.init();
        }
    }

    private void init() {
        MyListener listener = new MyListener();
        NotificationFilterSupport filter = null;

        try {
            // get reference to MBean server
            Context ic = new InitialContext();
            MBeanServerConnection server = (MBeanServerConnection) ic.lookup("jmx/invoker/RMIAdaptor");

            // get reference to CacheMgmtInterceptor MBean
            String cacheName = "rhq.cache:subsystem=alerts,service=cache";
            ObjectName mgmt_name = new ObjectName(cacheName);

            // configure a filter to only receive node created and removed events
            filter = new NotificationFilterSupport();
            filter.disableAllTypes();
            filter.enableType(CacheMgmtInterceptor.NOTIF_NODE_CREATED);
            filter.enableType(CacheMgmtInterceptor.NOTIF_NODE_REMOVED);
            filter.enableType(CacheMgmtInterceptor.NOTIF_NODE_MODIFIED);

            // register the listener with a filter
            // leave the filter null to receive all cache events
            server.addNotificationListener(mgmt_name, listener, filter, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class MyListener implements NotificationListener, Serializable {
        public void handleNotification(Notification notification, Object handback) {
            String message = notification.getMessage();
            String type = notification.getType();
            Object userData = notification.getUserData();
            System.out.println(type + ": " + message);
            if (userData == null) {
                System.out.println("notification data is null");
            } else if (userData instanceof String) {
                log.info("notification data: " + (String) userData);
            } else if (userData instanceof Object[]) {
                Object[] ud = (Object[]) userData;
                for (int i = 0; i > ud.length; i++) {
                    log.info("notification data: " + ud[i].toString());
                }
            } else {
                log.info("notification data class: " + userData.getClass().getName());
            }
        }
    }
}