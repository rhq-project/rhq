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
package org.rhq.enterprise.gui.common.scheduling;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.sun.faces.util.MessageUtils;

import org.richfaces.component.html.HtmlCalendar;
import org.richfaces.component.html.HtmlInputNumberSpinner;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.scheduling.supporting.TimeUnits;

public class HtmlSimpleTriggerRendererClassicModel implements HtmlSimpleTriggerRendererModel {

    private FacesContext currentContext;
    private final int TABLE_BORDER = 0;
    private final String richCalendarSuffix = "InputDate";

    public boolean isAvailable() {
        // available if "start" is present in the request, and it doesn't equal the "fake" default value
        String start = FacesContextUtility.getOptionalRequestParameter("start", "fake");
        return !start.equals("fake");
    }

    public boolean getDeferred() {
        String start = FacesContextUtility.getRequiredRequestParameter("start");
        return start.equals("startDate");
    }

    public Date getStartDateTime(DateFormat dateFormatter) {
        String startDateTimeStr = FacesContextUtility.getRequiredRequestParameter("startDateTime" + richCalendarSuffix);
        try {
            return dateFormatter.parse(startDateTimeStr);
        } catch (ParseException pe) {
            throw new RuntimeException(MessageUtils.getExceptionMessageString(MessageUtils.CONVERSION_ERROR_MESSAGE_ID,
                "component"));
        }
    }

    public boolean getRepeat() {
        String recur = FacesContextUtility.getRequiredRequestParameter("recur");
        return recur.equals("every");
    }

    public int getRepeatInterval() {
        return FacesContextUtility.getRequiredRequestParameter("repeatInterval", Integer.class);
    }

    public TimeUnits getRepeatUnits() {
        return FacesContextUtility.getRequiredRequestParameter("repeatUnits", TimeUnits.class);
    }

    public boolean getTerminate() {
        String end = FacesContextUtility.getOptionalRequestParameter("end");
        return end.equals("endDate");
    }

    public Date getEndDateTime(DateFormat dateFormatter) {
        String endDateTimeStr = FacesContextUtility.getRequiredRequestParameter("endDateTime" + richCalendarSuffix);
        try {
            return dateFormatter.parse(endDateTimeStr);
        } catch (ParseException pe) {
            throw new RuntimeException(MessageUtils.getExceptionMessageString(MessageUtils.CONVERSION_ERROR_MESSAGE_ID,
                "component"));
        }
    }

    public void encode(FacesContext context, HtmlSimpleTrigger trigger) throws IOException {
        currentContext = context;
        ResponseWriter writer = context.getResponseWriter();

        writeStartDateRows(writer, trigger);

        writeRepeatRows(writer, trigger);
        if ((trigger.getReadOnly() && !trigger.getRepeat()) || !trigger.getReadOnly()) {
            writer.write("<script type=\"text/javascript\">hidediv('repeatRows');</script>");
        }

        writeEndDateRows(writer, trigger);
        if ((trigger.getReadOnly() && !trigger.getTerminate()) || !trigger.getReadOnly()) {
            writer.write("<script type=\"text/javascript\">hidediv('endRows');</script>");
        }
    }

    protected void writeStartDateRows(ResponseWriter writer, HtmlSimpleTrigger trigger) throws IOException {
        // "Start:" (start) "Immediately" ""
        // ""       (start) <DateTime>    {format}

        writer.startElement("table", trigger);
        writer.writeAttribute("style", getTablePaddingStyle(), null);
        writer.writeAttribute("border", TABLE_BORDER, null);

        writer.startElement("tr", trigger);

        printLabel(writer, trigger, "Start:", getLeftLabelStyle());
        printRadio(writer, trigger, !trigger.getDeferred(), "start", "immediate",
            "hidediv('repeatRows');clickRadio('recur','never');");
        printLabel(writer, trigger, "Immediately");
        //printLabel(writer, trigger, "");

        writer.endElement("tr");

        writer.startElement("tr", trigger);

        printLabel(writer, trigger, "");
        printRadio(writer, trigger, trigger.getDeferred(), "start", "startDate", "showdiv('repeatRows');");
        String dateText = "";
        if (trigger.getStartDateTime() != null) {
            dateText = new SimpleDateFormat(trigger.getDateFormat()).format(trigger.getStartDateTime());
        }

        printInputCalendar(writer, trigger, dateText, "startDateTime", true);

        //printLabel(writer, trigger, (trigger.getPrintDateFormat() ? ("(" + trigger.getDateFormat() + ")") : "&nbsp;"));

        writer.endElement("tr");

        writer.endElement("table");
    }

