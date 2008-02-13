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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import com.sun.faces.util.MessageUtils;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.scheduling.supporting.TimeUnits;

public class HtmlSimpleTriggerRendererBlockModel implements HtmlSimpleTriggerRendererModel {
    public boolean isAvailable() {
        // piggy-back on some optional get that already suffices
        return getDeferred();
    }

    public boolean getDeferred() {
        return FacesContextUtility.getOptionalRequestParameter("deferred", Boolean.class, false);
    }

    public Date getStartDateTime(DateFormat dateFormatter) {
        String startDateTimeStr = FacesContextUtility.getRequiredRequestParameter("startDateTime");
        try {
            return dateFormatter.parse(startDateTimeStr);
        } catch (ParseException pe) {
            throw new RuntimeException(MessageUtils.getExceptionMessageString(MessageUtils.CONVERSION_ERROR_MESSAGE_ID,
                "component"));
        }
    }

    public boolean getRepeat() {
        return FacesContextUtility.getOptionalRequestParameter("repeat", Boolean.class, false);
    }

    public int getRepeatInterval() {
        return FacesContextUtility.getRequiredRequestParameter("repeatInterval", Integer.class);
    }

    public TimeUnits getRepeatUnits() {
        return FacesContextUtility.getRequiredRequestParameter("repeatUnits", TimeUnits.class);
    }

    public boolean getTerminate() {
        return FacesContextUtility.getOptionalRequestParameter("terminate", Boolean.class, false);
    }

    public Date getEndDateTime(DateFormat dateFormatter) {
        String endDateTimeStr = FacesContextUtility.getRequiredRequestParameter("endDateTime");
        try {
            return dateFormatter.parse(endDateTimeStr);
        } catch (ParseException pe) {
            throw new RuntimeException(MessageUtils.getExceptionMessageString(MessageUtils.CONVERSION_ERROR_MESSAGE_ID,
                "component"));
        }
    }

    public void encode(FacesContext context, HtmlSimpleTrigger trigger) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        writer.startElement("table", trigger);

        writeStartDateRows(writer, trigger);
        writeRepeatRows(writer, trigger);
        writeEndDateRows(writer, trigger);

        writer.endElement("table");
    }

    protected void writeStartDateRows(ResponseWriter writer, HtmlSimpleTrigger trigger) throws IOException {
        writer.startElement("tr", trigger);

        printCheckBox(writer, trigger, trigger.getDeferred(), "deferred", null);
        printLabel(writer, trigger, "Start Date/Time:");
        String dateText = "";
        if (trigger.getStartDateTime() != null) {
            dateText = new SimpleDateFormat(trigger.getDateFormat()).format(trigger.getStartDateTime());
        }

        printInputText(writer, trigger, dateText, "startDateTime", "deferred");
        printLabel(writer, trigger, (trigger.getPrintDateFormat() ? ("(" + trigger.getDateFormat() + ")") : "&nbsp;"));

        writer.endElement("tr");
    }

    protected void writeRepeatRows(ResponseWriter writer, HtmlSimpleTrigger trigger) throws IOException {
        writer.startElement("tr", trigger);

        printCheckBox(writer, trigger, trigger.getRepeat(), "repeat", null);
        printLabel(writer, trigger, "Repeat Inteval:");
        printInputText(writer, trigger, (trigger.getRepeat() ? ("" + trigger.getRepeatInterval()) : ""),
            "repeatInterval", "repeat");
        printDropDown(writer, trigger, TimeUnits.values(), trigger.getRepeatUnits(), "repeatUnits", "repeat");

        writer.endElement("tr");
    }

    protected void writeEndDateRows(ResponseWriter writer, HtmlSimpleTrigger trigger) throws IOException {
        writer.startElement("tr", trigger);

        printCheckBox(writer, trigger, trigger.getTerminate(), "terminate", "repeat");
        printLabel(writer, trigger, "End Date/Time:");
        String dateText = "";
        if (trigger.getEndDateTime() != null) {
            dateText = new SimpleDateFormat(trigger.getDateFormat()).format(trigger.getEndDateTime());
        }

        printInputText(writer, trigger, dateText, "endDateTime", "terminate");
        printLabel(writer, trigger, (trigger.getPrintDateFormat() ? ("(" + trigger.getDateFormat() + ")") : "&nbsp;"));

        writer.endElement("tr");
    }

    private void printCheckBox(ResponseWriter writer, HtmlSimpleTrigger trigger, boolean state, String id,
        String conditionalSelect) throws IOException {
        writer.startElement("td", trigger);

        writer.startElement("input", trigger);
        writer.writeAttribute("id", id, null);
        writer.writeAttribute("name", id, null);
        writer.writeAttribute("type", "checkbox", null);

        if (state) {
            writer.writeAttribute("CHECKED", "CHECKED", null);
        }

        if (trigger.getReadOnly()) {
            writer.writeAttribute("DISABLED", "DISABLED", null);
        }

        if (conditionalSelect != null) {
            writer.writeAttribute("select", conditionalSelect, null);
            writer.writeAttribute("low", "1", null);
            writer.writeAttribute("high", "1", null);
        }

        writer.writeAttribute("onclick", "javascript:updateButtons('" + id + "');", null);
        writer.endElement("input");

        writer.endElement("td");
    }

    private void printLabel(ResponseWriter writer, HtmlSimpleTrigger trigger, String label) throws IOException {
        writer.startElement("td", trigger);

        // labels can't be "disabled"
        writer.write(label);

        writer.endElement("td");
    }

    private void printInputText(ResponseWriter writer, HtmlSimpleTrigger trigger, String value, String id,
        String conditionalSelect) throws IOException {
        writer.startElement("td", trigger);

        writer.startElement("input", trigger);
        writer.writeAttribute("id", id, null);
        writer.writeAttribute("name", id, null);
        writer.writeAttribute("type", "text", null);
        writer.writeAttribute("value", value, null);

        if (trigger.getReadOnly()) {
            writer.writeAttribute("DISABLED", "DISABLED", null);
        }

        if (conditionalSelect != null) {
            writer.writeAttribute("select", conditionalSelect, null);
            writer.writeAttribute("low", "1", null);
            writer.writeAttribute("high", "1", null);
        }

        writer.endElement("input");

        writer.endElement("td");
    }

    @SuppressWarnings("unchecked")
    private void printDropDown(ResponseWriter writer, HtmlSimpleTrigger trigger, Enum[] values, Enum value, String id,
        String conditionalSelect) throws IOException {
        writer.startElement("td", trigger);

        writer.startElement("select", trigger);

        if (conditionalSelect != null) {
            writer.writeAttribute("select", conditionalSelect, null);
            writer.writeAttribute("low", "1", null);
            writer.writeAttribute("high", "1", null);
        }

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
}