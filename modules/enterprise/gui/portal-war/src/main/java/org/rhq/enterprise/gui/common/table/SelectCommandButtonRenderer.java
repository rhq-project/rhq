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
package org.rhq.enterprise.gui.common.table;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import com.sun.faces.renderkit.AttributeManager;
import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.ButtonRenderer;
import com.sun.faces.util.MessageUtils;

public class SelectCommandButtonRenderer extends ButtonRenderer {
    private static final String[] PASS_THRU_ATTRIBUTES = AttributeManager
        .getAttributes(AttributeManager.Key.COMMANDBUTTON);

    public SelectCommandButtonRenderer() {
        super();
    }

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
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

        SelectCommandButton selectCommandButton = (SelectCommandButton) component;

        ResponseWriter writer = context.getResponseWriter();
        assert (writer != null);

        writer.startElement("input", selectCommandButton);

        /* custom attribs */

        // this is a conditional button, so if 'low' is not specified, it will be 1 by default
        writer.writeAttribute("low", (selectCommandButton.getLow() == null) ? "1" : selectCommandButton.getLow(), null);

        // it's perfectly valid to have a conditional button operate on an arbitrary number of selections
        String high = selectCommandButton.getHigh();
        if (high != null) {
            writer.writeAttribute("high", high, null);
        }

        // the button must be conditional on something
        String target = selectCommandButton.getTarget();
        if (target == null) {
            throw new IllegalStateException("'target' attribute is required on 'selectCommandButton' tag");
        }

        writer.writeAttribute("target", target, "target");

        String styleClass = selectCommandButton.getStyleClass();
        if (styleClass != null) {
            writer.writeAttribute("class", styleClass, null);
        }

        /* standard attribs */
        writer.writeAttribute("type", "submit", null);

        String value = (String) selectCommandButton.getValue();
        writer.writeAttribute("value", value, null);

        // render "pass-thru" attributes (i.e. JSF attributes that have the same name as their HTML counterparts)...
        RenderKitUtils.renderPassThruAttributes(writer, component, PASS_THRU_ATTRIBUTES);

        // render "disabled", "ismap", and "readonly attributes...
        RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);

        String clientId = selectCommandButton.getClientId(context);
        writer.writeAttribute("name", clientId, null);

        writer.endElement("input");
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        super.encodeEnd(context, component);
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        super.encodeChildren(context, component);
    }
}