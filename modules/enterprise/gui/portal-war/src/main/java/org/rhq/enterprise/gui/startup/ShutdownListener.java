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

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
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
        Connection connection = null;
        Statement statement = null;
        DatabaseType dbType = null;
        try {
            DataSource ds = LookupUtil.getDataSource();
            connection = ds.getConnection();
            dbType = DatabaseTypeFactory.getDatabaseType(connection);
        } catch (Exception e) {
            log.warn("If using the embedded database, you should pass 'DB_CLOSE_ON_EXIT=FALSE' in the connection URL");
        } finally {
            JDBCUtil.safeClose(connection, statement, null);
        }

        if (org.jboss.system.server.Server.STOP_NOTIFICATION_TYPE.equals(notification.getType())) {
            stopScheduler();

            // Set the server operation mode to DOWN unless in MM
            Server server = LookupUtil.getServerManager().getServer();
            if (Server.OperationMode.MAINTENANCE != server.getOperationMode()) {
                LookupUtil.getCloudManager().updateServerMode(new Integer[] { server.getId() }, OperationMode.DOWN);
            }

            stopEmbeddedDatabase(dbType);
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

    private void stopEmbeddedDatabase(DatabaseType dbType) {
        Connection connection = null;
        Statement statement = null;
        try {
            DataSource ds = LookupUtil.getDataSource();
            connection = ds.getConnection();
            if (dbType instanceof H2DatabaseType) {
                statement = connection.createStatement();
                statement.execute("shutdown");
            }
            log.info("Embedded database closed cleanly");
        } catch (SQLException sqle) {
            if (sqle.getMessage().toLowerCase().indexOf("database is already closed") != -1) {
                log.warn("Database is already shut down, can not perform graceful service shutdown");
                return;
            } else {
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
}