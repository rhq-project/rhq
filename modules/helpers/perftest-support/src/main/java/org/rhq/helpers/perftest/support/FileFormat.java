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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.rhq.helpers.perftest.support.input.CsvInput;
import org.rhq.helpers.perftest.support.input.FileInputStreamProvider;
import org.rhq.helpers.perftest.support.input.InputStreamProvider;
import org.rhq.helpers.perftest.support.input.SystemInProvider;
import org.rhq.helpers.perftest.support.input.XmlInput;
import org.rhq.helpers.perftest.support.input.ZipInputStreamProviderDecorator;
import org.rhq.helpers.perftest.support.input.ZippedCsvInput;
import org.rhq.helpers.perftest.support.output.CsvOutput;
import org.rhq.helpers.perftest.support.output.XmlOutput;
import org.rhq.helpers.perftest.support.output.ZippedCsvOutput;
import org.rhq.helpers.perftest.support.output.ZippedXmlOutput;

/**
 * Represents a file format to export/import database data to/from.
 *
 * @author Lukas Krejci
 */
public enum FileFormat {

    /**
     * XML file format. The input/output specification is understood to be a path to an input/output file.
     */
    XML {
        public Input getInput(String inputSpec) throws IOException {
            InputStreamProvider isp = inputSpec == null ? new SystemInProvider() : new FileInputStreamProvider(
                new File(inputSpec));
            return new XmlInput(isp, inputSpec != null);
        }

        public Input getInput(InputStreamProvider provider) throws IOException {
            return new XmlInput(provider, true);
        }
        
        public Output getOutput(String outputSpec) throws IOException {
            OutputStream stream = outputSpec == null ? System.out : new FileOutputStream(outputSpec);

            return new XmlOutput(stream, outputSpec != null);
        }
    },

    /**
     * CSV file format. The input/output specification is understood to be a path to a pre-existing
     * directory that should contain the input/output files.
     */
    CSV {
        public Input getInput(String inputSpec) throws IOException {
            File directory = new File(inputSpec);

            return new CsvInput(directory);
        }

        public Input getInput(InputStreamProvider provider) throws IOException {
            throw new UnsupportedOperationException();
        }
        
        public Output getOutput(String outputSpec) throws IOException {
            File directory = new File(outputSpec);
            return new CsvOutput(directory);
        }
    },

    /**
     * Zipped XML format. The input/output specification is understood to be a path to a zip file
     * containing the xml file.
     */
    ZIPPED_XML {
        public Input getInput(String inputSpec) throws IOException {
            InputStreamProvider isp = inputSpec == null ? new SystemInProvider() : new FileInputStreamProvider(
                new File(inputSpec));
            return new XmlInput(new ZipInputStreamProviderDecorator(isp, true), inputSpec != null);
        }

        public Input getInput(InputStreamProvider provider) {
            return new XmlInput(new ZipInputStreamProviderDecorator(provider, true), true);
        }
        
        public Output getOutput(String outputSpec) throws IOException {
            OutputStream stream = outputSpec == null ? System.out : new FileOutputStream(outputSpec);
            return new ZippedXmlOutput(stream, outputSpec != null);
        }
    },

    /**
     * Zipped CSV format. The input/output specification is understood to be a path to a zip file
     * containing the compressed directory with the CSV files.
     */
    ZIPPED_CSV {
        public Input getInput(String inputSpec) throws IOException {
            File file = new File(inputSpec);

            return new ZippedCsvInput(new ZipInputStreamProviderDecorator(new FileInputStreamProvider(file), false));
        }

        public Input getInput(InputStreamProvider provider) throws IOException {
            if (provider instanceof ZipInputStreamProviderDecorator) {
                return new ZippedCsvInput((ZipInputStreamProviderDecorator)provider);
            } else {
                return new ZippedCsvInput(new ZipInputStreamProviderDecorator(provider, false));
            }
        }
        
        public Output getOutput(String outputSpec) throws IOException {
            File file = new File(outputSpec);
            return new ZippedCsvOutput(file);
        }
    };

    /**
     * Returns an {@link Input} instance based on the input specification.
     * The format of the input specification is file format dependent.
     * 
     * @param inputSpec the "location" of the input
     * @return an {@link Input} instance able to process the inputSpec
     */
    public abstract Input getInput(String inputSpec) throws IOException;

    /**
     * Returns an {@link Input} instance initialized with the specified
     * {@link InputStreamProvider}. This enables more "low-level" usage than
     * the {@link #getInput(String)} method which assumes that the inputSpec can
     * specify the input location without any other contextual information.
     *  
     * @param provider
     * @return
     * @throws IOException
     * @throws UnsupportedOperationException if this file format doesn't support this type of initialization.
     */
    public abstract Input getInput(InputStreamProvider provider) throws IOException;

    /**
     * Returns an {@link Output} instance based on the output specification.
     * The format of the output specification is file format dependent.
     * 
     * @param outputSpec the "location" of the output
     * @return an {@link Output} instance able to process the outputSpec
     */
    public abstract Output getOutput(String outputSpec) throws IOException;
}
