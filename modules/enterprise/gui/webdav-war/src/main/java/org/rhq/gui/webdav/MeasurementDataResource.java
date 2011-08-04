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

package org.rhq.gui.webdav;

import java.util.Date;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The measurement data WebDAV resource that provides information on metrics of a managed resource.
 * 
 * @author John Mazzitelli
 */
public class MeasurementDataResource extends GetableBasicResource {

    private String content;
    private List<MetricDisplaySummary> measurements;

    public MeasurementDataResource(Subject subject, Resource managedResource) {
        super(subject, managedResource);
    }

    public String getUniqueId() {
        return "metrics_" + getManagedResource().getId();
    }

    public String getName() {
        return "measurement_data.xml";
    }

    /**
     * We do not reply with any relevent date. The only way for a relevent date to
     * be calculated is for us to actually query the database for the measurement data.
     * To avoid querying the DB t(which is expensive), we simply return the current
     * date, since the majority of the time, a new measurement collection interval
     * will have triggered in the time the user took between getting the measurement data.
     */
    public Date getModifiedDate() {
        return new Date();
    }

    /**
     * The created date is the date of the resource itself was created.
     */
    public Date getCreateDate() {
        return new Date(getManagedResource().getCtime());
    }

    protected String loadContent() {
        if (this.content == null) {
            StringBuilder str = new StringBuilder();
            str.append("<?xml version=\"1.0\"?>\n");
            str.append("<measurements>\n");
            for (MetricDisplaySummary meas : getMeasurements()) {
                str.append("   <measurement>\n");
                str.append("      <name>").append(meas.getLabel()).append("</name>\n");
                str.append("      <description>").append(meas.getDescription()).append("</description>\n");
                str.append("      <alert-count>").append(meas.getAlertCount()).append("</alert-count>\n");
                str.append("      <min-value>").append(meas.getMinMetric().getValue()).append("</min-value>\n");
                str.append("      <max-value>").append(meas.getMaxMetric().getValue()).append("</max-value>\n");
                str.append("      <avg-value>").append(meas.getAvgMetric().getValue()).append("</avg-value>\n");
                str.append("      <last-value>").append(meas.getLastMetric().getValue()).append("</last-value>\n");
                str.append("      <units>").append(meas.getUnits()).append("</units>\n");
                str.append("      <begin-time>").append(new Date(meas.getBeginTimeFrame())).append("</begin-time>\n");
                str.append("      <end-time>").append(new Date(meas.getEndTimeFrame())).append("</end-time>\n");
                str.append("      <schedule-id>").append(meas.getScheduleId()).append("</schedule-id>\n");
                str.append("   </measurement>\n");
            }
            str.append("</measurements>\n");
            this.content = str.toString();
        }
        return this.content;
    }

    private List<MetricDisplaySummary> getMeasurements() {
        if (this.measurements == null) {

            int resourceId = getManagedResource().getId();

            // determine the user's preferences so we know how far back to get the measurement data
            MeasurementPreferences measurementPrefs = new MeasurementPreferences(getSubject());
            MetricRangePreferences range = measurementPrefs.getMetricRangePreferences();

            // get all the enabled measurement schedules
            MeasurementScheduleManagerLocal sm = LookupUtil.getMeasurementScheduleManager();
            List<MeasurementSchedule> measurementSchedules = sm.findSchedulesForResourceAndType(getSubject(),
                resourceId, DataType.MEASUREMENT, null, true);
            int[] scheduleIds = new int[measurementSchedules.size()];
            int i = 0;
            for (MeasurementSchedule sched : measurementSchedules) {
                scheduleIds[i++] = sched.getId();
            }

            // now get the measurement data for all the scheduled as far back as the user's range preferences says so
            MeasurementChartsManagerLocal chartManager = LookupUtil.getMeasurementChartsManager();
            this.measurements = chartManager.getMetricDisplaySummariesForResource(getSubject(), resourceId,
                scheduleIds, range.begin, range.end);
        }

        return this.measurements;
    }
}
