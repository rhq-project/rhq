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

import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.StorageNode;

/**
 * @author Stefan Negrea
 */
public class TopologyManager extends AbstractManager {

    private final Log log = LogFactory.getLog(TopologyManager.class);

    private static final String TOPOLOGY_BASE_FOLDER = "topology";


    private enum Task {
        UpdateReplicationFactor("0001.xml"),
        UpdateGCGrace("0002.xml");

        private final String file;

        private Task(String file) {
            this.file = file;
        }

        protected String getFile() {
            return TOPOLOGY_BASE_FOLDER + "/" + this.file;
        }
    }

    public TopologyManager(String username, String password, List<StorageNode> nodes) {
        super(username, password, nodes);
    }

    public void updateTopology() throws Exception {
        initCluster();
        if (schemaExists()) {
            log.info("Applying topology updates...");
            this.updateReplicationFactor(nodes.size());
            this.updateGCGrace(nodes.size());
        } else {
            log.info("Topology updates cannot be applied because the schema is not installed.");
        }
        shutdown();
    }

    private boolean updateReplicationFactor(int numberOfNodes) throws Exception {
        log.info("Starting to execute " + Task.UpdateReplicationFactor + " task.");

        int replicationFactor = 1;
        if (numberOfNodes < 4) {
            replicationFactor = numberOfNodes;
        } else {
            replicationFactor = 3;
        }

        log.info("Applying file " + Task.UpdateReplicationFactor.getFile() + " for " + Task.UpdateReplicationFactor
            + " task.");
        for (String query : this.getSteps(Task.UpdateReplicationFactor.getFile())) {
            executedPreparedStatement(query, replicationFactor);
        }
        log.info("File " + Task.UpdateReplicationFactor.getFile() + " applied for " + Task.UpdateReplicationFactor
            + " task.");

        log.info("Successfully executed " + Task.UpdateReplicationFactor + " task.");
        return true;
    }

    private boolean updateGCGrace(int numberOfNodes) throws Exception {
        log.info("Starting to execute " + Task.UpdateGCGrace + " task.");

        int gcGraceSeconds = 864000;
        if (numberOfNodes == 1) {
            gcGraceSeconds = 0;
        } else {
            gcGraceSeconds = 691200; // 8 days
        }


        log.info("Applying file " + Task.UpdateGCGrace.getFile() + " for " + Task.UpdateGCGrace + " task.");
        for (String query : this.getSteps(Task.UpdateGCGrace.getFile())) {
            executedPreparedStatement(query, gcGraceSeconds);
        }
        log.info("File " + Task.UpdateGCGrace.getFile() + " applied for " + Task.UpdateGCGrace + " task.");

        log.info("Successfully executed " + Task.UpdateGCGrace + " task.");
        return true;
    }

    private void executedPreparedStatement(String query, Object... values) {
        String formattedQuery = String.format(query, values);
        log.info("Statement: \n" + formattedQuery);
        PreparedStatement preparedStatement = session.prepare(formattedQuery);
        BoundStatement boundStatement = preparedStatement.bind();
        session.execute(boundStatement);
    }

}
