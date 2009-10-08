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
package org.rhq.enterprise.server.test;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.alert.InvalidAlertDefinitionException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

@Stateless
public class AlertTemplateTestBean implements AlertTemplateTestLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AlertTemplateManagerLocal alertTemplateManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    public void cloneAlertTemplate(int alertTemplateId, int numberOfClones) throws ResourceTypeNotFoundException,
        InvalidAlertDefinitionException {

        Subject overlord = subjectManager.getOverlord();
        long currentMillis = System.currentTimeMillis();

        for (int i = 1; i <= numberOfClones; i++) {
            AlertDefinition template = entityManager.find(AlertDefinition.class, alertTemplateId);
            AlertDefinition newTemplate = new AlertDefinition(template);
            String oldName = template.getName();
            int resourceTypeId = template.getResourceType().getId();

            String newName = oldName + " (clone " + i + " at " + currentMillis + ")";
            newTemplate.setName(newName);

            alertTemplateManager.createAlertTemplate(overlord, newTemplate, resourceTypeId);

            entityManager.flush();
            entityManager.clear();
        }

    }

}
