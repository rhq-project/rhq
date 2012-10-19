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
package org.rhq.plugins.perftest.calltime;

import java.util.Date;

import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;

/**
 * Create CallTime data. This is a very simple implementation that delivers
 * only one item per call. Call url is /base/+ &lt;id> where id is the schedule
 * id of the request.
 * @author Heiko W. Rupp
 */
public class SimpleCallTimeDataFactory implements CalltimeFactory {

    public CallTimeData nextValue(MeasurementScheduleRequest request) {

        String id = String.valueOf(request.getScheduleId());
        CallTimeData data = new CallTimeData(request);
        long duration = (long) (10L + Math.random()*100);
        data.addCallData("/base/" + id, new Date(), duration);

        return data;
    }
}
