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

package org.rhq.bindings.export;

import org.rhq.bindings.output.TabularWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Exporter {

    private String format;

    private String file;

    private TabularWriter tabularWriter;

    private FileWriter fileWriter;

    public void setTarget(String format, String file) {
        try {
            this.format = format;
            this.file = file;
            fileWriter = new FileWriter(this.file);
            tabularWriter = new TabularWriter(new PrintWriter(fileWriter), format);
            tabularWriter.setExportMode(true);
        } catch (IOException e) {
            throw new ExportException(e);
        }
    }

    public int getPageWidth() {
        return tabularWriter.getWidth();
    }

    public void setPageWidth(int width) {
        tabularWriter.setWidth(width);
    }

    public void write(Object object) {
        try {
            tabularWriter.print(object);
            fileWriter.flush();
        } catch (IOException e) {
            throw new ExportException(e);
        }
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
