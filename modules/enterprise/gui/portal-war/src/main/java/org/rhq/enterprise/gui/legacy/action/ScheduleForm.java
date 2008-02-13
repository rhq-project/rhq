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
package org.rhq.enterprise.gui.legacy.action;

import java.util.Calendar;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.beans.OptionItem;

/**
 * A subclass of <code>BaseValidatorForm</code> that contains all of the properties for scheduling an action.
 */
public class ScheduleForm extends CalendarForm {
    //------------------------------------- instance variables

    /**
     * A flag to indicate whether or not this should be scheduled immediately. Valid values: "now" "onDate"
     *
     * @see web/common/components/Schedule.jsp
     */
    private String startTime;
    public static final String START_NOW = "now";
    public static final String START_ON_DATE = "onDate";

    private String endTime;
    public static final String END_NEVER = "none";
    public static final String END_ON_DATE = "onDate";

    private String recurInterval;
    public static final String RECUR_NEVER = "recurNever";
    public static final String RECUR_DAILY = "recurDaily";
    public static final String RECUR_WEEKLY = "recurWeekly";
    public static final String RECUR_MONTHLY = "recurMonthly";

    private String recurrenceFrequencyDaily;
    public static final String EVERY_DAY = "everyDay";
    public static final String EVERY_WEEKDAY = "everyWeekday";

    // all of these are used to set the interval - i.e. every 3 weeks
    private String numDays;
    private String numWeeks;
    private String numMonths;

    private java.lang.Integer[] recurrenceDay;

    private String recurrenceFrequencyMonthly;
    public static final String ON_EACH = "onEach";
    public static final String ON_DAY = "onDay";

    private Integer recurrenceWeek;
    private Integer monthlyRecurrenceDay;
    private Integer eachDay;

    private List<OptionItem> controlActions;
    private Integer numControlActions;

    //-------------------------------------public methods

