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
package org.rhq.enterprise.gui.common.quicknav;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * A renderer that renders a {@link QuickNavComponent} component as XHTML.
 *
 * @author Ian Springer
 */
public class QuickNavRenderer extends Renderer {
    private static final String DATA_TABLE_STYLE_CLASS = "data-table";

    /**
     * Encode the beginning of this component.
     *
     * @param facesContext <code>FacesContext</code> for the current request
     * @param component    <code>QuickNavComponent</code> to be encoded
     */
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        QuickNavComponent quickNav = (QuickNavComponent) component;

        // Process f:param child tags...
        quickNav.setParameters(FacesComponentUtility.getParameters(quickNav));

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement("table", quickNav);
        writer.writeAttribute("class", DATA_TABLE_STYLE_CLASS, null);

        writer.startElement("tr", quickNav);
        writer.append("\n");
    }

    /**
     * Encode the ending of this component.
     *
     * @param facesContext <code>FacesContext</code> for the current request
     * @param component    <code>QuickNavComponent</code> to be encoded
     */
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        QuickNavComponent quickNav = (QuickNavComponent) component;

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.endElement("tr");
        writer.endElement("table");
        writer.append("\n");
    }
}