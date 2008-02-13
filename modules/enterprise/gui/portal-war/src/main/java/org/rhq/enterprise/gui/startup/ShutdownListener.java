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
package org.rhq.enterprise.gui.startup;

import javax.management.Notification;
import javax.management.NotificationListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.system.server.Server;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This listens for the RHQ server's shutdown notification and when it hears it, will start shutting down RHQ components
 * that need to clean up.
 *
 * @author John Mazzitelli
 */
public class ShutdownListener implements NotificationListener {
    /**
     * Logger
     */
    private final Log log = LogFactory.getLog(ShutdownListener.class);

    /**
     * This is called when the shutdown notification is received from the JBoss server. This gives a chance for us to
     * cleanly shutdown our application in an orderly fashion.
     *
     * @see javax.management.NotificationListener#handleNotification(Notification, Object)
     */
    public void handleNotification(Notification notification, Object handback) {
        if (Server.STOP_NOTIFICATION_TYPE.equals(notification.getType())) {
            stopScheduler();
        }
    }

    /**
     * This will shutdown the scheduler.
     */
    private void stopScheduler() {
        try {
            log.info("Shutting down the scheduler gracefully - currently running jobs will be allowed to finish...");
            LookupUtil.getSchedulerBean().shutdown(true);
            log.info("The scheduler has been shutdown and all jobs are done.");
        } catch (Exception e) {
            log.warn("Failed to shutdown the scheduler.", e);
        }
    }
}