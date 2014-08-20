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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.JobProperties;
import org.rhq.enterprise.server.storage.maintenance.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public class RunRepair extends ResourceOperationStepRunner {

    public RunRepair() {
        super("repair");
    }

    @Override
    public void execute() throws StepFailureException {
        super.execute();
        Configuration results = history.getResults();
        PropertyList failedSessions = results.getList("failedSessions");
        if (failedSessions != null && !failedSessions.getList().isEmpty()) {
            // TODO log or include in the exception the params
            throw new StepFailureException("Resource operation [repair] against " + getTarget() + " failed for " +
                failedSessions.getList().size() + " range(s)");
        }
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.CONTINUE;
    }

    @Override
    public StorageMaintenanceJob createNewJobForFailedStep() {
        Configuration results = history.getResults();
        PropertyList failedSessions = results.getList("failedSessions");

        Configuration jobParams = new Configuration();
        jobParams.put(new PropertySimple(JobProperties.TARGET, getTarget()));

        PropertyList ranges = new PropertyList("ranges");
        for (Property p : failedSessions.getList()) {
            ranges.add(p.deepCopy(false));
        }

        jobParams.put(ranges);
        jobParams.put(new PropertySimple("keyspace", results.getSimpleValue("keyspace")));
        jobParams.put(results.get("tables").deepCopy(false));

        return new StorageMaintenanceJob(MaintenanceStep.JobType.FAILED_REPAIR, "Repair " + getTarget(), jobParams);
    }
}
