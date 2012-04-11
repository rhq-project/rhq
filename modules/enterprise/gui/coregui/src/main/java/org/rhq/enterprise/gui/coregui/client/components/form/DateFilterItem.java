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
package org.rhq.enterprise.gui.coregui.client.components.form;

import com.google.gwt.user.datepicker.client.CalendarUtil;
import com.smartgwt.client.widgets.form.fields.DateItem;

import java.util.Date;

/**
 * DateItem used for filtering. This component adds the ability to returns
 * its date value a Integer offset of the current Date. For instance,
 * yesterday would return a -1. This ability to use Integer days instead of
 * Dates provides for a more robust solution when sending Dates across the wire
 * as Date.toString() in GWT creates a date format that has the local timezone
 * in it. GWT is not able to parse these dates with the local timezone in them.
 * GWT only knows how to parse RFC and GMT dates.
 * @author Mike Thompson
 */
public class DateFilterItem extends DateItem {

    public final static String START_DATE_FILTER = "startDateFilter";
    public final static String END_DATE_FILTER = "endDateFilter";


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

    private void standardConfiguration(){
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
            return 0;
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


}
