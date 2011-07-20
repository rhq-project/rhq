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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.enterprise.server.sync.ExportException;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.sync.ExportingInputStream;
import org.rhq.enterprise.server.sync.exporters.AbstractDelegatingExportingIterator;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.Exporters;
import org.rhq.enterprise.server.sync.exporters.ExportingIterator;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class ExportingInputStreamTest {

    private static class FailingExporter1 extends Exporters.DummyExporter<Void> {
        public FailingExporter1() {
            super(Void.class);
        }
    }
    
    private static class FailingExporter2 extends Exporters.DummyExporter<Integer> {
        public FailingExporter2() {
            super(Integer.class);
        }
    }
    
    private static class ListToStringExporter<T> implements Exporter<T> {

        List<T> valuesToExport;
        Class<T> clazz;
        
        private class Iterator extends AbstractDelegatingExportingIterator<T> {
            public Iterator() {
                super(valuesToExport.iterator());
            }
            
            public void export(ExportWriter output) throws XMLStreamException {
                output.writeStartElement("item");
                output.writeCData(getCurrent().toString());
                output.writeEndElement();
            }
            
            public String getNotes() {
                return null;
            }
        }
        
        public ListToStringExporter(Class<T> clz, List<T> valuesToExport) {
            clazz = clz;
            this.valuesToExport = valuesToExport;
        }
        
        public Class<T> exportedEntityType() {
            return clazz;
        }

        public void init() throws ExportException {
        }

        public ExportingIterator<T> getExportingIterator() {
            return new Iterator();
        }

        public String getNotes() {
            return valuesToExport.toString();
        }        
    }
    
    private class StringListExporter extends ListToStringExporter<String> {
        public StringListExporter(List<String> list) {
            super(String.class, list);
        }
    }
    
    private class IntegerListExporter extends ListToStringExporter<Integer> {
        public IntegerListExporter(List<Integer> list) {
            super(Integer.class, list);
        }
    }
    
    public void testWithExportersFailingToInit() throws Exception {
        Set<Exporter<?>> exporters = this.<Exporter<?>>asSet(new FailingExporter1(), new FailingExporter2());
        
        InputStream export = new ExportingInputStream(exporters, new HashMap<String, ExporterMessages>(), 1024, false);
        
        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
        Document doc = bld.parse(export);
        
        Element root = doc.getDocumentElement();
        
        assertEquals(ExportingInputStream.CONFIGURATION_EXPORT_ELEMENT, root.getNodeName());
        
        assertEquals(root.getChildNodes().getLength(), 2, "Unexpected number of entities elements");
        
        Element export1 = (Element) root.getChildNodes().item(0);
        Element export2 = (Element) root.getChildNodes().item(1);
        
        assertEquals(export1.getAttribute("id"), FailingExporter1.class.getName());
        assertEquals(export2.getAttribute("id"), FailingExporter2.class.getName());

        for(int i = 0; i < root.getChildNodes().getLength(); ++i) {
            Element entitiesElement = (Element) root.getChildNodes().item(i);
            
            assertEquals(entitiesElement.getNodeName(), ExportingInputStream.ENTITIES_EXPORT_ELEMENT);            
            assertEquals(entitiesElement.getChildNodes().getLength(), 1, "Unexpected number of elements in the entities export on index " + i);
            
            Element errorMessageElement = (Element) entitiesElement.getChildNodes().item(0);
            
            assertEquals(errorMessageElement.getNodeName(), ExportingInputStream.ERROR_MESSAGE_ELEMENT);
        }
    }
    
    public void testWithFullyWorkingExporters() throws Exception {
        StringListExporter ex1 = new StringListExporter(Arrays.asList("a", "b", "c"));
        IntegerListExporter ex2 = new IntegerListExporter(Arrays.asList(1, 2, 3));
        
        Set<Exporter<?>> exporters = this.<Exporter<?>>asSet(ex1, ex2);
        
        InputStream export = new ExportingInputStream(exporters, new HashMap<String, ExporterMessages>(), 1024, false);
        
        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
        Document doc = bld.parse(export);
        
        Element root = doc.getDocumentElement();
        
        assertEquals(ExportingInputStream.CONFIGURATION_EXPORT_ELEMENT, root.getNodeName());
        
        assertEquals(root.getChildNodes().getLength(), 2, "Unexpected number of entities elements");
        
        Element export1 = (Element) root.getChildNodes().item(0);
        Element export2 = (Element) root.getChildNodes().item(1);
        
        assertEquals(export1.getAttribute("id"), StringListExporter.class.getName());
        assertEquals(export2.getAttribute("id"), IntegerListExporter.class.getName());

        for(int i = 0; i < root.getChildNodes().getLength(); ++i) {
            Element entitiesElement = (Element) root.getChildNodes().item(i);
            
            assertEquals(entitiesElement.getNodeName(), ExportingInputStream.ENTITIES_EXPORT_ELEMENT);
            
            //TODO finish this
        }
    }
    
    private <T> LinkedHashSet<T> asSet(T... ts) {
        LinkedHashSet<T> ret = new LinkedHashSet<T>();
        for (T t : ts) {
            ret.add(t);
        }
        
        return ret;
    }
}
