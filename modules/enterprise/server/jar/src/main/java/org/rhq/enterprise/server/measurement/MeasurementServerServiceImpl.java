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
package org.rhq.enterprise.server.measurement;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The remote POJO implementation that takes measurement data from an agent and merges that data into the JON Server's
 * database.
 */
public class MeasurementServerServiceImpl implements MeasurementServerService {
    private Log log = LogFactory.getLog(MeasurementServerServiceImpl.class);

    public void mergeMeasurementReport(MeasurementReport report) {
        long start = System.currentTimeMillis();
        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        dataManager.mergeMeasurementReport(report);
        long time = (System.currentTimeMillis() - start);

        if (time >= 10000L) {
            log.info("Performance: measurement merge [" + report.getDataCount() + "] timing (" + time + ")ms");
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: measurement merge [" + report.getDataCount() + "] timing (" + time + ")ms");
        }
    }

    public Set<ResourceMeasurementScheduleRequest> getLatestSchedulesForResourceId(int resourceId,
        boolean getChildSchedules) {
        MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();

        long start = System.currentTimeMillis();
        Set<ResourceMeasurementScheduleRequest> results = measurementScheduleManager
            .getSchedulesForResourceAndItsDescendants(resourceId, getChildSchedules);
        long time = (System.currentTimeMillis() - start);

        if (time >= 10000L) {
            log.info("Performance: get measurement schedules timing: resource/count/millis=" + resourceId + '/'
                + results.size() + '/' + time);
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: get measurement schedules timing: resource/count/millis=" + resourceId + '/'
                + results.size() + '/' + time);
        }

        // for those resources that had one or more schedules created, we need to apply alert templates to them
        AlertTemplateManagerLocal alertTemplateManager = LookupUtil.getAlertTemplateManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        start = System.currentTimeMillis();
        for (ResourceMeasurementScheduleRequest resultItem : results) {
            if (resultItem.getCreatedCount() > 0) {
                applyAlertTemplate(resultItem.getResourceId(), false, alertTemplateManager, subjectManager);
            }
        }
        time = (System.currentTimeMillis() - start);
        if (time >= 10000L) {
            log.info("Performance: apply alert templates timing: millis=" + time);
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: apply alert templates timing: millis=" + time);
        }

        return results;
    }

    /**
     * Applies alert templates as necessary to the specified Resource and, as specified, its descendants. This
     * should only be requested for any particular resource one time, typically as it's committed to inventory.
     *   
     * @param resourceId a {@link Resource} id
     * @param descendants true if the resource's descendants should be included, or false if not
     * @param alertTemplateManager
     * @param subjectManager 
     */
    private void applyAlertTemplate(int resourceId, boolean descendants,
        AlertTemplateManagerLocal alertTemplateManager, SubjectManagerLocal subjectManager) {

        try {
            Subject overlord = subjectManager.getOverlord();
            alertTemplateManager.updateAlertDefinitionsForResource(overlord, resourceId, descendants);
        } catch (Exception e) {
            log.warn("Could not apply alert templates for resourceId = " + resourceId, e);
        }
    }
}