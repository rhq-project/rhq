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
package org.rhq.enterprise.gui.common.error;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

/**
 * Renders the necessary HTML to popup a window to display an error message. The link to popup the message will be
 * included in this rendering. The text for the link is the text between the start and end of the tag that uses this
 * renderer.
 *
 * @author Jason Dobies
 */
public class ErrorPopupRenderer extends Renderer {
    // Renderer Overridden Methods  --------------------------------------------

    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent) throws IOException {
        ErrorPopupComponent component = (ErrorPopupComponent) uiComponent;
        String id = component.getPopupId();
        String errorMessage = component.getErrorMessage();

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement("div", null);
        writer.writeAttribute("id", id, null);
        writer.writeAttribute("style", "display: none;", null);

        writer.startElement("div", null);
        writer.writeAttribute("id", "PageHeader", null);
        writer.writeAttribute("align", "right", null);

        writer.startElement("a", null);
        writer.writeAttribute("href", "javascript:window.close()", null);
        writer.writeText("Close Window", null);
        writer.endElement("a");

        writer.endElement("div");

        writer.startElement("div", null);

        writer.startElement("pre", null);
        writer.writeAttribute("class", "StackTrace", null);
        writer.writeAttribute("wrap", "on", null);

        writer.writeText(errorMessage, null);

        writer.endElement("pre");
        writer.endElement("div");
        writer.endElement("div");

        writer.startElement("a", null);
        writer.writeAttribute("href", "javascript:popupStackTrace('" + id + "')", null);
    }

    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();

        writer.endElement("a");
    }
}