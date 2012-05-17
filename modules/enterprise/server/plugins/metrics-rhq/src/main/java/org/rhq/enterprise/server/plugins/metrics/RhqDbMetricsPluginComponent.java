/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.plugins.metrics;

import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.metrics.MetricsServerPluginFacet;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class RhqDbMetricsPluginComponent implements MetricsServerPluginFacet, ServerPluginComponent {

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void insertMetrics(MeasurementReport report) {
        MeasurementDataManagerLocal dataMgr = LookupUtil.getMeasurementDataManager();
        //dataMgr.mergeMeasurementReport(report);

        if (report.getNumericData() != null && !report.getNumericData().isEmpty()) {
            dataMgr.addNumericData(report.getNumericData());
        }
        if (report.getTraitData() != null && !report.getTraitData().isEmpty()) {
            dataMgr.addTraitData(report.getTraitData());
        }
        if (report.getCallTimeData() != null && !report.getCallTimeData().isEmpty()) {
            CallTimeDataManagerLocal callTimeDataMgr = LookupUtil.getCallTimeDataManager();
            callTimeDataMgr.addCallTimeData(report.getCallTimeData());
        }
    }
}
