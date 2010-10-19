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

import java.io.IOException;
import java.io.OutputStream;

import org.dbunit.dataset.stream.IDataSetConsumer;
import org.dbunit.dataset.xml.FlatXmlWriter;
import org.rhq.helpers.perftest.support.Output;

/**
 * Implements the {@link Output} interface to output the database data into an XML file using
 * {@link FlatXmlWriter}.
 * 
 * @author Lukas Krejci
 */
public class XmlOutput implements Output {

    private OutputStream stream;
    private FlatXmlWriter consumer;
    private boolean doClose;
    
    public XmlOutput(OutputStream stream, boolean doClose) {
        this.stream = stream;
        this.doClose = doClose;
    }
    
    public IDataSetConsumer getConsumer() throws Exception {
        if (consumer == null) {
            consumer = new FlatXmlWriter(stream);
        }
        
        return consumer;
    }

    public void close() throws IOException {
        if (doClose) {
            stream.close();
        }
    }

}
