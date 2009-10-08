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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * A subclass of <code>BaseValidatorForm</code> that contains all of the properties for a start and end date.
 */
public class CalendarForm extends BaseValidatorForm {
    //------------------------------------- instance variables

    private Integer startMonth;
    private Integer startDay;
    private Integer startYear;
    private String startHour;
    private String startMin;
    private String startAmPm;

    private boolean wantEndDate;

    private Integer endMonth;
    private Integer endDay;
    private Integer endYear;
    private String endHour;
    private String endMin;
    private String endAmPm;

    public static final String AM = "am";
    public static final String PM = "pm";

    //-------------------------------------public methods

    /**
     * Utility method that converts the fields associated with the the end time into a date.
     *
     * @return A java.util.Date that represents the end date.
     */
    public Date getEndDate() {
        if (!getWantEndDate()) {
            return null;
        }

        GregorianCalendar cal = new GregorianCalendar();

        cal.set(Calendar.YEAR, endYear.intValue());
        cal.set(Calendar.MONTH, endMonth.intValue());
        cal.set(Calendar.DAY_OF_MONTH, endDay.intValue());

        try {
            cal.set(Calendar.HOUR_OF_DAY, calcHour(endHour, endAmPm));
            cal.set(Calendar.MINUTE, Integer.parseInt(endMin));
            cal.set(Calendar.SECOND, 0);
        } catch (NumberFormatException nfe) {
            return null;
        }

        return cal.getTime();
    }

    /**
     * Populates the form's end date fields with a date.
     *
     * @param date A java.util.Date that represents the end date.
     */
    public void populateEndDate(Date d, Locale userLocale) {
        GregorianCalendar cal = new GregorianCalendar(userLocale);
        cal.setTime(d);

        endYear = Integer.valueOf(cal.get(Calendar.YEAR));
        endMonth = Integer.valueOf(cal.get(Calendar.MONTH));
        endDay = Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        endMin = fmtTime(cal.get(Calendar.MINUTE));

        int tmpEndHour = cal.get(Calendar.HOUR);
        if (cal.get(Calendar.HOUR) == 0) {
            tmpEndHour = 12;
        }

        if (cal.get(Calendar.AM_PM) == Calendar.PM) {
            endAmPm = PM;
        } else {
            endAmPm = AM;
        }

        endHour = fmtTime(tmpEndHour);
    }

