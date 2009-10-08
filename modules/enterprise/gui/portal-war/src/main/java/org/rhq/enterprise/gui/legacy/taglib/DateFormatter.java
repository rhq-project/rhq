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
package org.rhq.enterprise.gui.legacy.taglib;

import java.io.IOException;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import org.rhq.core.clientapi.util.units.DateFormatter.DateSpecifics;
import org.rhq.core.clientapi.util.units.FormattedNumber;
import org.rhq.core.clientapi.util.units.ScaleConstants;
import org.rhq.core.clientapi.util.units.UnitNumber;
import org.rhq.core.clientapi.util.units.UnitsConstants;
import org.rhq.core.clientapi.util.units.UnitsFormat;
import org.rhq.enterprise.gui.legacy.StringConstants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * Tag that will take a value that is a long, or a runtime expression, and output a date representation of that long.
 */
public class DateFormatter extends VarSetterBaseTag {
    /**
     * A string which contains the long, or the expression, we hope to convert into a Long, and format as a date.
     */
    private Long value = null;

    /**
     * Holds value of property time.
     */
    private Boolean time = Boolean.FALSE;

    /**
     * Holds value of property showTime.
     */
    private Boolean showTime = Boolean.TRUE;

    public DateFormatter() {
        super();
    }

    public Long getValue() {
        return this.value;
    }

    public void setValue(Long v) {
        this.value = v;
    }

    /**
     * Utility method for formatting dates. XXX Might want to pass in a dateFmt'r if need more than 1 format. Right now,
     * using simple "time" flag in tag to decide if should format as a time.
     *
     * @param date   The long to convert to a date.
     * @param asTime Whether to format this date as a time.
     */
    private String formatDate(Long date) {
        UnitsConstants unit = time.booleanValue() ? UnitsConstants.UNIT_DURATION : UnitsConstants.UNIT_DATE;
        String key = StringConstants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis";

        if (!showTime.booleanValue()) {
            key += ".dateonly";
        }

        String formatString = RequestUtils.message((HttpServletRequest) pageContext.getRequest(), key);
        DateSpecifics specs = new DateSpecifics();
        specs.setDateFormat(new SimpleDateFormat(formatString));
        FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(date.doubleValue(), unit, ScaleConstants.SCALE_MILLI),
            pageContext.getRequest().getLocale(), specs);
        return fmtd.toString();
    }

    /**
     * This evaluates <em>value</em> as a struts expression, then outputs the resulting string to the <em>
     * pageContext</em>'s out.
     */
    public int doStartTag() throws JspException {
        Long newDate;

        newDate = (value == null) ? System.currentTimeMillis() : value;

        String d = formatDate(newDate);

        if (getVar() != null) {
            setScopedVariable(d);
        } else {
            try {
                pageContext.getOut().write(d);
            } catch (IOException ioe) {
                throw new JspException(getClass().getName() + " Could not output date.");
            }
        }

        return SKIP_BODY;
    }

    /**
     * Reset the values of the tag.
     */
    public void release() {
        value = null;
    }

    /**
     * Getter for property time.
     *
     * @return Value of property time.
     */
    public Boolean getTime() {
        return this.time;
    }

    /**
     * Setter for property time.
     *
     * @param time New value of property time.
     */
    public void setTime(Boolean time) {
        this.time = time;
    }

    /**
     * Getter for property showTime.
     *
     * @return Value of property showTime.
     */
    public Boolean getShowTime() {
        return this.showTime;
    }

    /**
     * Setter for property showTime.
     *
     * @param time New value of property showTime.
     */
    public void setShowTime(Boolean showTime) {
        this.showTime = showTime;
    }
}