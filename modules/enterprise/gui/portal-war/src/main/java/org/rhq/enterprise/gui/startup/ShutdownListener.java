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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This listens for the RHQ server's shutdown notification and when it hears it, will start shutting down RHQ components
 * that need to clean up.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 * @author Joseph Marques
 */
public class ShutdownListener implements NotificationListener {
    /**
     * Logger
     */
    private final Log log = LogFactory.getLog(ShutdownListener.class);

    private final String RHQ_DB_TYPE_MAPPING_PROPERTY = "rhq.server.database.type-mapping";

    /**
     * This is called when the shutdown notification is received from the JBoss server. This gives a chance for us to
     * cleanly shutdown our application in an orderly fashion.
     *
     * @see javax.management.NotificationListener#handleNotification(Notification, Object)
     */
    public void handleNotification(Notification notification, Object handback) {
        if (org.jboss.system.server.Server.STOP_NOTIFICATION_TYPE.equals(notification.getType())) {
            stopScheduler();

            updateServerOperationMode();

            stopEmbeddedDatabase();
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
        } catch (Throwable t) {
            // only show ugly stack traces if the user runs the server in debug mode
            if (log.isDebugEnabled()) {
                log.warn("Failed to shutdown the scheduler", t);
            } else {
                log.warn("Failed to shutdown the scheduler: " + t.getMessage());
            }
        }
    }

    private void updateServerOperationMode() {
        try {
            // Set the server operation mode to DOWN unless in MM
            Server server = LookupUtil.getServerManager().getServer();
            if (Server.OperationMode.MAINTENANCE != server.getOperationMode()) {
                LookupUtil.getCloudManager().updateServerMode(new Integer[] { server.getId() }, OperationMode.DOWN);
            }
        } catch (Throwable t) {
            // only show ugly stack traces if the user runs the server in debug mode
            if (log.isDebugEnabled()) {
                log.warn("Could not update this server's OperationMode to DOWN in the database", t);
            } else {
                log.warn("Could not update this server's OperationMode to DOWN in the database: " + t.getMessage());
            }
        }
    }

    private void stopEmbeddedDatabase() {
        if (!isEmbedded()) {
            // only perform shutdown actions if we ABSOLUTELY know for sure this is the embedded database
            return;
        }

        Connection connection = null;
        Statement statement = null;
        try {
            DataSource ds = LookupUtil.getDataSource();
            connection = ds.getConnection();
            statement = connection.createStatement();
            statement.execute("shutdown");
            log.info("Embedded database closed cleanly");
        } catch (SQLException sqle) {
            if (sqle.getMessage().toLowerCase().indexOf("database is already closed") != -1) {
                log.warn("Database is already shut down, can not perform graceful service shutdown");
                return;
            } else {
                // only show ugly stack traces if the user runs the server in debug mode
                if (log.isDebugEnabled()) {
                    log.warn("Could not shut down the embedded database cleanly", sqle);
                } else {
                    log.warn("Could not shut down the embedded database cleanly: " + sqle.getMessage());
                }
            }
        } finally {
            JDBCUtil.safeClose(connection, statement, null);
        }
    }

    private boolean isEmbedded() {
        String identity = System.getProperty(RHQ_DB_TYPE_MAPPING_PROPERTY, "");
        if (identity.equals("")) {
            log.error("Could not determine datatype base; is the " + RHQ_DB_TYPE_MAPPING_PROPERTY
                + " property set in rhq-server.properties?");
        }
        return identity.toLowerCase().indexOf("h2") != -1;
    }
}