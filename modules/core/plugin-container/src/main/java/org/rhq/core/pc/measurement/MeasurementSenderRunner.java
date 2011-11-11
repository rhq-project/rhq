/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.measurement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;

/**
 * @author Greg Hinkle
 */
public class MeasurementSenderRunner implements Callable<MeasurementReport>, Runnable {
    private static final Log LOG = LogFactory.getLog(MeasurementSenderRunner.class);

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
        cleanseInvalidNumericValues(report);
        this.measurementManager.perMinuteItizeData(report);

        if (report.getDataCount() > 0) {
            LOG.info("Measurement collection for [" + report.getDataCount() + "] metrics took "
                + report.getCollectionTime() + "ms - sending report to Server...");
            this.measurementManager.sendMeasurementReport(report);
        } else {
            LOG.debug("Measurement report contains no data - not sending to Server.");
        }

        return report;
    }

    private void filterUnchangedTraits(MeasurementReport report) {

        // now filter TRAITS that have not changed since the last time
        List<MeasurementDataTrait> duplicates = new ArrayList<MeasurementDataTrait>(report.getTraitData().size());

        for (MeasurementDataTrait trait : report.getTraitData()) {
            if (!this.measurementManager.checkTrait(trait.getScheduleId(), trait.getValue())) // we have an old value and it is the same as this
            {
                duplicates.add(trait);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Trait " + trait.getName() + " with id " + trait.getScheduleId() + " is a dup");
                }
            }
        }

        report.getTraitData().removeAll(duplicates);
    }

    private void cleanseInvalidNumericValues(MeasurementReport report) {
        Iterator<MeasurementDataNumeric> iter = report.getNumericData().iterator();
        while (iter.hasNext()) {
            MeasurementDataNumeric numeric = iter.next();
            Double value = numeric.getValue();
            if (value == null || value.isInfinite() || value.isNaN()) {
                if (LOG.isDebugEnabled()) {
                    String stringValue = getStringValue(value);
                    LOG.debug("Numeric metric [" + numeric.getName() + "] with schedule id [" + numeric.getScheduleId()
                        + "] is invalid - value is [" + stringValue + "].");
                }
                iter.remove();
            }
        }
    }

    private String getStringValue(Double value) {
        String stringValue;
        if (value == null) {
            stringValue = "null";
        } else if (value.isNaN()) {
            stringValue = "Double.NaN";
        } else if (value == Double.POSITIVE_INFINITY) {
            stringValue = "Double.POSITIVE_INFINITY";
        } else if (value == Double.NEGATIVE_INFINITY) {
            stringValue = "Double.NEGATIVE_INFINITY";
        } else {
            stringValue = value.toString();
        }
        return stringValue;
    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            LOG.error("Could not send measurement report", e);
        }
    }
}