    //    /**
    //     * This method constructs a schedule time value object
    //     * from the form.
    //     * It assumes that validate() has already been called.
    //     */
    //    public ScheduleValue createSchedule()
    //        throws IllegalArgumentException {
    //        // this assumes that validate has already been called
    //        Date start;
    //        Date end = null;
    //        int occur = 0;
    //
    //        // when to start
    //        if (startTime.equals(START_NOW)) {
    //            start = new Date();
    //        } else {
    //            start = getStartDate();
    //        }
    //
    //        // when to end
    //        if (endTime.equals(END_ON_DATE)) {
    //            this.setWantEndDate(true);
    //            end = getEndDate();
    //        } else {
    //            end = null;
    //        }
    //
    //                // never end
    //        if (recurInterval.equals(RECUR_NEVER)) {
    //            return new ScheduleSingleValue(start);
    //        }
    //        else if (recurInterval.equals(RECUR_DAILY)) {
    //            if (recurrenceFrequencyDaily.equals(EVERY_DAY)) {
    //                return new ScheduleDailyValue(start, end, Integer.parseInt(numDays));
    //            } else if (recurrenceFrequencyDaily.equals(EVERY_WEEKDAY)) {
    //                ScheduleDailyValue sdv = new ScheduleDailyValue(start, end, 0);
    //                sdv.setEveryWeekDay(true /* only on weekdays */);
    //                return sdv;
    //            } else {
    //                // shouldnt get here.
    //                return new ScheduleDailyValue(start, end, 0);
    //            }
    //        }
    //        else if (recurInterval.equals(RECUR_WEEKLY)) {
    //            int intNumWeeks = Integer.parseInt(numWeeks);
    //            ScheduleWeeklyValue swv = new ScheduleWeeklyValue(start, end, intNumWeeks);
    //            for (int i = 0; i < recurrenceDay.length; i++) {
    //                swv.setDay(recurrenceDay[i].intValue());
    //            }
    //            return swv;
    //        }
    //        else if (recurInterval.equals(RECUR_MONTHLY)){
    //            int intNumMonths = Integer.parseInt(numMonths);
    //            ScheduleMonthlyValue smv
    //                = new ScheduleMonthlyValue(start, end, intNumMonths);
    //
    //            // which day of the month to run this on
    //            if (ON_EACH.equals(recurrenceFrequencyMonthly)) {
    //                // "1st of November"
    //                smv.setDay(eachDay.intValue());
    //            } else if (ON_DAY.equals(recurrenceFrequencyMonthly)) {
    //                // "3rd Sunday"
    //                smv.setWeekOfMonth(recurrenceWeek.intValue());
    //                smv.setDayOfWeek(monthlyRecurrenceDay.intValue());
    //            } else {
    //                // shouldnt get here
    //                smv.setDay(1);
    //            }
    //            return smv;
    //        } else {
    //          throw new IllegalArgumentException("recurInterval had invalid value");
    //        }
    //    }
    //
    //    /**
    //     * This method populates the form from a schedule time value object.
    //     *
    //     * @param sv The ScheduleValue object to populate the form from.
    //     */
    //    public void populateFromSchedule(ScheduleValue sv, Locale userLocale)
    //        throws IllegalArgumentException {
    //
    //        this.populateStartDate(sv.getStart(), userLocale);
    //        Date end = sv.getEnd();
    //        int ival = sv.getInterval();
    //        if (null != end && ival > 0) {
    //            this.setWantEndDate(true); // performs validation on end date
    //            this.populateEndDate(sv.getEnd(), userLocale);
    //            endTime = END_ON_DATE;
    //        } else {
    //            endTime = END_NEVER;
    //        }
    //
    //        // won't ever be editing an "immediate"
    //        this.startTime = START_ON_DATE;
    //
    //        if (sv instanceof ScheduleSingleValue) {
    //            ScheduleSingleValue ssv = (ScheduleSingleValue)sv;
    //            recurInterval = RECUR_NEVER;
    //            return;
    //        }
    //        else if (sv instanceof ScheduleDailyValue) {
    //            ScheduleDailyValue sdv = (ScheduleDailyValue)sv;
    //            this.recurInterval = RECUR_DAILY;
    //            this.numDays = new Integer(sdv.getInterval()).toString();
    //            if (sdv.getEveryWeekDay()) {
    //                recurrenceFrequencyDaily = EVERY_WEEKDAY;
    //            } else {
    //                recurrenceFrequencyDaily = EVERY_DAY;
    //            }
    //            return;
    //        }
    //        else if (sv instanceof ScheduleWeeklyValue) {
    //            ScheduleWeeklyValue swv = (ScheduleWeeklyValue)sv;
    //            this.recurInterval = RECUR_WEEKLY;
    //            this.numWeeks = new Integer(swv.getInterval()).toString();
    //            ArrayList tmpDays = new ArrayList();
    //            for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
    //                if (swv.isDaySet(i)) {
    //                    tmpDays.add(new Integer(i));
    //                }
    //            }
    //            recurrenceDay = new Integer[tmpDays.size()];
    //            recurrenceDay = (Integer[])tmpDays.toArray(recurrenceDay);
    //            return;
    //        }
    //        else if (sv instanceof ScheduleMonthlyValue) {
    //            ScheduleMonthlyValue smv = (ScheduleMonthlyValue)sv;
    //            this.recurInterval = RECUR_MONTHLY;
    //            this.numMonths = new Integer(smv.getInterval()).toString();
    //
    //            if (smv.isOffset()) {
    //                // "3rd Sunday"
    //                this.recurrenceWeek = new Integer(smv.getWeekOfMonth());
    //                this.monthlyRecurrenceDay = new Integer(smv.getDayOfWeek());
    //                this.recurrenceFrequencyMonthly = ON_DAY;
    //            } else {
    //                // "31st of October"
    //                this.recurrenceFrequencyMonthly = ON_EACH;
    //                this.eachDay = new Integer(smv.getDay());
    //            }
    //            return;
    //        }
    //    }

    public boolean getIsNow() {
        return startTime.equals(START_NOW);
    }

