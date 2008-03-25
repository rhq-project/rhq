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
package org.rhq.core.pc.measurement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;

/**
 * @author Greg Hinkle
 */
public class MeasurementSenderRunner implements Callable<MeasurementReport>, Runnable {
    private static final Log LOG = LogFactory.getLog(MeasurementCollectorRunner.class);

    private MeasurementManager measurementManager;

    public MeasurementSenderRunner(MeasurementManager measurementManager) {
        this.measurementManager = measurementManager;
    }

    public MeasurementReport call() throws Exception {
        MeasurementReport report = this.measurementManager.swapReport();
        if (report == null) {
            LOG.error("Measurement report is null - nothing to do.");
            return null;
        }

        filterUnchangedTraits(report);
        perMinuteItizeData(report);

        if (report.getDataCount() > 0) {
            LOG.info("Measurement collection for [" + report.getDataCount() + "] metrics took " +
                    report.getCollectionTime() + "ms - sending report to Server...");
            this.measurementManager.sendMeasurementReport(report);
        } else {
            LOG.debug("Measurement report contains no data - not sending to Server.");
        }
        
        return report;
    }

    private void perMinuteItizeData(MeasurementReport report) {
        Iterator<MeasurementDataNumeric> iter = report.getNumericData().iterator();
        while (iter.hasNext()) {
            MeasurementData d = iter.next();

            MeasurementDataNumeric numeric = (MeasurementDataNumeric) d;
            if (numeric.isPerMinuteCollection()) {
                Double perMinuteValue = this.measurementManager.updatePerMinuteMetric(numeric);
                if (perMinuteValue == null) {
                    // This is the first collection, don't return the value yet
                    iter.remove();
                } else {
                    // set the value to the transformed rate value
                    numeric.setValue(perMinuteValue);
                }
            }
        }
    }

    private void filterUnchangedTraits(MeasurementReport report) {
        // TODO This is horribly inefficient because we're iterating the whole collection list for every set of collections
        // now filter TRAITS that have not changed since the last time
        List<MeasurementDataTrait> duplicates = new ArrayList<MeasurementDataTrait>();
        for (MeasurementData item : report.getNumericData()) {
            if (item instanceof MeasurementDataTrait) {
                MeasurementDataTrait trait = (MeasurementDataTrait) item;

                if (!this.measurementManager.checkTrait(trait.getScheduleId(), trait.getValue())) // we have an old value and it is the same as this
                {
                    duplicates.add(trait);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Trait " + trait.getName() + " with id " + trait.getScheduleId() + " is a dup");
                    }
                }
            }
        }

        report.getNumericData().removeAll(duplicates);
    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            LOG.error("Could not send measurement report", e);
        }
    }
}