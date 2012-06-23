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

package org.rhq.enterprise.server.measurement;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.TraitMeasurementCriteria;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.TraitMeasurement;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;

/**
 * @author John Sanda
 */
@Local
public interface MetricsManagerLocal {

    void mergeMeasurementReport(MeasurementReport report);

    void compressPurgeAndTruncate();

    List<MeasurementDataNumeric> findRawData(Subject subject, int scheduleId, long startTime, long endTime);

    List<? extends TraitMeasurement> findResourceTraits(Subject subject, TraitMeasurementCriteria criteria);

    List<MeasurementDataNumericHighLowComposite> findDataForContext(Subject subject, EntityContext context,
        MeasurementSchedule schedule, long beginTime, long endTime);
}