    protected void writeRepeatRows(ResponseWriter writer, HtmlSimpleTrigger trigger) throws IOException {
        // "Recur:" (recur) "Never"            ""
        // ""       (recur) "Every" <Interval> [units]

        writer.startElement("div", trigger);
        writer.writeAttribute("id", "repeatRows", null);

        writer.startElement("table", trigger);
        writer.writeAttribute("style", getTablePaddingStyle(), null);
        writer.writeAttribute("border", TABLE_BORDER, null);

        writer.startElement("tr", trigger);

        printLabel(writer, trigger, "Recur:", getLeftLabelStyle());
        printRadio(writer, trigger, !trigger.getRepeat(), "recur", "never", "hidediv('endRows');");
        printLabel(writer, trigger, "Never");
        printLabel(writer, trigger, "");
        printLabel(writer, trigger, "");

        writer.endElement("tr");

        writer.startElement("tr", trigger);

        printLabel(writer, trigger, "");
        printRadio(writer, trigger, trigger.getRepeat(), "recur", "every", "showdiv('endRows');");

        MeasurementNumericValueAndUnits bestFit = MeasurementConverter.fit((double) trigger.getRepeatInterval(),
            getMeasurementUnitsFromTimeUnits(trigger.getRepeatUnits()),
            getMeasurementUnitsFromTimeUnits(TimeUnits.Seconds), getMeasurementUnitsFromTimeUnits(TimeUnits.Days));
        NumberFormat nf = DecimalFormat.getNumberInstance();
        nf.setMaximumFractionDigits(1);
        String convertedRepeatInterval = nf.format(bestFit.getValue());
        TimeUnits convertedRepeatedUnits = getTimeUnitsFromMeasurementUnits(bestFit.getUnits());

        // composite row data of "Every <Interval>"
        writer.startElement("td", trigger);
        //writer.writeAttribute("style", "width: 200px;", null);
        printLabel(writer, trigger, "Every ", false);
        writer.endElement("td");

        if (trigger.getReadOnly()) {
            printInputText(writer, trigger, (trigger.getRepeat() ? ("" + convertedRepeatInterval) : ""),
                "repeatInterval", true);
        } else {
            printInputSpinner(writer, trigger, (trigger.getRepeat() ? ("" + convertedRepeatInterval) : ""),
                "repeatInterval", true);
        }

        printDropDown(writer, trigger, TimeUnits.values(), convertedRepeatedUnits, "repeatUnits");

        writer.endElement("tr");

        writer.endElement("table");

        writer.endElement("div");
    }

    private MeasurementUnits getMeasurementUnitsFromTimeUnits(TimeUnits units) {
        if (units == TimeUnits.Seconds) {
            return MeasurementUnits.SECONDS;
        } else if (units == TimeUnits.Minutes) {
            return MeasurementUnits.MINUTES;
        } else if (units == TimeUnits.Hours) {
            return MeasurementUnits.HOURS;
        } else {
            return MeasurementUnits.DAYS;
        }
    }

    private TimeUnits getTimeUnitsFromMeasurementUnits(MeasurementUnits units) {
        if (units == MeasurementUnits.SECONDS) {
            return TimeUnits.Seconds;
        } else if (units == MeasurementUnits.MINUTES) {
            return TimeUnits.Minutes;
        } else if (units == MeasurementUnits.HOURS) {
            return TimeUnits.Hours;
        } else {
            return TimeUnits.Days;
        }
    }

    protected void writeEndDateRows(ResponseWriter writer, HtmlSimpleTrigger trigger) throws IOException {
        // "Recurrence End:" (end) "Never"    ""
        // ""                (end) <DateTime> {format}

        writer.startElement("div", trigger);
        writer.writeAttribute("id", "endRows", null);

        writer.startElement("table", trigger);
        writer.writeAttribute("style", getTablePaddingStyle(), null);
        writer.writeAttribute("border", TABLE_BORDER, null);

        writer.startElement("tr", trigger);

        printLabel(writer, trigger, "Recurrence End:", getLeftLabelStyle());
        printRadio(writer, trigger, !trigger.getTerminate(), "end", "none");
        printLabel(writer, trigger, "None");
        //printLabel(writer, trigger, "");

        writer.endElement("tr");

        writer.startElement("tr", trigger);

        printLabel(writer, trigger, "");
        printRadio(writer, trigger, trigger.getTerminate(), "end", "endDate");
        String dateText = "";
        if (trigger.getEndDateTime() != null) {
            dateText = new SimpleDateFormat(trigger.getDateFormat()).format(trigger.getEndDateTime());
        }

        printInputCalendar(writer, trigger, dateText, "endDateTime", true);
        //printLabel(writer, trigger, (trigger.getPrintDateFormat() ? ("(" + trigger.getDateFormat() + ")") : "&nbsp;"));

        writer.endElement("tr");

        writer.endElement("table");

        writer.endElement("div");
    }

    private void printRadio(ResponseWriter writer, HtmlSimpleTrigger trigger, boolean state, String name, String value)
        throws IOException {
        printRadio(writer, trigger, state, name, value, null);
    }

