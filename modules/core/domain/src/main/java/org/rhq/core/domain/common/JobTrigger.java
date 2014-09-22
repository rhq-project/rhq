/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.core.domain.common;

import java.io.Serializable;
import java.util.Date;

/**
 * Indicates when a particular job (e.g. invocation of an RHQ Agent plugin operation) should execute.
 * Note, all constructors of this class are private - the create*Trigger() static factory methods
 * must be used to create instances.
 *
 * @author Ian Springer
 */
public class JobTrigger implements Serializable {

    private static final long serialVersionUID = 1L;

    // Together, the startType, recurrenceType, and endType define what type of trigger this is.
    // For the remaining fields in this class, only those that apply to the trigger's type will have non-null values.
    private StartType startType;
    private RecurrenceType recurrenceType;
    // Note, this field will be null when recurrenceType=NONE, since it's not applicable in that case.
    private EndType endType;

    // Fields used by startType=DATETIME triggers
    private Date startDate;

    // Fields used by recurrenceType=REPEAT_INTERVAL triggers
    private Long repeatInterval;
    // endDate and repeatCount are mutually exclusive - only one can have a non-null value;
    // if both are null, the repetition is indefinite.
    private Date endDate;
    private Integer repeatCount;

    // Fields used by recurrenceType=CRON_EXPRESSION triggers
    private String cronExpression;

    public enum StartType {
        /**
         * Start the initial run now.
         */
        NOW,
        /**
         * Start the initial run at a specified date-time in the future.
         */
        DATETIME;
    }

    public enum RecurrenceType {
        /**
         * No recurrence - just run once.
         */
        NONE,
        /**
         * Run every n seconds, starting at the start time of the initial run.
         */
        REPEAT_INTERVAL,
        /**
         * Run on a schedule based on the specified cron expression. The expression must follow
         * <a href="http://www.quartz-scheduler.org/docs/tutorials/crontrigger.html">Quartz
         * cron expression syntax</a>, which is a bit different than Unix cron expression syntax.
         */
        CRON_EXPRESSION;
    }

    public enum EndType {
        /**
         * No end - recurrence is indefinite.
         */
        NEVER,
        /**
         * End recurrence at a specified date/time in the future.
         */
        DATETIME,
        /**
         * End recurrence after n repetitions.
         */
        REPEAT_COUNT;
    }

    public StartType getStartType() {
        return startType;
    }

    public EndType getEndType() {
        return endType;
    }

    public RecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Long getRepeatInterval() {
        return repeatInterval;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Trigger");
        sb.append("[cronExpression='").append(cronExpression).append('\'');
        sb.append(", startType=").append(startType);
        sb.append(", recurrenceType=").append(recurrenceType);
        sb.append(", endType=").append(endType);
        sb.append(", startDate=").append(startDate);
        sb.append(", repeatInterval=").append(repeatInterval);
        sb.append(", endDate=").append(endDate);
        sb.append(", repeatCount=").append(repeatCount);
        sb.append(']');
        return sb.toString();
    }

    /**
     * Create a trigger that will run now, once.
     *
     * @return a trigger that will run now, once
     */
    public static JobTrigger createNowTrigger() {
        return new JobTrigger();
    }

    /**
     * Create a trigger that will run now and repeat on interval indefinitely.
     *
     * @param repeatInterval
     * @return a trigger that will run now and repeat on interval indefinitely
     */
    public static JobTrigger createNowAndRepeatTrigger(long repeatInterval) {
        return new JobTrigger(repeatInterval);
    }

    /**
     * Create a trigger that will run now and repeat on interval until end date/time.
     *
     * @param repeatInterval
     * @param endDate
     * @return a trigger that will run now and repeat on interval until end date/time
     */
    public static JobTrigger createNowAndRepeatTrigger(long repeatInterval, Date endDate) {
        return new JobTrigger(repeatInterval, endDate);
    }

    /**
     * Create a trigger that will run now and repeat on interval n times.
     *
     * @param repeatInterval
     * @param repeatCount
     * @return a trigger that will run now and repeat on interval n times
     */
    public static JobTrigger createNowAndRepeatTrigger(long repeatInterval, int repeatCount) {
        return new JobTrigger(repeatInterval, repeatCount);
    }

    /**
     * Create a trigger that will run at specified date/time, once.
     *
     * @param startDate
     * @return a trigger that will run at specified date/time, once
     */
    public static JobTrigger createLaterTrigger(Date startDate) {
        return new JobTrigger(startDate);
    }

