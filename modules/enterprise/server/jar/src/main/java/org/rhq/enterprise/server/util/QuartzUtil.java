/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.server.util;

import java.util.Date;

import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

public class QuartzUtil {
    public static Trigger getFireOnceOffsetTrigger(JobDetail jobDetail, long millis) {
        return getFireOnceTrigger(jobDetail, System.currentTimeMillis() + millis);
    }

    public static Trigger getFireOnceImmediateTrigger(JobDetail jobDetail) {
        return getFireOnceTrigger(jobDetail, System.currentTimeMillis());
    }

    public static Trigger getFireOnceTrigger(JobDetail jobDetail, long fireOn) {
        return getFireOnceTrigger(jobDetail, new Date(fireOn));
    }

    public static Trigger getFireOnceTrigger(JobDetail jobDetail, Date fireOn) {
        Trigger trigger = new SimpleTrigger(jobDetail.getName(), jobDetail.getGroup(), fireOn);

        trigger.setJobName(jobDetail.getName());
        trigger.setJobGroup(jobDetail.getGroup());

        return trigger;
    }

    /**
     * Creates a trigger that will repeat indefinitely starting at the specified time, firing at a specified
     * interval.
     *
     * @param jobDetail the job details
     * @param startTimeMillis the time in epoch milliseconds at which to first start the trigger. If <= 0, it starts
     *                        immediately
     * @param periodMillis the repetition interval
     * @return the trigger to use
     */
    public static Trigger getRepeatingTrigger(JobDetail jobDetail, long startTimeMillis, long periodMillis) {
        Date fireOn = new Date(startTimeMillis <= 0 ? System.currentTimeMillis() : startTimeMillis);

        Trigger trigger = new SimpleTrigger(jobDetail.getName(), jobDetail.getGroup(),
            fireOn, null, SimpleTrigger.REPEAT_INDEFINITELY, periodMillis);

        trigger.setJobName(jobDetail.getName());
        trigger.setJobGroup(jobDetail.getGroup());

        return trigger;
    }
}
