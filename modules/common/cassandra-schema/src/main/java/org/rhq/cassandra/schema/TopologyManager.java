/*
 *
 *  RHQ Management Platform
 *  Copyright (C) 2005-2012 Red Hat, Inc.
 *  All rights reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation, and/or the GNU Lesser
 *  General Public License, version 2.1, also as published by the Free
 *  Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License and the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and the GNU Lesser General Public License along with this program;
 *  if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.schema;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Stefan Negrea
 */
class TopologyManager extends AbstractManager {

    private static final String TOPOLOGY_BASE_FOLDER = "topology";

    private final Log log = LogFactory.getLog(TopologyManager.class);

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

    public TopologyManager(String username, String password, String[] nodes, int cqlPort,
        SessionManager sessionManager) {
        super(username, password, nodes, cqlPort, sessionManager);
    }

    /**
     * Updates cluster topology settings:
     * 1) replication factor
     * 2) gc grace period
     *
     * @return true if update successful, false otherwise.
     */
    public void updateTopology() {
        initClusterSession();
        if (schemaExists()) {
            log.info("Applying topology updates...");
            updateReplicationFactor();
            updateGCGrace();
        } else {
            log.info("Topology updates cannot be applied because the schema is not installed.");
        }
    }

    /**
     * Update replication factor based on the current set of storage nodes.
     *
     * @return true if successful, false otherwise.
     */
    private void updateReplicationFactor() {
        log.info("Starting to execute " + Task.UpdateReplicationFactor + " task.");

        int newReplicationFactor = calculateNewReplicationFactor();
        int existingReplicationFactor = queryReplicationFactor();
        if (existingReplicationFactor == newReplicationFactor) {
            log.info("No need to update replication factor. Replication factor already " + newReplicationFactor);
        } else {
            execute(new UpdateFile(Task.UpdateReplicationFactor.getFile()), "replication_factor", newReplicationFactor
                + "");
            log.info("Updated replication factor from " + existingReplicationFactor + " to  " + newReplicationFactor);
        }

        log.info("Successfully executed " + Task.UpdateReplicationFactor + " task.");
    }

    /**
     * Update gc grace interval based on the current set of storage nodes.
     */
    private void updateGCGrace() {
        log.info("Starting to execute " + Task.UpdateGCGrace + " task.");

        int gcGraceSeconds = 864000;
        if (getActualClusterSize() == 1) {
            gcGraceSeconds = 0;
        } else {
            gcGraceSeconds = 691200; // 8 days
        }

        updateGCGraceSeconds("rhq.raw_metrics", gcGraceSeconds);
        updateGCGraceSeconds("rhq.one_hour_metrics", gcGraceSeconds);
        updateGCGraceSeconds("rhq.six_hour_metrics", gcGraceSeconds);
        updateGCGraceSeconds("rhq.twenty_four_hour_metrics", gcGraceSeconds);
        updateGCGraceSeconds("rhq.schema_version", gcGraceSeconds);
        updateGCGraceSeconds("rhq.metrics_cache", gcGraceSeconds);
        updateGCGraceSeconds("rhq.metrics_cache_index", gcGraceSeconds);

        log.info("Updated gc_grace_seconds to " + gcGraceSeconds);
        log.info("Successfully executed " + Task.UpdateGCGrace + " task.");
    }

    private void updateGCGraceSeconds(String table, int gcGraceSeconds) {
        execute("ALTER COLUMNFAMILY " + table + " WITH gc_grace_seconds = " + gcGraceSeconds);
    }
}
