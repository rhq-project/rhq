/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.quartz.CronTrigger;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.OperationGWTService;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create.ExecutionSchedule;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class OperationGWTServiceImpl extends AbstractGWTServiceImpl implements OperationGWTService {

    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();

    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(ResourceOperationHistoryCriteria criteria) {
        return SerialUtility.prepare(operationManager.findResourceOperationHistoriesByCriteria(getSessionSubject(), criteria),
                "OperationService.findResourceOperationHistoriesByCriteria");

    }

    public PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(GroupOperationHistoryCriteria criteria) {
        return SerialUtility.prepare(operationManager.findGroupOperationHistoriesByCriteria(getSessionSubject(), criteria),
                "OperationService.findGroupOperationHistoriesByCriteria");
    }


    public void scheduleResourceOperation(
            int resourceId, String operationName, Configuration parameters, ExecutionSchedule schedule, String description, int timeout)
            throws RuntimeException {
        ResourceOperationSchedule opSchedule;
        try {

            if (schedule.getStart() == ExecutionSchedule.Start.Immediately) {
                opSchedule = operationManager.scheduleResourceOperation(getSessionSubject(), resourceId, operationName, 0, 0, 0, 0, parameters, description);
            } else {

                CronTrigger ct = new CronTrigger(
                        "resource " + resourceId + "_" + operationName,
                        "group",
                        schedule.getCronString());

                opSchedule = operationManager.scheduleResourceOperation(getSessionSubject(), resourceId, operationName, parameters, ct, description);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unabled to schedule operation execution" + e.getMessage());
        }
    }


}