    private String fmtTime(int s) {
        if (s < 10) {
            return "0" + s;
        } else {
            return new Integer(s).toString();
        }
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.startTime = START_NOW;
        Calendar cal = Calendar.getInstance();
        this.recurInterval = "";
        this.recurrenceFrequencyDaily = EVERY_DAY;
        this.recurrenceFrequencyMonthly = ON_EACH;
        this.numMonths = "1";
        this.setEndTime(END_NEVER);
        this.eachDay = new Integer(1);
        this.numDays = "1";
        this.numWeeks = "1";
        this.recurrenceDay = new Integer[0]; /* for everyWeekday enumeration 1-7 */
        this.monthlyRecurrenceDay = null;
        this.controlActions = null;
        this.numControlActions = new Integer(0);

        super.reset(mapping, request);
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if (!shouldValidate(mapping, request)) {
            // A special case, BaseValidatorForm will return
            // null if Ok is clicked and input is null, indicating
            // a PrepareForm action is occurring. This is tricky.

            return null;
        }

        ActionErrors errs = super.validate(mapping, request);
        if (errs == null) {
            errs = new ActionErrors();
        }

        if ((startTime == null) || startTime.equals(START_NOW)) {
            return errs.isEmpty() ? null : errs;
        }

        if (null != recurInterval) {
            // now check whichever is appropriate: numDays numWeeks numMonths, the
            // interval (every X days/weeks/months) that the user enters.
            if (recurInterval.equals(RECUR_DAILY)) {
                if ((recurrenceFrequencyDaily != null) && recurrenceFrequencyDaily.equals(EVERY_DAY)) {
                    Integer tmpNumDays = null;
                    try {
                        tmpNumDays = new Integer(Integer.parseInt(numDays));
                    } catch (NumberFormatException nfe) {
                        errs.add("numDays",
                            new ActionError("resource.autodiscovery.ScheduleTab.error.numDays", numDays));
                    }
                }
            } else if (recurInterval.equals(RECUR_WEEKLY)) {
                Integer tmpNumWeeks = null;
                try {
                    tmpNumWeeks = new Integer(Integer.parseInt(numWeeks));
                } catch (NumberFormatException nfe) {
                    errs
                        .add("numWeeks", new ActionError("resource.autodiscovery.ScheduleTab.error.numWeeks", numWeeks));
                }

                // check that the user clicked at least one day of the week to occurr on
                if (recurrenceDay.length == 0) {
                    errs
                        .add("recurrenceDay", new ActionError("resource.autodiscovery.ScheduleTab.error.recurrenceDay"));
                }
            } else if (recurInterval.equals(RECUR_MONTHLY)) {
                Integer tmpNumMonths = null;
                try {
                    tmpNumMonths = new Integer(Integer.parseInt(numMonths));
                } catch (NumberFormatException nfe) {
                    errs.add("numMonths", new ActionError("resource.autodiscovery.ScheduleTab.error.numMonths",
                        numMonths));
                }
            }
        } /* if (null != recurInterval) { */

        return errs.isEmpty() ? null : errs;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(" startTime=").append(startTime);
        buf.append(" recurInterval=").append(recurInterval);
        buf.append(" recurrenceFrequencyDaily=").append(recurrenceFrequencyDaily);
        buf.append(" numDays=").append(numDays);
        buf.append(" numWeeks=").append(numWeeks);
        buf.append(" recurrenceDay=").append(recurrenceDay);
        buf.append(" recurrenceFrequencyMonthly=").append(recurrenceFrequencyMonthly);
        buf.append(" numMonths=").append(numMonths);
        buf.append(" recurrenceWeek=").append(recurrenceWeek);
        buf.append(" monthlyRecurrenceDay=").append(monthlyRecurrenceDay);
        buf.append(" eachDay=").append(eachDay);
        buf.append(" endTime=").append(endTime);

        return buf.toString();
    }

    /**
     * Getter for property eachDay.
     *
     * @return Value of property eachDay.
     */
    public java.lang.Integer getEachDay() {
        return eachDay;
    }

    /**
     * Setter for property eachDay.
     *
     * @param eachDay New value of property eachDay.
     */
    public void setEachDay(java.lang.Integer eachDay) {
        this.eachDay = eachDay;
    }

    /**
     * Getter for property endTime.
     *
     * @return Value of property endTime.
     */
    public java.lang.String getEndTime() {
        return endTime;
    }

    /**
     * Setter for property endTime. Also sets wantEndDate to true if endTime equals END_ON_DATE
     *
     * @param endTime New value of property endTime.
     */
    public void setEndTime(java.lang.String endTime) {
        this.endTime = endTime;
        setWantEndDate(endTime.equals(END_ON_DATE) ? true : false);
    }

    /**
     * Getter for property numDays.
     *
     * @return Value of property numDays.
     */
    public java.lang.String getNumDays() {
        return numDays;
    }

    /**
     * Setter for property numDays.
     *
     * @param numDays New value of property numDays.
     */
    public void setNumDays(java.lang.String numDays) {
        this.numDays = numDays;
    }

    /**
     * Getter for property numMonths.
     *
     * @return Value of property numMonths.
     */
    public java.lang.String getNumMonths() {
        return numMonths;
    }

