/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.storage.maintenance.step;

import static org.rhq.core.domain.storage.MaintenanceStep.JobType.FAILED_ANNOUNCE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public class AnnounceStorageNode extends ResourceOperationStepRunner {

    private static final Log log = LogFactory.getLog(AnnounceStorageNode.class);

    public AnnounceStorageNode() {
        super("announce");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        Configuration configuration = step.getConfiguration();
        PropertySimple property = configuration.getSimple(JobProperties.FAILURE_STRATEGY);
        if (property != null) {
            return StepFailureStrategy.fromString(property.getStringValue());
        }

        // If we are a single node cluster we have to abort since the new node will not be
        // able to gossip with the existing node. But if we are running multiple nodes, we
        // can since the new node will be able to bootstrap from them.
        if (clusterSnapshot.size() == 1) {
            return StepFailureStrategy.ABORT;
        }
        return StepFailureStrategy.CONTINUE;
    }

    @Override
    public StorageMaintenanceJob createNewJobForFailedStep() {
        Configuration newJobConfiguration = new Configuration.Builder()
            .addSimple(JobProperties.TARGET, getTarget())
            .addSimple("newNodeAddress", step.getConfiguration().getMap(JobProperties.PARAMETERS)
                .getSimple("address").getStringValue())
            .build();

        return new StorageMaintenanceJob(FAILED_ANNOUNCE, FAILED_ANNOUNCE + " " + getTarget(), newJobConfiguration);
    }
}
