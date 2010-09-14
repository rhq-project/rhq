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

package org.rhq.helpers.perftest.support.input;

import java.io.File;
import java.io.IOException;

import org.dbunit.dataset.csv.CsvProducer;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.rhq.helpers.perftest.support.Input;

/**
 * Implements the {@link Input} interface for the set of CSV files using the {@link CsvProducer}.
 * 
 * @author Lukas Krejci
 */
public class CsvInput implements Input {

    private File directory;
    private CsvProducer producer;
    
    public CsvInput(File directory) {
        this.directory = directory;
    }
    
    protected File getDirectory() {
        return directory;
    }
    
    protected CsvProducer getCsvProducer() {
        return producer;
    }
    
    public IDataSetProducer getProducer() throws Exception {
        if (producer == null) {
            if (!directory.isDirectory()) {
                throw new IllegalArgumentException("'" + directory.getAbsolutePath()
                    + "' must be a directory for CSV type input.");
            }
            
            producer = new CsvProducer(directory);
        }
        
        return producer;
    }

    public void close() throws IOException {
        producer = null;
    }

}
