/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.test.measurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

public class BZ821058ResourceComponent implements ResourceComponent<ResourceComponent<?>>, MeasurementFacet {

    public List<Throwable> errors = Collections.synchronizedList(new ArrayList<Throwable>());
    public CountDownLatch getValuesLatch = new CountDownLatch(1);

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws Exception {
    }

    @Override
    public void stop() {
    }

    /**
     * To test BZ 821058, we just need to make sure we get exceptions if we try to
     * manipulate the metrics set when this method is called by the PC.
     */
    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        try {
            for (Iterator<MeasurementScheduleRequest> i = metrics.iterator(); i.hasNext();) {
                MeasurementScheduleRequest metric = i.next();
                report.addData(new MeasurementDataNumeric(metric, new Double(1.0)));
                System.out.println("===============PRE REMOVE");
                i.remove(); // this should not be allowed
                System.out.println("===============POST REMOVE");
            }
        } catch (Throwable e) {
            errors.add(e);
        }

        try {
            System.out.println("===============PRE CLEAR");
            metrics.clear(); // this should not be allowed
            System.out.println("===============POST CLEAR");
        } catch (Throwable e) {
            errors.add(e);
        }

        getValuesLatch.countDown();
        return;
    }
}
