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

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementScheduleGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class MeasurementScheduleGWTServiceImpl extends AbstractGWTServiceImpl implements MeasurementScheduleGWTService {
    private static final long serialVersionUID = 1L;

    private MeasurementScheduleManagerLocal schedulerManager = LookupUtil.getMeasurementScheduleManager();

    @Override
    public ArrayList<MeasurementSchedule> findSchedulesForResourceAndType(int resourceId, DataType dataType,
        DisplayType displayType, boolean enabledOnly) throws RuntimeException {
        try {
            List<MeasurementSchedule> schedules = schedulerManager.findSchedulesForResourceAndType(getSessionSubject(),
                resourceId, dataType, displayType, enabledOnly);
            return SerialUtility.prepare(new ArrayList<MeasurementSchedule>(schedules),
                "MeasurementSchedule.findSchedulesForResourceAndType");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }
}
