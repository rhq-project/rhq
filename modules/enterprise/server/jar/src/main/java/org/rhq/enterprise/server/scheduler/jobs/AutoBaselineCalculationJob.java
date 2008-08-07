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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import org.rhq.enterprise.server.util.LookupUtil;

/**
 * <p>This implements {@link StatefulJob} (as opposed to {@link Job}) because we do not need nor want this job triggered
 * concurrently. That is, we don't want to calculate baselines concurrently by more than one job.</p>
 */
public class AutoBaselineCalculationJob implements StatefulJob {
    private final Log log = LogFactory.getLog(AutoBaselineCalculationJob.class);

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            LookupUtil.getMeasurementBaselineManager().calculateAutoBaselines();
        } catch (Exception e) {
            throw new JobExecutionException("Unable to calculate baselines.", e, false);
        }
    }
}