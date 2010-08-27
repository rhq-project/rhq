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

import java.io.IOException;
import java.io.InputStream;

import org.dbunit.dataset.stream.IDataSetProducer;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.rhq.helpers.perftest.support.Input;
import org.xml.sax.InputSource;

/**
 *
 * @author Lukas Krejci
 */
public class XmlInput implements Input {

    InputStream stream;
    FlatXmlProducer producer;
    boolean doClose;
    
    public XmlInput(InputStream stream, boolean doClose) {
        this.stream = stream;
        this.doClose = doClose;
    }
    
    public void close() throws IOException {
        if (doClose) {
            stream.close();
        }
    }

    public IDataSetProducer getProducer() throws Exception {
        if (producer == null) {
            producer = new FlatXmlProducer(new InputSource(stream));
        }
        
        return producer;
    }
}
