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
package org.rhq.enterprise.gui.common.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;

public class UploadRenderer extends Renderer {
    // UploadRenderer Implementation  --------------------------------------------

    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        if (!component.isRendered()) {
            return;
        }

        ResponseWriter writer = context.getResponseWriter();
        String clientId = component.getClientId(context);

        writer.startElement("input", component);
        writer.writeAttribute("type", "file", "type");
        writer.writeAttribute("name", clientId, "clientId");
        writer.endElement("input");

        // TODO: Make the file input's Browse button look consistent with other RHQ buttons.
        //       (see http://www.quirksmode.org/dom/inputfile.html for a way this can be done).
        writer.flush();
    }

    public void decode(FacesContext context, UIComponent component) {
        ExternalContext external = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) external.getRequest();
        String clientId = component.getClientId(context);
        FileItem item = (FileItem) request.getAttribute(clientId);

        Object newValue;
        ValueExpression valueExpression = component.getValueExpression("value");
        if (valueExpression != null) {
            Class valueType = valueExpression.getType(context.getELContext());
            if (valueType == byte[].class) {
                newValue = item.get();
            } else if (valueType == InputStream.class) {
                try {
                    newValue = item.getInputStream();
                } catch (IOException ex) {
                    throw new FacesException(ex);
                }
            } else {
                String encoding = request.getCharacterEncoding();
                if (encoding != null) {
                    try {
                        newValue = item.getString(encoding);
                    } catch (UnsupportedEncodingException ex) {
                        newValue = item.getString();
                    }
                } else {
                    newValue = item.getString();
                }
            }

            ((EditableValueHolder) component).setSubmittedValue(newValue);
            ((EditableValueHolder) component).setValid(true);
        }

        Object target = component.getAttributes().get("target");

        if (target != null) {
            File file;
            if (target instanceof File) {
                file = (File) target;
            } else {
                ServletContext servletContext = (ServletContext) external.getContext();
                String realPath = servletContext.getRealPath(target.toString());
                file = new File(realPath);
            }

            try {
                item.write(file);
            } catch (Exception ex) {
                throw new FacesException(ex);
            }
        }
    }
}