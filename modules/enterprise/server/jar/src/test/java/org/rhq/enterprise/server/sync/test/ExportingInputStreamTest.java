/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.test;

import static org.testng.Assert.assertEquals;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.annotations.Test;
import org.w3c.dom.Document;

import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.enterprise.server.sync.ExportingInputStream;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.Exporters;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class ExportingInputStreamTest {

    private static class Exporter1 extends Exporters.DummyExporter<Void> {
        public Exporter1() {
            super(Void.class);
        }
    }
    
    private static class Exporter2 extends Exporters.DummyExporter<Integer> {
        public Exporter2() {
            super(Integer.class);
        }
    }
    
    public void testWithExportersFailingToInit() throws Exception {
        Exporter1 exporter1 = new Exporter1();
        Exporter2 exporter2 = new Exporter2();
        
        Set<Exporter<?>> exporters = new HashSet<Exporter<?>>();
        exporters.add(exporter1);
        exporters.add(exporter2);
        
        InputStream export = new ExportingInputStream(exporters, new HashMap<String, ExporterMessages>());
        
        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
        Document doc = bld.parse(export);
        
        assertEquals(ExportingInputStream.CONFIGURATION_EXPORT_ELEMENT, doc.getDocumentElement().getNodeName());
    }
}