    /**
     * Utility method that converts the fields associated with the the start time into a date.
     *
     * @return A java.util.Date that represents the start date.
     */
    public Date getStartDate() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, startYear.intValue());
        cal.set(Calendar.MONTH, startMonth.intValue());
        cal.set(Calendar.DAY_OF_MONTH, startDay.intValue());

        try {
            cal.set(Calendar.HOUR_OF_DAY, calcHour(startHour, startAmPm));
            cal.set(Calendar.MINUTE, Integer.parseInt(startMin));
            cal.set(Calendar.SECOND, 0);
        } catch (NumberFormatException nfe) {
            return null;
        }

        return cal.getTime();
    }

    /* If the hour is 0, or [13-23] inclusive, the user is trying
     * to use military time. In this case, ignore AM_PM setting.
     *
     * This only works if you are setting HOUR_OF_DAY in the calendar object.
     *
     * @see http://greenwichmeantime.com/info/noon.htm
     */
    protected int calcHour(String hour, String ampm) throws NumberFormatException {
        int tmpStart = Integer.parseInt(hour);
        if ((tmpStart == 0) || ((tmpStart > 12) && (tmpStart <= 23))) {
            return tmpStart;
        } else if (PM.equals(ampm)) {
            if (tmpStart == 12) {
                return tmpStart;
            }

            return tmpStart + 12;
        } else {
            if (tmpStart == 12) {
                tmpStart -= 12;
            }

            return tmpStart;
        }
    }

    /**
     * Utility method that populates fields associated with the the start date.
     *
     * @param d A java.util.Date that represents the start date.
     */
    public void populateStartDate(Date startDate, Locale userLocale) {
        GregorianCalendar cal = new GregorianCalendar(userLocale);
        cal.setTime(startDate);

        startYear = Integer.valueOf(cal.get(Calendar.YEAR));
        startMonth = Integer.valueOf(cal.get(Calendar.MONTH));
        startDay = Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        startMin = this.fmtTime(cal.get(Calendar.MINUTE));

        int tmpStartHour = cal.get(Calendar.HOUR);
        if (cal.get(Calendar.HOUR) == 0) {
            tmpStartHour = 12;
        }

        if (cal.get(Calendar.AM_PM) == Calendar.PM) {
            startAmPm = PM;
        } else {
            startAmPm = AM;
        }

        startHour = fmtTime(tmpStartHour);
    }

    private String fmtTime(int s) {
        if (s < 10) {
            return "0" + s;
        } else {
            return String.valueOf(s);
        }
    }

    /**
     * @return an initial current time
     */
    protected Calendar getInitStartTime() {
        Calendar cal = Calendar.getInstance();
        return cal;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        Calendar cal = getInitStartTime();

        this.startMin = fmtTime(cal.get(Calendar.MINUTE));
        int hr = cal.get(Calendar.HOUR);

        // assume that if the hr is 0, it is 12 o'clock am/pm.
        if (hr == 0) {
            hr = 12;
        }

        this.startHour = fmtTime(hr);
        if (cal.get(Calendar.AM_PM) == Calendar.AM) {
            this.startAmPm = AM;
        } else {
            this.startAmPm = PM;
        }

        this.startDay = Integer.valueOf(cal.get(Calendar.DAY_OF_WEEK));
        this.startMonth = Integer.valueOf(cal.get(Calendar.MONTH));
        this.startYear = Integer.valueOf(Calendar.YEAR);

        this.wantEndDate = false;

        this.endHour = this.startHour;
        this.endMin = this.startMin;
        this.endMonth = Integer.valueOf(Calendar.JANUARY);
        this.endDay = Integer.valueOf(Calendar.SUNDAY);
        this.endYear = Integer.valueOf(cal.get(Calendar.YEAR));

        super.reset(mapping, request);
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if (!shouldValidate(mapping, request)) {
            return null;
        }

        ActionErrors errs = super.validate(mapping, request);
        if (errs == null) {
            errs = new ActionErrors();
        }

        if (shouldValidateDateRange()) {
            validateDateRange(mapping, request, errs);
        }

        return errs.isEmpty() ? null : errs;
    }

    protected boolean shouldValidateDateRange() {
        return true;
    }

    protected final void validateDateRange(ActionMapping mapping, HttpServletRequest request, ActionErrors errs) {
        // custom validation

        // Check start time
        try {
            int tmph = Integer.parseInt(startHour);
            if ((tmph < 0) || (tmph > 12)) {
                errs.add("startHour", new ActionMessage("errors.range", startHour, Integer.valueOf(1), Integer
                    .valueOf(12)));
            }
        } catch (NumberFormatException nfe) {
            errs.add("startHour", new ActionMessage("errors.invalid.StartHour", startHour));
        }

        try {
            int tmpmin = Integer.parseInt(startMin);
            if ((tmpmin > 59) || (tmpmin < 0)) {
                errs.add("startMin", new ActionMessage("errors.range", startMin, Integer.valueOf(0), Integer
                    .valueOf(59)));
            }
        } catch (NumberFormatException nfe) {
            errs.add("startMin", new ActionMessage("errors.invalid.StartMin", startMin));
        }

        // Check end time
        try {
            int tmph = Integer.parseInt(endHour);
            if ((tmph < 0) || (tmph > 12)) {
                errs
                    .add("endHour", new ActionMessage("errors.range", endHour, Integer.valueOf(1), Integer.valueOf(12)));
            }
        } catch (NumberFormatException nfe) {
            errs.add("endHour", new ActionMessage("errors.invalid.EndHour", endHour));
        }

        try {
            int tmpmin = Integer.parseInt(endMin);
            if ((tmpmin > 59) || (tmpmin < 0)) {
                errs.add("endMin", new ActionMessage("errors.range", endMin, 0, 59));
            }
        } catch (NumberFormatException nfe) {
            errs.add("endMin", new ActionMessage("errors.invalid.EndMin", endMin));
        }

        Date tmpStartDate = getStartDate();
        Date tmpEndDate = getEndDate();

        //System.out.println("start: "  + tmpStartDate + " end: " + tmpEndDate);
        if ((tmpStartDate != null) && (tmpEndDate != null) && tmpStartDate.after(tmpEndDate)) {
            errs.add("endDate", new ActionMessage("resource.common.monitor.error.FromEarlierThanTo"));
        }
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(" startMonth=").append(startMonth);
        buf.append(" startDay=").append(startDay);
        buf.append(" startYear=").append(startYear);
        buf.append(" startHour=").append(startHour);
        buf.append(" startMin=").append(startMin);
        buf.append(" startAmPm=").append(startAmPm);
        buf.append(" endMonth=").append(endMonth);
        buf.append(" endDay=").append(endDay);
        buf.append(" endYear=").append(endYear);
        buf.append(" endHour=").append(endHour);
        buf.append(" endMin=").append(endMin);
        buf.append(" endAmPm=").append(endAmPm);

        return buf.toString();
    }

    /**
     * A flag indicating whether or not end date should be checked.
     */
    public boolean getWantEndDate() {
        return wantEndDate;
    }

    public void setWantEndDate(boolean b) {
        this.wantEndDate = b;
    }

    /**
     * Getter for property endDay.
     *
     * @return Value of property endDay.
     */
    public Integer getEndDay() {
        return endDay;
    }

    /**
     * Setter for property endDay.
     *
     * @param endDay New value of property endDay.
     */
    public void setEndDay(Integer endDay) {
        this.endDay = endDay;
    }

    /**
     * Getter for property endMonth.
     *
     * @return Value of property endMonth.
     */
    public Integer getEndMonth() {
        return endMonth;
    }

    /**
     * Setter for property endMonth.
     *
     * @param endMonth New value of property endMonth.
     */
    public void setEndMonth(Integer endMonth) {
        this.endMonth = endMonth;
    }

    /**
     * Getter for property endYear.
     *
     * @return Value of property endYear.
     */
    public Integer getEndYear() {
        return endYear;
    }

    /**
     * Setter for property endYear.
     *
     * @param endYear New value of property endYear.
     */
    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    /**
     * Getter for property endMin.
     *
     * @return Value of property endMin.
     */
    public String getEndMin() {
        return endMin;
    }

    /**
     * Setter for property endMin.
     *
     * @param endMin New value of property endMin.
     */
    public void setEndMin(String endMin) {
        this.endMin = endMin;
    }

    /**
     * Getter for property endHour.
     *
     * @return Value of property endHour.
     */
    public String getEndHour() {
        return endHour;
    }

    /**
     * Setter for property endHour.
     *
     * @param endHour New value of property endHour.
     */
    public void setEndHour(String endHour) {
        this.endHour = endHour;
    }

    /**
     * Getter for property startDay.
     *
     * @return Value of property startDay.
     */
    public Integer getStartDay() {
        return startDay;
    }

    /**
     * Setter for property startDay.
     *
     * @param startDay New value of property startDay.
     */
    public void setStartDay(Integer startDay) {
        this.startDay = startDay;
    }

    /**
     * Getter for property startAmPm.
     *
     * @return Value of property startAmPm.
     */
    public String getStartAmPm() {
        return startAmPm;
    }

    /**
     * Setter for property startAmPm.
     *
     * @param startAmPm New value of property startAmPm.
     */
    public void setStartAmPm(String startAmPm) {
        this.startAmPm = startAmPm;
    }

    /**
     * Getter for property endAmPm.
     *
     * @return Value of property endAmPm.
     */
    public String getEndAmPm() {
        return endAmPm;
    }

    /**
     * Setter for property endAmPm.
     *
     * @param endAmPm New value of property endAmPm.
     */
    public void setEndAmPm(String endAmPm) {
        this.endAmPm = endAmPm;
    }

    /**
     * Getter for property startYear.
     *
     * @return Value of property startYear.
     */
    public Integer getStartYear() {
        return startYear;
    }

    /**
     * Setter for property startYear.
     *
     * @param startYear New value of property startYear.
     */
    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    /**
     * Getter for property startMin.
     *
     * @return Value of property startMin.
     */
    public String getStartMin() {
        return startMin;
    }

    /**
     * Setter for property startMin.
     *
     * @param startMin New value of property startMin.
     */
    public void setStartMin(String startMin) {
        this.startMin = startMin;
    }

    /**
     * Getter for property startHour.
     *
     * @return Value of property startHour.
     */
    public String getStartHour() {
        return startHour;
    }

    /**
     * Setter for property startHour.
     *
     * @param startHour New value of property startHour.
     */
    public void setStartHour(String startHour) {
        this.startHour = startHour;
    }

    /**
     * Getter for property startMonth.
     *
     * @return Value of property startMonth.
     */
    public Integer getStartMonth() {
        return startMonth;
    }

    /**
     * Setter for property startMonth.
     *
     * @param startMonth New value of property startMonth.
     */
    public void setStartMonth(Integer startMonth) {
        this.startMonth = startMonth;
    }

    public Collection<Integer> getYearOptions() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());

        int year = cal.get(Calendar.YEAR);
        List<Integer> ret = new ArrayList<Integer>(11);
        for (int i = 3; i > 0; i--) {
            ret.add(year + i);
        }

        for (int i = 0; i < 8; i++) {
            ret.add(year - i);
        }

        return ret;
    }
}