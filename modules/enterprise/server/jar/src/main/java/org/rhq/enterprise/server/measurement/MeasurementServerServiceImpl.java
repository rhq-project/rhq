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
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
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
        long start = System.currentTimeMillis();
        MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
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

        return results;
    }
}