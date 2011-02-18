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

    private String format = "raw";

    private String file;

    private int pageWidth = 160;

    private TabularWriter tabularWriter;

    private PrintWriter fileWriter;

    private boolean newWriterNeeded;

    public void setTarget(String format, String file) {
        setFormat(format);
        setFile(file);
        initWriter();
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(int width) {
        pageWidth = width;
    }

    public void write(Object object) {
        initWriter();
        tabularWriter.print(object);
        fileWriter.flush();
    }

    public void close() {
        if (fileWriter != null) {
            fileWriter.close();
            newWriterNeeded = true;
        }
    }
    
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
        newWriterNeeded = true;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
        newWriterNeeded = true;
    }

    private void initWriter() {
        if (newWriterNeeded || tabularWriter == null) {
            if (fileWriter != null) {
                fileWriter.close();
            }
            
            if (format == null) {
                throw new IllegalStateException("No format is set. Please set it to 'raw' or 'csv'.");
            }

            if (file == null) {
                throw new IllegalStateException("No file is set. Please specify the file to outut the data to.");
            }

            newWriterNeeded = false;

            try {
                fileWriter = new PrintWriter(new FileWriter(this.file));
                tabularWriter = new TabularWriter(fileWriter, format);
                tabularWriter.setExportMode(true);
            } catch (IOException e) {
                if (fileWriter != null) {
                    fileWriter.close();
                }

                throw new ExportException("Failed to initialize the exporter.", e);
            }
        }
        tabularWriter.setWidth(pageWidth);
    }
}
