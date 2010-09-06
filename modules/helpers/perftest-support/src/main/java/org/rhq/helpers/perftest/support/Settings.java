/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rhq.helpers.perftest.support.input.CsvInput;
import org.rhq.helpers.perftest.support.input.XmlInput;
import org.rhq.helpers.perftest.support.output.CsvOutput;
import org.rhq.helpers.perftest.support.output.XmlOutput;

/**
 *
 * @author Lukas Krejci
 */
public class Settings {

    public static final String DATABASE_DRIVER_CLASS_PROPERTY = "driverClass";
    public static final String DATABASE_URL_PROPERTY = "url";
    public static final String DATABASE_USER_PROPERTY = "user";
    public static final String DATABASE_PASSWORD_PROPERTY = "password";

    public static final String NULL_REPLACEMENT = "%NULL%";

    private Settings() {

    }

    /**
     * Creates an "output object" which is a wrapper object able to create a dbUnit consumer
     * that is then used to "consume" the database data and produce an output. The wrapper can
     * then close any system resources that the consumer used.
     * <p>
     * For XML format, the <code>outputSpec</code> can be either a file name or null (in which case
     * the xml is written to standard output).
     * <p>
     * For CSV format, the <code>outputSpec</code> is a path to a directory (possibly non-existing)
     * to which the CSV files corresponding to database tables will be written.
     * 
     * @param fileFormat one of the values specified in {@link FileFormat} (case-insensitive)
     * @param outputSpec format dependent specifier of output location
     * @return an output object
     * @throws FileNotFoundException 
     */
    public static Output getOutputObject(String fileFormat, final String outputSpec) throws FileNotFoundException {

        FileFormat format = fileFormat == null ? FileFormat.XML : Enum.valueOf(FileFormat.class,
            fileFormat.toUpperCase());

        if (format == null) {
            throw new IllegalArgumentException("Unknown file format specified: " + fileFormat);
        }

        switch (format) {
        case XML:
            OutputStream stream = outputSpec == null ? System.out : new FileOutputStream(new File(outputSpec));
            return new XmlOutput(stream, stream != System.out);
        case CSV:
            return new CsvOutput(new File(outputSpec));
        default:
            throw new IllegalStateException("FileFormat defined, but don't know how to create it: " + format);
        }
    }

    /**
     * An analogous method to {@link #getOutputObject(String, String)} only handling input.
     * 
     * @param fileFormat
     * @param inputSpec
     * @return
     */
    public static Input getInputObject(String fileFormat, final String inputSpec) throws IOException {
        FileFormat format = fileFormat == null ? FileFormat.XML : Enum.valueOf(FileFormat.class,
            fileFormat.toUpperCase());

        if (format == null) {
            throw new IllegalArgumentException("Unknown file format specified: " + fileFormat);
        }

        switch (format) {
        case XML:
            InputStream stream = inputSpec == null ? System.in : new FileInputStream(new File(inputSpec));            
            return new XmlInput(stream, stream != System.in);
        case CSV:
            return new CsvInput(new File(inputSpec));
        default:
            throw new IllegalStateException("FileFormat defined, but don't know how to create it: " + format);
        }
    }
}
