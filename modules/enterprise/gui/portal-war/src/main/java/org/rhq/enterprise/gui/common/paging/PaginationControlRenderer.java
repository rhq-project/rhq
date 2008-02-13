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
package org.rhq.enterprise.gui.common.paging;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

public class PaginationControlRenderer extends Renderer {
    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        PaginationControl pc = (PaginationControl) component;
        processAttributes(pc);

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement("span", pc);
        writer.writeAttribute("id", pc, "id");
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        PaginationControl pc = (PaginationControl) component;
        processAttributes(pc);

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.endElement("span");
    }

    private void processAttributes(UIComponent component) {
        if (component.getId() == null) {
            throw new IllegalStateException("The paginationControl element requires an 'id' attribute.");
        }
    }
}