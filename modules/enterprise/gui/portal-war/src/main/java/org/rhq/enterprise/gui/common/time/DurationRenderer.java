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
package org.rhq.enterprise.gui.common.time;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import com.sun.faces.util.MessageUtils;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.time.DurationComponent.TimeUnit;

/**
 * @author Joseph Marques
 */
public class DurationRenderer extends Renderer {

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
        if (context == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "context"));
        }

        if (component == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "component"));
        }

        if (!component.isRendered()) {
            return;
        }

        DurationComponent duration;
        if (component instanceof DurationComponent) {
            duration = (DurationComponent) component;
        } else {
            return;
        }

        duration.setValue(getDurationValue());
        duration.setUnit(getDurationUnit());
    }

    public int getDurationValue() {
        return FacesContextUtility.getRequiredRequestParameter(DurationComponent.VALUE, Integer.class);
    }

    public String getDurationUnit() {
        return FacesContextUtility.getRequiredRequestParameter(DurationComponent.UNIT, String.class);
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);

        ResponseWriter writer = context.getResponseWriter();

        DurationComponent duration = (DurationComponent) component;

        writer.startElement("input", duration);
        writer.writeAttribute("id", DurationComponent.VALUE, null);
        writer.writeAttribute("name", DurationComponent.VALUE, null);
        writer.writeAttribute("style", "width: 50px;", null);
        writer.writeAttribute("value", duration.getValue(), DurationComponent.VALUE);
        writer.endElement("input");
        writer.write(" "); // space

        writer.startElement("select", duration);
        writer.writeAttribute("id", DurationComponent.UNIT, null);
        writer.writeAttribute("name", DurationComponent.UNIT, null);
        for (TimeUnit unit : duration.getUnitOptions()) {
            writer.startElement("option", duration);
            writer.writeAttribute("value", unit.name(), null);
            if (unit.name() == duration.getUnit()) {
                writer.writeAttribute("SELECTED", "SELECTED", null);
            }
            writer.write(unit.getDisplayName());
            writer.endElement("option");
        }
        writer.endElement("select");
    }
}
