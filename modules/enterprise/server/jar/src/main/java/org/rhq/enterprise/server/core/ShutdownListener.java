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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;

/**
 * This listens for the RHQ server's shutdown notification and when it hears it, will start shutting down RHQ components
 * that need to clean up.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 * @author Joseph Marques
 */
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ShutdownListener {
    private final Log log = LogFactory.getLog(ShutdownListener.class);

    private final String RHQ_DB_TYPE_MAPPING_PROPERTY = "rhq.server.database.type-mapping";

    @EJB
    private SchedulerLocal schedulerBean;

    @EJB
    private ServerManagerLocal serverManager;

    @EJB
    private CloudManagerLocal cloudManager;

    @Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    /**
     * This is called when the shutdown notification is received from the JBoss server. This gives a chance for us to
     * cleanly shutdown our application in an orderly fashion.
     *
     * @see javax.management.NotificationListener#handleNotification(Notification, Object)
     */
    @PreDestroy
    public void handleNotification() {
        // JBossAS 4.2.3 used to send us this JMX notification on shutdown - AS7 does not have shutdown notifications.
        // So we are using the @PreDestroy mechanism on a singleton EJB to attempt to clean up the application before it is shutdown
        log.info("Shutdown listener has been told we are shutting down - starting to clean up now...");
        stopScheduler();
        updateServerOperationMode();
        stopEmbeddedDatabase();
        log.info("Shutdown listener completed its shutdown tasks. It is safe to shutdown now.");
    }

    /**
     * This will shutdown the scheduler.
     */
    private void stopScheduler() {
        try {
            log.info("Shutting down the scheduler gracefully - currently running jobs will be allowed to finish...");
            schedulerBean.shutdown(true);
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
            Server server = serverManager.getServer();
            if (Server.OperationMode.MAINTENANCE != server.getOperationMode()) {
                cloudManager.updateServerMode(new Integer[] { server.getId() }, OperationMode.DOWN);
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
            connection = dataSource.getConnection();
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