    /**
     * Setter for property numMonths.
     *
     * @param numMonths New value of property numMonths.
     */
    public void setNumMonths(java.lang.String numMonths) {
        this.numMonths = numMonths;
    }

    /**
     * Getter for property numWeeks.
     *
     * @return Value of property numWeeks.
     */
    public java.lang.String getNumWeeks() {
        return numWeeks;
    }

    /**
     * Setter for property numWeeks.
     *
     * @param numWeeks New value of property numWeeks.
     */
    public void setNumWeeks(java.lang.String numWeeks) {
        this.numWeeks = numWeeks;
    }

    /**
     * Getter for property recurrenceDay.
     *
     * @return Value of property recurrenceDay.
     */
    public java.lang.Integer[] getRecurrenceDay() {
        return recurrenceDay;
    }

    /**
     * Setter for property recurrenceDay.
     *
     * @param recurrenceDay New value of property recurrenceDay.
     */
    public void setRecurrenceDay(java.lang.Integer[] recurrenceDay) {
        this.recurrenceDay = recurrenceDay;
    }

    /**
     * Getter for property recurrenceFrequencyDaily.
     *
     * @return Value of property recurrenceFrequencyDaily.
     */
    public java.lang.String getRecurrenceFrequencyDaily() {
        return recurrenceFrequencyDaily;
    }

    /**
     * Setter for property recurrenceFrequencyDaily.
     *
     * @param recurrenceFrequencyDaily New value of property recurrenceFrequencyDaily.
     */
    public void setRecurrenceFrequencyDaily(java.lang.String recurrenceFrequencyDaily) {
        this.recurrenceFrequencyDaily = recurrenceFrequencyDaily;
    }

    /**
     * Getter for property recurrenceFrequencyMonthly.
     *
     * @return Value of property recurrenceFrequencyMonthly.
     */
    public java.lang.String getRecurrenceFrequencyMonthly() {
        return recurrenceFrequencyMonthly;
    }

    /**
     * Setter for property recurrenceFrequencyMonthly.
     *
     * @param recurrenceFrequencyMonthly New value of property recurrenceFrequencyMonthly.
     */
    public void setRecurrenceFrequencyMonthly(java.lang.String recurrenceFrequencyMonthly) {
        this.recurrenceFrequencyMonthly = recurrenceFrequencyMonthly;
    }

    /**
     * Getter for property recurrenceWeek.
     *
     * @return Value of property recurrenceWeek.
     */
    public java.lang.Integer getRecurrenceWeek() {
        return recurrenceWeek;
    }

    /**
     * Setter for property recurrenceWeek.
     *
     * @param recurrenceWeek New value of property recurrenceWeek.
     */
    public void setRecurrenceWeek(java.lang.Integer recurrenceWeek) {
        this.recurrenceWeek = recurrenceWeek;
    }

    /**
     * Getter for property recurrenceWeek.
     *
     * @return Value of property recurrenceWeek.
     */
    public java.lang.Integer getMonthlyRecurrenceDay() {
        return monthlyRecurrenceDay;
    }

    /**
     * Setter for property monthlyRecurrenceDay.
     *
     * @param monthlyRecurrenceDay New value of property monthlyRecurrenceDay.
     */
    public void setMonthlyRecurrenceDay(java.lang.Integer monthlyRecurrenceDay) {
        this.monthlyRecurrenceDay = monthlyRecurrenceDay;
    }

    /**
     * Getter for property recurInterval.
     *
     * @return Value of property recurInterval.
     */
    public java.lang.String getRecurInterval() {
        return recurInterval;
    }

    /**
     * Setter for property recurInterval.
     *
     * @param recurInterval New value of property recurInterval.
     */
    public void setRecurInterval(java.lang.String recurInterval) {
        this.recurInterval = recurInterval;
    }

    /**
     * Getter for property startTime.
     *
     * @return Value of property startTime.
     */
    public java.lang.String getStartTime() {
        return startTime;
    }

    /**
     * Setter for property startTime.
     *
     * @param now New value of property startTime.
     */
    public void setStartTime(java.lang.String st) {
        this.startTime = st;
    }

    public List<OptionItem> getControlActions() {
        return controlActions;
    }

    public void setControlActions(List<OptionItem> controlActions) {
        this.controlActions = controlActions;
    }

    public Integer getNumControlActions() {
        return (this.controlActions == null) ? 0 : this.controlActions.size();
    }
}