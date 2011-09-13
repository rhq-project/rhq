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

package org.rhq.enterprise.server.sync.util;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class IndentingXMLStreamWriter implements XMLStreamWriter {

    public static final String DEFAULT_INDENT_STRING = "    ";
    
    private int depth;
    private XMLStreamWriter wrapped;
    private String indentString;
    private boolean seenData;
    
    public IndentingXMLStreamWriter(XMLStreamWriter wrapped) {
        this(wrapped, DEFAULT_INDENT_STRING);
    }
    
    public IndentingXMLStreamWriter(XMLStreamWriter wrapped, String indentString) {
        this.wrapped = wrapped;
        this.indentString = indentString;
    }
    
    private void onStartElement() throws XMLStreamException {
        wrapped.writeCharacters("\n");
        writeIndent();
        ++depth;
        seenData = false;
    }
    
    private void onEndElement() throws XMLStreamException{
        --depth;
        if (!seenData) {
            wrapped.writeCharacters("\n");
            writeIndent();
        }
        seenData = false;
    }
    
    private void onEmptyElement() throws XMLStreamException {
        seenData = false;
        wrapped.writeCharacters("\n");
        writeIndent();
    }
    
    private void writeIndent() throws XMLStreamException {
        for(int i = 0; i < depth; ++i) {
            wrapped.writeCharacters(indentString);
        }
    }
    
    /**
     * @param localName
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeStartElement(java.lang.String)
     */
    public void writeStartElement(String localName) throws XMLStreamException {
        onStartElement();
        wrapped.writeStartElement(localName);
    }

    /**
     * @param namespaceURI
     * @param localName
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeStartElement(java.lang.String, java.lang.String)
     */
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        onStartElement();
        wrapped.writeStartElement(namespaceURI, localName);
    }

    /**
     * @param prefix
     * @param localName
     * @param namespaceURI
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeStartElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onStartElement();
        wrapped.writeStartElement(prefix, localName, namespaceURI);
    }

    /**
     * @param namespaceURI
     * @param localName
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeEmptyElement(java.lang.String, java.lang.String)
     */
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        onEmptyElement();
        wrapped.writeEmptyElement(namespaceURI, localName);
    }

    /**
     * @param prefix
     * @param localName
     * @param namespaceURI
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeEmptyElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        onEmptyElement();
        wrapped.writeEmptyElement(prefix, localName, namespaceURI);
    }

    /**
     * @param localName
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeEmptyElement(java.lang.String)
     */
    public void writeEmptyElement(String localName) throws XMLStreamException {
        onEmptyElement();
        wrapped.writeEmptyElement(localName);
    }

    /**
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeEndElement()
     */
    public void writeEndElement() throws XMLStreamException {
        onEndElement();
        wrapped.writeEndElement();
    }

    /**
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeEndDocument()
     */
    public void writeEndDocument() throws XMLStreamException {
        while (depth > 0) {
            writeEndElement();
        }
        wrapped.writeEndDocument();
    }

    /**
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#close()
     */
    public void close() throws XMLStreamException {
        wrapped.close();
    }

    /**
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#flush()
     */
    public void flush() throws XMLStreamException {
        wrapped.flush();
    }

    /**
     * @param localName
     * @param value
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeAttribute(java.lang.String, java.lang.String)
     */
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        wrapped.writeAttribute(localName, value);
    }

    /**
     * @param prefix
     * @param namespaceURI
     * @param localName
     * @param value
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeAttribute(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
        throws XMLStreamException {
        wrapped.writeAttribute(prefix, namespaceURI, localName, value);
    }

    /**
     * @param namespaceURI
     * @param localName
     * @param value
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeAttribute(java.lang.String, java.lang.String, java.lang.String)
     */
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        wrapped.writeAttribute(namespaceURI, localName, value);
    }

    /**
     * @param prefix
     * @param namespaceURI
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeNamespace(java.lang.String, java.lang.String)
     */
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        wrapped.writeNamespace(prefix, namespaceURI);
    }

    /**
     * @param namespaceURI
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeDefaultNamespace(java.lang.String)
     */
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        wrapped.writeDefaultNamespace(namespaceURI);
    }

    /**
     * @param data
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeComment(java.lang.String)
     */
    public void writeComment(String data) throws XMLStreamException {
        wrapped.writeComment(data);
    }

    /**
     * @param target
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeProcessingInstruction(java.lang.String)
     */
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        wrapped.writeProcessingInstruction(target);
    }

    /**
     * @param target
     * @param data
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeProcessingInstruction(java.lang.String, java.lang.String)
     */
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        wrapped.writeProcessingInstruction(target, data);
    }

    /**
     * @param data
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeCData(java.lang.String)
     */
    public void writeCData(String data) throws XMLStreamException {
        wrapped.writeCData(data);
        seenData = true;
    }

    /**
     * @param dtd
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeDTD(java.lang.String)
     */
    public void writeDTD(String dtd) throws XMLStreamException {
        writeIndent();
        wrapped.writeDTD(dtd);
    }

    /**
     * @param name
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeEntityRef(java.lang.String)
     */
    public void writeEntityRef(String name) throws XMLStreamException {
        wrapped.writeEntityRef(name);
    }

    /**
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeStartDocument()
     */
    public void writeStartDocument() throws XMLStreamException {
        wrapped.writeStartDocument();
    }

    /**
     * @param version
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeStartDocument(java.lang.String)
     */
    public void writeStartDocument(String version) throws XMLStreamException {
        wrapped.writeStartDocument(version);
    }

    /**
     * @param encoding
     * @param version
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeStartDocument(java.lang.String, java.lang.String)
     */
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        wrapped.writeStartDocument(encoding, version);
    }

    /**
     * @param text
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeCharacters(java.lang.String)
     */
    public void writeCharacters(String text) throws XMLStreamException {
        wrapped.writeCharacters(text);
        seenData = true;
    }

    /**
     * @param text
     * @param start
     * @param len
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#writeCharacters(char[], int, int)
     */
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        wrapped.writeCharacters(text, start, len);
        seenData = true;
    }

    /**
     * @param uri
     * @return
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#getPrefix(java.lang.String)
     */
    public String getPrefix(String uri) throws XMLStreamException {
        return wrapped.getPrefix(uri);
    }

    /**
     * @param prefix
     * @param uri
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#setPrefix(java.lang.String, java.lang.String)
     */
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        wrapped.setPrefix(prefix, uri);
    }

    /**
     * @param uri
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#setDefaultNamespace(java.lang.String)
     */
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        wrapped.setDefaultNamespace(uri);
    }

    /**
     * @param context
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamWriter#setNamespaceContext(javax.xml.namespace.NamespaceContext)
     */
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        wrapped.setNamespaceContext(context);
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamWriter#getNamespaceContext()
     */
    public NamespaceContext getNamespaceContext() {
        return wrapped.getNamespaceContext();
    }

    /**
     * @param name
     * @return
     * @throws IllegalArgumentException
     * @see javax.xml.stream.XMLStreamWriter#getProperty(java.lang.String)
     */
    public Object getProperty(String name) throws IllegalArgumentException {
        return wrapped.getProperty(name);
    }
}