    private void printRadio(ResponseWriter writer, HtmlSimpleTrigger trigger, boolean state, String name, String value,
        String javascript) throws IOException {
        writer.startElement("td", trigger);

        writer.startElement("input", trigger);
        writer.writeAttribute("value", value, null);
        writer.writeAttribute("name", name, null);
        writer.writeAttribute("type", "radio", null);

        if (state) {
            writer.writeAttribute("CHECKED", "CHECKED", null);
        }

        if (trigger.getReadOnly()) {
            writer.writeAttribute("DISABLED", "DISABLED", null);
        }

        if (javascript != null) {
            writer.writeAttribute("onclick", "javascript:" + javascript, null);
        }

        writer.endElement("input");

        writer.endElement("td");
    }

    private void printLabel(ResponseWriter writer, HtmlSimpleTrigger trigger, String label, boolean wrapped)
        throws IOException {
        printLabel(writer, trigger, label, null, wrapped);
    }

    private void printLabel(ResponseWriter writer, HtmlSimpleTrigger trigger, String label, String style,
        boolean wrapped) throws IOException {
        if (wrapped) {
            writer.startElement("td", trigger);
            if (style != null) {
                writer.writeAttribute("style", style, null);
            }
        }

        // labels can't be "disabled"
        writer.write(label);

        if (wrapped) {
            writer.endElement("td");
        }
    }

    private void printLabel(ResponseWriter writer, HtmlSimpleTrigger trigger, String label) throws IOException {
        printLabel(writer, trigger, label, null, true);
    }

    private void printLabel(ResponseWriter writer, HtmlSimpleTrigger trigger, String label, String style)
        throws IOException {
        printLabel(writer, trigger, label, style, true);
    }

    private void printInputText(ResponseWriter writer, HtmlSimpleTrigger trigger, String value, String id,
        boolean wrapped) throws IOException {
        if (wrapped) {
            writer.startElement("td", trigger);
        }

        writer.startElement("input", trigger);
        writer.writeAttribute("id", id, null);
        writer.writeAttribute("name", id, null);
        writer.writeAttribute("type", "text", null);
        writer.writeAttribute("value", value, null);

        if (trigger.getReadOnly()) {
            writer.writeAttribute("DISABLED", "DISABLED", null);
        }

        writer.endElement("input");

        if (wrapped) {
            writer.endElement("td");
        }
    }

    private void printInputSpinner(ResponseWriter writer, HtmlSimpleTrigger trigger, String value, String id,
        boolean wrapped) throws IOException {
        if (wrapped) {
            writer.startElement("td", trigger);
        }

        HtmlInputNumberSpinner spinner = new HtmlInputNumberSpinner();
        spinner.setEnableManualInput(false);
        spinner.setId(id);
        spinner.setValue(value);
        spinner.setRequired(true);
        spinner.setMinValue("1");
        spinner.setInputSize(2);
        if (trigger.getReadOnly()) {
            spinner.setDisabled(true);
        }
        spinner.encodeAll(currentContext);

        if (wrapped) {
            writer.endElement("td");
        }
    }

    private void printInputCalendar(ResponseWriter writer, HtmlSimpleTrigger trigger, String value, String id,
        boolean wrapped) throws IOException {
        if (wrapped) {
            writer.startElement("td", trigger);
        }
        HtmlCalendar calendar = new HtmlCalendar();
        calendar.setId(id);
        calendar.setValue(value);
        calendar.setEnableManualInput(true);
        calendar.setShowApplyButton(true);
        calendar.setShowWeeksBar(false);
        calendar.setDatePattern(trigger.getDateFormat());
        if (trigger.getReadOnly()) {
            calendar.setDisabled(true);
        }
        calendar.encodeAll(currentContext);
        if (wrapped) {
            writer.endElement("td");
        }
    }

    @SuppressWarnings("unchecked")
    private void printDropDown(ResponseWriter writer, HtmlSimpleTrigger trigger, Enum[] values, Enum value, String id)
        throws IOException {
        writer.startElement("td", trigger);

        writer.startElement("select", trigger);

        if (trigger.getReadOnly()) {
            writer.writeAttribute("DISABLED", "DISABLED", null);
        }

        writer.writeAttribute("id", id, null);
        writer.writeAttribute("name", id, null);
        for (int i = 0; i < values.length; i++) {
            writer.startElement("option", trigger);
            writer.writeAttribute("value", values[i].toString(), null);
            if (values[i] == value) {
                writer.writeAttribute("SELECTED", "SELECTED", null);
            }

            writer.write(values[i].toString());
            writer.endElement("option");
        }

        writer.endElement("select");

        writer.endElement("td");
    }

    private String getLeftLabelStyle() {
        return "width: 100px;";
    }

    private String getTablePaddingStyle() {
        return "padding-top: 5px";
    }
}