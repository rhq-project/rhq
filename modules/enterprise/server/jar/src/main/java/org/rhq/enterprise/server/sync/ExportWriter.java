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

package org.rhq.enterprise.server.sync;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * This is a decorator implementation of the {@link XMLStreamWriter} that
 * will disallow the users from doing illegal things during export (like closing the writer)
 * but at the same time implements the full {@link XMLStreamWriter} interface so that
 * it can be more interoperable with other tooling.
 *
 * @author Lukas Krejci
 */
public class ExportWriter implements XMLStreamWriter {

    private int depth;
    private XMLStreamWriter wrt;
    
    public ExportWriter(XMLStreamWriter wrapped) {
        wrt = wrapped;
    }
    
    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        wrt.writeStartElement(localName);
        ++depth;
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        wrt.writeStartElement(namespaceURI, localName);
        ++depth;
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        wrt.writeStartElement(prefix, localName, namespaceURI);
        ++depth;
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        wrt.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        wrt.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        wrt.writeEmptyElement(localName);
    }

    /**
     * @see javax.xml.stream.XMLStreamWriter#writeEndElement()
     * 
     * @throws IllegalExporterActionException if the writer would end an element
     * not previously defined by this writer (i.e. if it would end an element that this
     * writer wrote to and thus would escape out of the exporter's "shell").
     */
    @Override
    public void writeEndElement() throws XMLStreamException, IllegalExporterActionException {
        if (depth < 1) {
            throw new IllegalExporterActionException();
        }
        
        wrt.writeEndElement();
        
        --depth;
    }

    /**
     * @throws IllegalExporterActionException exporters aren't allowed to do this.
     */
    @Override
    public void writeEndDocument() throws XMLStreamException {
        throw new IllegalExporterActionException();
    }

    @Override
    public void close() throws XMLStreamException {
        throw new IllegalExporterActionException();
    }

    @Override
    public void flush() throws XMLStreamException {
        wrt.flush();
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        wrt.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
        throws XMLStreamException {
        
        wrt.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        wrt.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        wrt.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        wrt.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        wrt.writeComment(data);
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        wrt.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        wrt.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        wrt.writeCData(data);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        wrt.writeDTD(dtd);
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        wrt.writeEntityRef(name);
    }

    /**
     * @throws IllegalExporterActionException
     */
    @Override
    public void writeStartDocument() throws XMLStreamException {
        throw new IllegalExporterActionException();
    }
    
    /**
     * @throws IllegalExporterActionException
     */
    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        throw new IllegalExporterActionException();
    }

    /**
     * @throws IllegalExporterActionException
     */
    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        throw new IllegalExporterActionException();
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        wrt.writeCharacters(text);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        wrt.writeCharacters(text, start, len);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return wrt.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        wrt.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        wrt.setDefaultNamespace(uri);
    }

    /**
     * @throws IllegalExporterActionException
     */
    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        throw new IllegalExporterActionException();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return wrt.getNamespaceContext();
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return wrt.getProperty(name);
    }
}
