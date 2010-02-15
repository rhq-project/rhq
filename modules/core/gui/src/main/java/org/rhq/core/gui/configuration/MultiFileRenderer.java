/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.gui.configuration;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

/**
 * A renderer that renders an {@link RawConfigurationComponent} component as XHTML.
 *
 * @author Adam Young
 */
public class MultiFileRenderer extends Renderer {

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        writer
            .write("<script language=\"JavaScript\" type=\"text/javascript\" src=\"/js/multi-file-edit.js\" ></script>\n");

        writer.write("<script type=\"text/javascript\">\n");
        writer.write("var multiFileEdit = new MultiFileEdit();\n");

        for (int i = 0; i < 6; i++) {
            writer.write("multiFileEdit.addArchive(\"/tmp/file/" + i + "\",\"/tmp/file" + i + " archive = " + i
                + "\");");
        }

        writer.write("multiFileEdit.downloadUrl = \"download.jsp\";\n");

        writer.write("multiFileEdit.drawTable();\n");
        writer.write("</script>\n");

    }
}