    /**
     * Create a trigger that will run at specified date/time and then repeat on interval indefinitely.
     *
     * @param startDate
     * @param repeatInterval
     * @return a trigger that will run at specified date/time and then repeat on interval indefinitely
     */
    public static JobTrigger createLaterAndRepeatTrigger(Date startDate, long repeatInterval) {
        return new JobTrigger(startDate, repeatInterval);
    }

    /**
     * Create a trigger that will run at specified date/time and then repeat on interval until end date/time.
     *
     * @param startDate
     * @param repeatInterval
     * @param endDate
     * @return a trigger that will run at specified date/time and then repeat on interval until end date/time
     */
    public static JobTrigger createLaterAndRepeatTrigger(Date startDate, long repeatInterval, Date endDate) {
        return new JobTrigger(startDate, repeatInterval, endDate);
    }

    /**
     * Create a trigger that will run at specified date/time and then repeat on interval n times.
     *
     * @param startDate
     * @param repeatInterval
     * @param repeatCount
     * @return a trigger that will run at specified date/time and then repeat on interval n times
     */
    public static JobTrigger createLaterAndRepeatTrigger(Date startDate, long repeatInterval, int repeatCount) {
        return new JobTrigger(startDate, repeatInterval, repeatCount);
    }

    /**
     * Create a trigger that will run on the schedule specified by a cron expression. The expression must follow
     * <a href="http://www.quartz-scheduler.org/docs/tutorials/crontrigger.html">Quartz
     * cron expression syntax</a>, which is a bit different than Unix cron expression syntax.
     *
     * @param cronExpression
     * @return a trigger that will run on the schedule specified by a cron expression
     */
    public static JobTrigger createCronTrigger(String cronExpression) {
        return new JobTrigger(cronExpression);
    }


    private JobTrigger(StartType startType, RecurrenceType recurrenceType, EndType endType) {
        this.startType = startType;
        this.recurrenceType = recurrenceType;
        this.endType = endType;
    }

    // run now, once
    private JobTrigger() {
        this(StartType.NOW, RecurrenceType.NONE, null);
    }

    // run now and repeat on interval indefinitely
    private JobTrigger(long repeatInterval) {
        this(StartType.NOW, RecurrenceType.REPEAT_INTERVAL, EndType.REPEAT_COUNT);
        this.repeatInterval = repeatInterval;
    }

    // run now and repeat on interval until end date/time
    private JobTrigger(long repeatInterval, Date endDate) {
        this(StartType.NOW, RecurrenceType.REPEAT_INTERVAL, EndType.DATETIME);
        this.repeatInterval = repeatInterval;
        this.endDate = endDate;
    }

    // run now and repeat on interval n times
    private JobTrigger(long repeatInterval, int repeatCount) {
        this(StartType.NOW, RecurrenceType.REPEAT_INTERVAL, EndType.REPEAT_COUNT);
        this.repeatInterval = repeatInterval;
        this.repeatCount = repeatCount;
    }

    // run at specified date/time, once
    private JobTrigger(Date startDate) {
        this(StartType.DATETIME, RecurrenceType.NONE, EndType.REPEAT_COUNT);
        this.startDate = startDate;
        this.repeatCount = 1;
    }

    // run at specified date/time and then repeat on interval indefinitely
    private JobTrigger(Date startDate, long repeatInterval) {
        this(StartType.DATETIME, RecurrenceType.REPEAT_INTERVAL, EndType.NEVER);
        this.startDate = startDate;
        this.repeatInterval = repeatInterval;
    }

    // run at specified date/time and then repeat on interval until end date/time
    private JobTrigger(Date startDate, long repeatInterval, Date endDate) {
        this(StartType.DATETIME, RecurrenceType.REPEAT_INTERVAL, EndType.DATETIME);
        this.startDate = startDate;
        this.repeatInterval = repeatInterval;
        this.endDate = endDate;
    }

    // run at specified date/time and then repeat on interval n times
    private JobTrigger(Date startDate, long repeatInterval, int repeatCount) {
        this(StartType.DATETIME, RecurrenceType.REPEAT_INTERVAL, EndType.REPEAT_COUNT);
        this.startDate = startDate;
        this.repeatInterval = repeatInterval;
        this.repeatCount = repeatCount;
    }

    // run on the schedule specified by cron expression
    private JobTrigger(String cronExpression) {
        this(StartType.NOW, RecurrenceType.CRON_EXPRESSION, EndType.NEVER);
        this.cronExpression = cronExpression;
    }

}
