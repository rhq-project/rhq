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
package org.rhq.coregui.client.components.form;

import static com.smartgwt.client.data.RelativeDate.END_OF_TODAY;
import static com.smartgwt.client.data.RelativeDate.START_OF_TODAY;
import static com.smartgwt.client.types.RelativeDateRangePosition.END;
import static com.smartgwt.client.types.RelativeDateRangePosition.START;

import java.util.Date;

import com.google.gwt.user.datepicker.client.CalendarUtil;
import com.smartgwt.client.util.DateUtil;
import com.smartgwt.client.widgets.form.fields.DateItem;



/**
 * DateItem used for filtering. This component adds the ability to returns
 * its date value a Integer offset of the current Date. For instance,
 * yesterday would return a -1. This ability to use Integer days instead of
 * Dates provides for a more robust solution when sending Dates across the wire
 * as Date.toString() in GWT creates a date format that has the local timezone
 * in it. GWT is not able to parse these dates with the local timezone in them.
 * GWT only knows how to parse RFC and GMT dates.
 *
 * @author Mike Thompson
 */
public class DateFilterItem extends DateItem {

    public final static String START_DATE_FILTER = "startTime";
    public final static String END_DATE_FILTER = "endTime";


    public DateFilterItem() {
        standardConfiguration();
    }

    public DateFilterItem(String name) {
        super(name);
        standardConfiguration();
    }

    public DateFilterItem(String name, String title) {
        super(name, title);
        standardConfiguration();
    }

    private void standardConfiguration() {
        this.setUseTextField(true);
        this.setEnforceDate(true);
        this.setEndDate(new Date());

    }

    /**
     * By passing around integers instead of Dates we can avoid the GWT
     * formatting issues around default format differences between prod and dev modes.
     *
     * @return Integer offset of current Date (i.e., yesterday is -1)
     */
    public Integer getValueAsDays() {
        if (getValueAsDate() == null) {
            return null;
        } else {
            return CalendarUtil.getDaysBetween(new Date(), getValueAsDate());
        }
    }

    @Override
    public Object getValue() {
        return getValueAsDays();
    }

    @Override
    public String toString() {
        return String.valueOf(getValueAsDays());
    }

    /**
     * For inclusive date ranges the start date time portion needs to be set to the beginning of the day.
     * @param startDate
     * @return same day but the time is adjusted to start of day
     */
    public static Date adjustTimeToStartOfDay(Date startDate) {
        return  DateUtil.getAbsoluteDate(START_OF_TODAY, startDate, START);
    }

    /**
     * For inclusive date ranges the end date time needs to be the end of the day.
     * @param endDate
     * @return same day but time portion adjusted to end of day
     */
    public static Date adjustTimeToEndOfDay(Date endDate) {
        return  DateUtil.getAbsoluteDate(END_OF_TODAY, endDate, END);

    }

}
