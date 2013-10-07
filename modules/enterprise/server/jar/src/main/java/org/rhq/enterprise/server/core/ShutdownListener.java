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
package org.rhq.enterprise.server.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cloud.TopologyManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * <p>This listens for the RHQ server's shutdown notification and when it hears it, will start shutting down RHQ
 * components that need to clean up.</p>
 *
 * <p>This session bean will depend on every over session bean in the deployment. Its class name is processed by the
 * org.rhq.enterprise.startup.deployment.RhqShutdownDependenciesProcessor which is installed by the RHQ Startup
 * Subsystem. This is completely AS7 specific and is a work around AS7 shutting down session beans in parallel.</p>
 *
 * <p><strong>DO NOT CHANGE THIS CLASS FULLY QUALIFIED NAME</strong> org.rhq.enterprise.server.core.ShutdownListener
 * or make the change in the DUP accordingly.</p>
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 * @author Joseph Marques
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ShutdownListener {
    private final Log log = LogFactory.getLog(ShutdownListener.class);

    private final String RHQ_SHUTDOWN_TIME_LOG_FILE = "rhq-shutdown-time.dat";
    private final String RHQ_DB_TYPE_MAPPING_PROPERTY = "rhq.server.database.type-mapping";

    @EJB
    private SchedulerLocal schedulerBean;

    @EJB
    private ServerManagerLocal serverManager;

    @EJB
    private TopologyManagerLocal topologyManager;

    @Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @Resource(name = "NoTx_RHQ_DS", mappedName = RHQConstants.NO_TX_DATASOURCE_JNDI_NAME)
    @SuppressWarnings("unused")
    // Prevent the server from closing the NoTx datasource before RHQ EAR
    // NoTxDS is used by  the Quartz scheduler
    private DataSource noTxDataSource;

    private File shutdownTimeLogFile;

    /**
     * This is called when the shutdown notification is received from the JBoss server. This gives a chance for us to
     * cleanly shutdown our application in an orderly fashion.
     */
    public void handleNotification() {
        log.info("Shutdown listener has been told we are shutting down - starting to clean up now...");
        logShutdownTime();
        stopScheduler();
        updateServerOperationMode();
        stopEmbeddedDatabase();
        log.info("Shutdown listener completed its shutdown tasks. It is safe to shutdown now.");
    }

    /**
     * This is the file that the {@link ShutdownListener} will write to when it logs
     * the last time the server was shutdown.
     *
     * @return shutdown time log file location
     * @throws Exception
     */
    public File getShutdownTimeLogFile() throws Exception {
        if (shutdownTimeLogFile == null) {
            try {
                CoreServerMBean coreServer = LookupUtil.getCoreServer();
                File dataDir = coreServer.getJBossServerDataDir();
                File timeFile = new File(dataDir, RHQ_SHUTDOWN_TIME_LOG_FILE);
                if (!timeFile.exists()) {
                    ByteArrayInputStream data = new ByteArrayInputStream("0".getBytes());
                    FileUtil.writeFile(data, timeFile);
                }
                shutdownTimeLogFile = timeFile;
            } catch (Exception e) {
                // only show ugly stack traces if the user runs the server in debug mode
                if (log.isDebugEnabled()) {
                    log.warn("Failed to get shutdown time log file", e);
                } else {
                    log.warn("Failed to get shutdown time log file: " + e.getMessage());
                }
                throw e;
            }
        }

        return shutdownTimeLogFile;
    }

    /**
     * Stores the current epoch millis time in a file in the data directory. This is used by the
     * StartupBean so it knows how long to wait (if at all) before completing startup (this is to
     * give enough time to allow agents to realize the server has been down).
     */
    private void logShutdownTime() {
        try {
            File shutdownTimeLogFile = getShutdownTimeLogFile();
            ByteArrayInputStream input = new ByteArrayInputStream(String.valueOf(System.currentTimeMillis()).getBytes());
            FileUtil.writeFile(input, shutdownTimeLogFile);
        } catch (Throwable t) {
            // only show ugly stack traces if the user runs the server in debug mode
            if (log.isDebugEnabled()) {
                log.warn("Failed to store shutdown time", t);
            } else {
                log.warn("Failed to store shutdown time: " + t.getMessage());
            }
        }
    }

    /**
     * This will shutdown the scheduler.
     */
    private void stopScheduler() {
        try {
            if (schedulerBean.isStarted()) {
                log.info("Shutting down the scheduler gracefully - currently running jobs will be allowed to finish...");
                schedulerBean.shutdown(true);
                log.info("The scheduler has been shutdown and all jobs are done.");
            } else log.info("No need for shutting down the scheduler, because it is not running.");
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
                topologyManager.updateServerMode(LookupUtil.getSubjectManager().getOverlord(), new Integer[] { server.getId() }, OperationMode.DOWN);
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
