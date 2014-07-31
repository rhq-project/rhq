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

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;

/**
 * @author Stefan Negrea
 *
 */
public class UpdateStorageNodeEntity implements MaintenanceStepRunner {

    @EJB
    StorageNodeManagerLocal storageNodeManagerBean;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    public UpdateStorageNodeEntity() {
    }

    @Override
    public void execute(MaintenanceStep step) {
        if (step.getStorageNode() == null || step.getStorageNode() == null)
            throw new IllegalArgumentException("Storage node not found.");

        step.getStorageNode().setCqlPort(Integer.parseInt(step.getArgs()));

        entityManager.persist(step.getStorageNode());
    }
}
