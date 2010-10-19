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

package org.rhq.helpers.perftest.support.output;

import java.io.File;
import java.io.IOException;

import org.dbunit.dataset.csv.CsvDataSetWriter;
import org.dbunit.dataset.stream.IDataSetConsumer;
import org.rhq.helpers.perftest.support.Output;

/**
 * Implements the {@link Output} interface to support output to a set of CSV files using {@link CsvDataSetWriter}.
 * 
 * @author Lukas Krejci
 */
public class CsvOutput implements Output {

    private File directory;
    private CsvDataSetWriter consumer;

    public CsvOutput(File directory) {
        this.directory = directory;
    }

    protected File getDirectory() {
        return directory;
    }
    
    public IDataSetConsumer getConsumer() throws Exception {
        if (consumer == null) {
            consumer = new CsvDataSetWriter(directory);
        }

        return consumer;
    }

    public void close() throws IOException {
    }

}
