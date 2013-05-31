/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.schema;

import java.util.Date;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.StorageNode;

/**
 * @author Stefan Negrea
 */
public class VersionManager extends AbstractManager {

    private static final String SCHEMA_BASE_FOLDER = "schema";
    private static final String INSERT_VERSION_QUERY = "INSERT INTO rhq.schema_version (version, time ) VALUES ( ?, ?);";

    private final Log log = LogFactory.getLog(VersionManager.class);

    private enum Task {
        Drop("drop"),
        Create("create"),
        Update("update");

        private final String folder;

        private Task(String folder){
            this.folder = folder;
        }

        protected String getFolder() {
            return SCHEMA_BASE_FOLDER + "/" + this.folder + "/";
        }
    }

    public VersionManager(String username, String password, List<StorageNode> nodes) {
        super(username, password, nodes);
    }

    public void install() throws Exception {
        log.info("Preparing to install schema");
        try {
            initCluster();

            if (!schemaExists()) {
                this.executeTask(Task.Create);
            } else {
                log.info("RHQ schema already exists.");
            }

            this.executeTask(Task.Update);
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        } finally {
            shutdown();
        }
    }

    public void drop() throws Exception {
        log.info("Preparing to drop RHQ schema");
        try {
            initCluster();

            if (schemaExists()) {
                this.executeTask(Task.Drop);
            } else {
                log.info("RHQ schema does not exist. Drop operation not required.");
            }
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        } finally {
            shutdown();
        }
    }

    private void executeTask(Task task) throws Exception {
        try {
            log.info("Starting to execute " + task + " task.");

            List<String> updateFiles = this.getUpdateFiles(task.getFolder());

            if (Task.Update.equals(task)) {
                int currentSchemaVersion = this.getSchemaVersion();
                log.info("Current schema version is " + currentSchemaVersion);
                this.removeAppliedUpdates(updateFiles, currentSchemaVersion);
            }

            if (updateFiles.size() == 0 && Task.Update.equals(task)) {
                log.info("RHQ schema is current! No updates applied.");
            }

            for (String updateFile : updateFiles) {
                log.info("Applying file " + updateFile + " for " + task + " task.");
                for (String step : getSteps(updateFile)) {
                    log.info("Statement: \n" + step);
                    session.execute(step);
                }

                if (Task.Update.equals(task)) {
                    this.updateSchemaVersion(updateFile);
                }

                log.info("File " + updateFile + " applied for " + task + " task.");
            }
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }

        log.info("Successfully executed " + task + " task.");
    }

    private void updateSchemaVersion(String updateFileName) {
        PreparedStatement preparedStatement = session.prepare(INSERT_VERSION_QUERY);
        BoundStatement boundStatement = preparedStatement.bind(this.extractVersionFromUpdateFile(updateFileName),
            new Date());
        session.execute(boundStatement);
    }
}
