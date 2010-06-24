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
import org.rhq.plugins.perftest.scenario.ConfigurableCallTimeDataGenerator;

/**
 * Calltime data factory that allows to configure parameters such as minimum
 * and maximum number of entries created.
 * @see ConfigurableCallTimeDataGenerator
 * @author Heiko W. Rupp
 */
public class ConfigurableCallTimeDataFactory implements CalltimeFactory {

    private ConfigurableCallTimeDataGenerator generator;

    public ConfigurableCallTimeDataFactory( ConfigurableCallTimeDataGenerator generator) {
        this.generator = generator;
    }

    public CallTimeData nextValue(MeasurementScheduleRequest request) {

        int minMsgCount = generator.getMinMsgCount();
        int maxMsgCount = generator.getMaxMsgCount();
        int numSubPath  = generator.getNumberSubPaths();
        int minDuration = generator.getMinDuration();
        int maxDuration = generator.getMaxDuration();


        String id = String.valueOf(request.getScheduleId());
        CallTimeData data = new CallTimeData(request);

        int countInterval = (int) ((maxMsgCount - minMsgCount) * Math.random());

        for (int count = 0 ; count < minMsgCount + countInterval ; count ++) {
            long duration = (long) (minDuration + Math.random()*(maxDuration-minDuration));
            String path = "/base/" + id;
            if (numSubPath > 0) {
                path = path + "/" + (int) (Math.random() * numSubPath);
            }
            data.addCallData(path, new Date(), duration);
        }

        return data;

    }
}
