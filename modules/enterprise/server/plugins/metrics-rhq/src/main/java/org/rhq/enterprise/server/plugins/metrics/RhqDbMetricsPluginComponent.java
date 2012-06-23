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

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.criteria.TraitMeasurementCriteria;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.MeasurementCompressionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.bundle.MetricsException;
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
    public List<MeasurementDataNumericHighLowComposite> findDataForContext(Subject subject, EntityContext context,
        MeasurementSchedule schedule, long beginTime, long endTime) {
        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        List<List<MeasurementDataNumericHighLowComposite>> data = dataManager.findDataForContext(subject, context,
            schedule.getDefinition().getId(), beginTime, endTime, 60);

        if (data.isEmpty()) {
            return Collections.emptyList();
        }
        return data.get(0);
    }

    @Override
    public List<MeasurementDataNumeric> findRawData(Subject subject, int scheduleId, long startTime, long endTime) {
        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        return dataManager.findRawData(subject, scheduleId, startTime, endTime);
    }

    @Override
    public PageList<MeasurementDataTrait> findTraitsByCriteria(Subject subject, TraitMeasurementCriteria criteria) {
        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        return dataManager.findTraitsByCriteria(subject, new MeasurementDataTraitCriteria(criteria));
    }

    @Override
    public void insertMetrics(MeasurementReport report) {
        MeasurementDataManagerLocal dataMgr = LookupUtil.getMeasurementDataManager();
        dataMgr.mergeMeasurementReport(report);
    }

    @Override
    public void calculateAggregates() {
        MeasurementCompressionManagerLocal compressionMgr = LookupUtil.getMeasurementCompressionManager();
        try {
            compressionMgr.compressPurgeAndTruncate();
        } catch (SQLException e) {
            throw new MetricsException(e);
        }
    }
}
