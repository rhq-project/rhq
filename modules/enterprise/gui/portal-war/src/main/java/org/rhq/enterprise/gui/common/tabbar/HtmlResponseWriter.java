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
package org.rhq.enterprise.gui.common.tabbar;

import java.io.IOException;
import java.io.Writer;
import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;
import com.sun.faces.util.HtmlUtils;

/**
 * An implementation of a JSF {@link ResponseWriter} that does not interact at all with the current FacesContext.
 *
 * @author Ian Springer
 */
public class HtmlResponseWriter extends ResponseWriter {
    private static final String CONTENT_TYPE = "text/html";
    private static final String CHARACTER_ENCODING = "UTF-8";

    private Writer writer;
    private char[] buffer;
    private boolean startElementIsOpen;

    public HtmlResponseWriter(Writer writer) {
        this.writer = writer;
        this.buffer = new char[1024];
    }

    public String getContentType() {
        return CONTENT_TYPE;
    }

    public String getCharacterEncoding() {
        return CHARACTER_ENCODING;
    }

    public void flush() throws IOException {
        closeStartElementIfOpen();
    }

    public void startDocument() throws IOException {
    }

    public void endDocument() throws IOException {
        flush();
    }

    public void startElement(String name, UIComponent component) throws IOException {
        closeStartElementIfOpen();
        this.writer.write("<" + name);

        // Leave the element "open" in case attributes will be added.
        this.startElementIsOpen = true;
    }

    public void endElement(String name) throws IOException {
        closeStartElementIfOpen();

        // TODO: Close "empty" elements like "br" using the abbreviated syntax (e.g. "<br />").
        this.writer.write("</" + name + ">");
    }

    public void writeAttribute(String name, Object value, String componentPropertyName) throws IOException {
        this.writer.write(" ");
        this.writer.write(name);
        this.writer.write("=\"");

        // NOTE: The below HtmlUtils class is from the JSR RI impl jar.
        HtmlUtils.writeAttribute(this.writer, this.buffer, value.toString());
        this.writer.write("\"");
    }

    public void writeURIAttribute(String name, Object value, String componentPropertyName) throws IOException {
        writeAttribute(name, value, componentPropertyName);
    }

    public void writeComment(Object comment) throws IOException {
        closeStartElementIfOpen();
        this.writer.write("<!--");
        this.writer.write(comment.toString());
        this.writer.write("-->");
    }

    public void writeText(Object o, String s) throws IOException {
        // TODO: Implement this method.
    }

    public void writeText(char[] chars, int i, int i1) throws IOException {
        // TODO: Implement this method.
    }

    public ResponseWriter cloneWithWriter(Writer writer) {
        return null; // TODO: Implement this method.
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        closeStartElementIfOpen();
        this.writer.write(cbuf, off, len);
    }

    public void close() throws IOException {
        closeStartElementIfOpen();
        this.writer.close();
    }

    private void closeStartElementIfOpen() throws IOException {
        if (startElementIsOpen) {
            writer.write('>');
            this.startElementIsOpen = false;
        }
    }
}