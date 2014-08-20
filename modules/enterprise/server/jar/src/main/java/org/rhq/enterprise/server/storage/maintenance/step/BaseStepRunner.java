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

import java.util.Set;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.enterprise.server.storage.maintenance.MaintenanceStepRunner;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;

/**
 * @author John Sanda
 */
public abstract class BaseStepRunner implements MaintenanceStepRunner {

    protected Set<String> clusterSnapshot;

    protected MaintenanceStep step;

    protected StorageNodeManagerLocal storageNodeManager;

    protected OperationManagerLocal operationManager;

    protected SubjectManagerLocal subjectManager;

    protected StorageClientManager storageClientManager;

    @Override
    public void setClusterSnapshot(Set<String> clusterSnapshot) {
        this.clusterSnapshot = clusterSnapshot;
    }

    @Override
    public void setStep(MaintenanceStep step) {
        this.step = step;
    }

    @Override
    public void setStorageNodeManager(StorageNodeManagerLocal storageNodeManager) {
        this.storageNodeManager = storageNodeManager;
    }

    @Override
    public void setOperationManager(OperationManagerLocal operationManager) {
        this.operationManager = operationManager;
    }

    @Override
    public void setStorageClientManager(StorageClientManager storageClientManager) {
        this.storageClientManager = storageClientManager;
    }

    @Override
    public void setSubjectManager(SubjectManagerLocal subjectManager) {
        this.subjectManager = subjectManager;
    }

    @Override
    public StorageMaintenanceJob createNewJobForFailedStep() {
        return null;
    }
}
