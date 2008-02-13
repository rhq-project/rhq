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
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.Date;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * <p>This implements {@link StatefulJob} (as opposed to {@link Job}) because we do not need nor want this job triggered
 * concurrently. That is, we don't want to calculate baselines concurrently by more than one job.</p>
 */
public class AutoBaselineCalculationJob implements StatefulJob {
    private final Log log = LogFactory.getLog(AutoBaselineCalculationJob.class);

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            SystemManagerLocal systemManager = LookupUtil.getSystemManager();
            Properties conf = systemManager.getSystemConfiguration();

            // frequency is how often the baselines are recalculated
            // data set is how far back for a particular scheduled measurement is included in the baseline calcs
            // frequency of 3 days and data set of 10 days means "every 3 days, recalculate baselines automatically.
            // For each scheduled measurement, take their last 10 days worth of data and use that data set
            // as the portion that will be used to get the min/max/average".
            String baselineFrequencyString = conf.getProperty(HQConstants.BaselineFrequency);
            String baselineDataSetString = conf.getProperty(HQConstants.BaselineDataSet);
            String baselineLastCalcTimeString = conf.getProperty(HQConstants.BaselineLastCalculationTime);

            log.debug("Found baseline defaults: " + "frequency=" + baselineFrequencyString + " dataset="
                + baselineDataSetString + " last-calc-time=" + baselineLastCalcTimeString);

            // see if baseline auto-calculations is disabled; if so, return immediately and do nothing
            long frequency = Long.parseLong(baselineFrequencyString);
            if (frequency <= 0) {
                log.debug("System was configured to never auto-calculate baselines, so not calculating them now");
                return;
            }

            // see if its time to auto-calculate the baselines; if not, return immediately and do nothing
            long now = System.currentTimeMillis();
            long lastCalcTime = Long.parseLong(baselineLastCalcTimeString);
            long timeToNextCalc = now - (lastCalcTime + frequency);

            if (timeToNextCalc < 0) {
                long minutes = (-timeToNextCalc) / (1000L * 60);
                long hours = minutes / 60L;
                if (hours > 2) {
                    log.debug("Not time yet to auto-calculate baselines - [" + hours + "] hours more to go");
                } else {
                    log.debug("Not time yet to auto-calculate baselines - [" + minutes + "] minutes more to go");
                }

                return;
            }

            // Its time to auto-calculate the baselines again.
            // Determine how much data we need to calculate baselines for by determining the oldest and youngest
            // measurement data to include in the calculations.
            long dataSet = Long.parseLong(baselineDataSetString);
            long endTime = now;
            long startTime = endTime - dataSet;

            MeasurementBaselineManagerLocal baselineManager = LookupUtil.getMeasurementBaselineManager();
            long computeTime = baselineManager.calculateAutoBaselines(startTime, endTime);

            // everything was calculated successfully, remember this time
            conf = systemManager.getSystemConfiguration(); // reload the config in case it was changed since we started
            conf.setProperty(HQConstants.BaselineLastCalculationTime, String.valueOf(computeTime));
            systemManager.setSystemConfiguration(LookupUtil.getSubjectManager().getOverlord(), conf);

            log.info("Auto-calculation of baselines done. Next scheduled for " + new Date(computeTime + frequency));
        } catch (Exception e) {
            throw new JobExecutionException("Unable to calculate baselines.", e, false);
        }
    }
}