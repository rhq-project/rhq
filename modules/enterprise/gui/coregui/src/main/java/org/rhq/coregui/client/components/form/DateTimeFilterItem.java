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

import java.util.Date;

import com.smartgwt.client.widgets.form.fields.DateTimeItem;

/**
 * DateTimeItem used for filtering. This component adds the ability to returns
 * its date and time value a Integer offset of the current Date. For instance,
 * yesterday would return a -1. This ability to use Integer days instead of
 * Dates provides for a more robust solution when sending Dates across the wire
 * as Date.toString() in GWT creates a date format that has the local timezone
 * in it. GWT is not able to parse these dates with the local timezone in them.
 * GWT only knows how to parse RFC and GMT dates.
 *
 * @author Mike Thompson
 */
public class DateTimeFilterItem extends DateTimeItem {

    public final static String START_DATE_FILTER = "startTime";
    public final static String END_DATE_FILTER = "endTime";


    public DateTimeFilterItem() {
        standardConfiguration();
    }

    public DateTimeFilterItem(String name) {
        super(name);
        standardConfiguration();
    }

    public DateTimeFilterItem(String name, String title) {
        super(name, title);
        standardConfiguration();
    }

    private void standardConfiguration() {
        this.setUseTextField(true);
        this.setEnforceDate(true);
        this.setEndDate(new Date());

    }


    @Override
    public Object getValue() {
        Date date = getValueAsDate();
        if (date!=null)
            return date.getTime();
        return null;
    }

    @Override
    public String toString() {
        return String.valueOf(getValue());
    }

}
