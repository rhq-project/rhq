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
package org.rhq.server.storage.maintenance.step;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.storage.MaintenanceJob;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.core.domain.storage.MaintenanceStep.Type;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;

/**
 * @author Stefan Negrea
 *
 */
public class UpdateStorageNodeEntity implements MaintenanceStepFacade {

    @EJB
    StorageNodeManagerLocal storageNodeManagerBean;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    public UpdateStorageNodeEntity() {
    }

    public MaintenanceStep build(MaintenanceJob job, int stepNumber, String[] storageNodes, String affectedNode) {
        MaintenanceStep step = new MaintenanceStep();
        step.setStep(stepNumber)
            .setName(UpdateStorageNodeEntity.class.getSimpleName())
            .setNodeAddress(affectedNode)
            .setType(Type.EntityUpdate)
            .setSequential(true)
            .setTimeout(1000)
            .setMaintenanceJob(job);

        return step;
    }

    @Override
    public void execute(MaintenanceStep step) {
        StorageNode storageNode = storageNodeManagerBean.findStorageNodeByAddress(step.getNodeAddress());
        if (storageNode == null || storageNode.getResource() == null)
            throw new IllegalArgumentException("Storage node not found.");

        storageNode.setCqlPort(Integer.parseInt(step.getArgs()));

        entityManager.persist(storageNode);
    }
}
