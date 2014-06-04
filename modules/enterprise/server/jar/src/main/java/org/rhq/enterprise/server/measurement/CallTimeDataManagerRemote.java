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

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Public API for CallTime Data.
 */
@Remote
public interface CallTimeDataManagerRemote {

    /**
     * @param subject
     * @param scheduleId The MeasurementSchedule id
     * @param beginTime in millis
     * @param endTime in millis
     * @param pc
     * @return not null
     */
    PageList<CallTimeDataComposite> findCallTimeDataForResource(Subject subject, int scheduleId, long beginTime,
        long endTime, PageControl pc);
    /**
     * Compared to {@link #findCallTimeDataForResource(Subject, int, long, long, PageControl)}
     * this method returns more detailed call-time data not grouped by callDestination.
     * @param subject
     * @param scheduleId The MeasurementSchedule id
     * @param beginTime in millis
     * @param endTime in millis
     * @param pc
     * @return not null
     */
    PageList<CallTimeDataComposite> findCallTimeDataRawForResource(Subject subject, int scheduleId, long beginTime, long endTime, PageControl pc);

}