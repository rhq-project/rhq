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
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A decorator implementation of the {@link XMLStreamReader} interface to
 * support safe reading from the export stream by multiple "parties".
 * <p>
 * This implementation works with the assumption that each of its users will
 * read the XML data fully contained within another tag.
 * <p>
 * The instantiator of this class will create a new instance of this class
 * when it encounters a tag that it wants the "user" to read from. The instantiator
 * creates a new ExportReader and passes it over to the user. It is guaranteed
 * that the user cannot read past the end tag (to the user, it appears she reached the
 * end of the document). When the ExportReader reaches the "end" of document, the
 * wrapped XML stream will have read the end tag of the start tag that the instantiator
 * first encountered. The current state of the wrapped XML reader will therefore be
 * END_ELEMENT.
 * 
 * @author Lukas Krejci
 */
public class ExportReader implements XMLStreamReader {

    private XMLStreamReader reader;
    private int depth;
    
    public ExportReader(XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * @param name
     * @return
     * @throws IllegalArgumentException
     * @see javax.xml.stream.XMLStreamReader#getProperty(java.lang.String)
     */
    public Object getProperty(String name) throws IllegalArgumentException {
        return reader.getProperty(name);
    }

    /**
     * @return
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamReader#next()
     */
    public int next() throws XMLStreamException {
        int ret = reader.next();
        switch (ret) {
        case XMLStreamReader.START_ELEMENT:
            ++depth;
            break;
        case XMLStreamReader.END_ELEMENT:
            if (depth > 0) {
                --depth;
            } else {
                return XMLStreamReader.END_DOCUMENT;
            }
            break;
        }
        return ret;
    }

    /**
     * @param type
     * @param namespaceURI
     * @param localName
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamReader#require(int, java.lang.String, java.lang.String)
     */
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        reader.require(type, namespaceURI, localName);
    }

    /**
     * @return
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamReader#getElementText()
     */
    public String getElementText() throws XMLStreamException {
        return reader.getElementText();
    }

    /**
     * @return
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamReader#nextTag()
     */
    public int nextTag() throws XMLStreamException {
        int ret = reader.nextTag();
        switch (ret) {
        case XMLStreamReader.START_ELEMENT:
            ++depth;
            break;
        case XMLStreamReader.END_ELEMENT:
            if (depth > 0) {
                --depth;
            } else {
                throw new IllegalStateException("End of the export segment reached.");
            }
        }
        
        return ret;
    }

    /**
     * @return
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamReader#hasNext()
     */
    public boolean hasNext() throws XMLStreamException {
        if (depth == 0 && reader.isEndElement()) {
            return false;
        } else {
            return reader.hasNext();
        }
    }

    /**
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamReader#close()
     */
    public void close() throws XMLStreamException {
        throw new XMLStreamException("Illegal operation on export segment.");
    }

    /**
     * @param prefix
     * @return
     * @see javax.xml.stream.XMLStreamReader#getNamespaceURI(java.lang.String)
     */
    public String getNamespaceURI(String prefix) {
        return reader.getNamespaceURI(prefix);
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#isStartElement()
     */
    public boolean isStartElement() {
        return reader.isStartElement();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#isEndElement()
     */
    public boolean isEndElement() {
        if (depth == 0 && reader.isEndElement()) {
            return false;
        } else {
            return reader.isEndElement();
        }
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#isCharacters()
     */
    public boolean isCharacters() {
        return reader.isCharacters();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#isWhiteSpace()
     */
    public boolean isWhiteSpace() {
        return reader.isWhiteSpace();
    }

    /**
     * @param namespaceURI
     * @param localName
     * @return
     * @see javax.xml.stream.XMLStreamReader#getAttributeValue(java.lang.String, java.lang.String)
     */
    public String getAttributeValue(String namespaceURI, String localName) {
        return reader.getAttributeValue(namespaceURI, localName);
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getAttributeCount()
     */
    public int getAttributeCount() {
        return reader.getAttributeCount();
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#getAttributeName(int)
     */
    public QName getAttributeName(int index) {
        return reader.getAttributeName(index);
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#getAttributeNamespace(int)
     */
    public String getAttributeNamespace(int index) {
        return reader.getAttributeNamespace(index);
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#getAttributeLocalName(int)
     */
    public String getAttributeLocalName(int index) {
        return reader.getAttributeLocalName(index);
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#getAttributePrefix(int)
     */
    public String getAttributePrefix(int index) {
        return reader.getAttributePrefix(index);
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#getAttributeType(int)
     */
    public String getAttributeType(int index) {
        return reader.getAttributeType(index);
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#getAttributeValue(int)
     */
    public String getAttributeValue(int index) {
        return reader.getAttributeValue(index);
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#isAttributeSpecified(int)
     */
    public boolean isAttributeSpecified(int index) {
        return reader.isAttributeSpecified(index);
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getNamespaceCount()
     */
    public int getNamespaceCount() {
        return reader.getNamespaceCount();
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#getNamespacePrefix(int)
     */
    public String getNamespacePrefix(int index) {
        return reader.getNamespacePrefix(index);
    }

    /**
     * @param index
     * @return
     * @see javax.xml.stream.XMLStreamReader#getNamespaceURI(int)
     */
    public String getNamespaceURI(int index) {
        return reader.getNamespaceURI(index);
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getNamespaceContext()
     */
    public NamespaceContext getNamespaceContext() {
        return reader.getNamespaceContext();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getEventType()
     */
    public int getEventType() {
        if (depth == 0 && reader.isEndElement()) {
            return XMLStreamReader.END_DOCUMENT;
        } else {
            return reader.getEventType();
        }
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getText()
     */
    public String getText() {
        return reader.getText();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getTextCharacters()
     */
    public char[] getTextCharacters() {
        return reader.getTextCharacters();
    }

    /**
     * @param sourceStart
     * @param target
     * @param targetStart
     * @param length
     * @return
     * @throws XMLStreamException
     * @see javax.xml.stream.XMLStreamReader#getTextCharacters(int, char[], int, int)
     */
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        return reader.getTextCharacters(sourceStart, target, targetStart, length);
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getTextStart()
     */
    public int getTextStart() {
        return reader.getTextStart();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getTextLength()
     */
    public int getTextLength() {
        return reader.getTextLength();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getEncoding()
     */
    public String getEncoding() {
        return reader.getEncoding();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#hasText()
     */
    public boolean hasText() {
        return reader.hasText();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getLocation()
     */
    public Location getLocation() {
        return reader.getLocation();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getName()
     */
    public QName getName() {
        return reader.getName();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getLocalName()
     */
    public String getLocalName() {
        return reader.getLocalName();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#hasName()
     */
    public boolean hasName() {
        return reader.hasName();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getNamespaceURI()
     */
    public String getNamespaceURI() {
        return reader.getNamespaceURI();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getPrefix()
     */
    public String getPrefix() {
        return reader.getPrefix();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getVersion()
     */
    public String getVersion() {
        return reader.getVersion();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#isStandalone()
     */
    public boolean isStandalone() {
        return reader.isStandalone();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#standaloneSet()
     */
    public boolean standaloneSet() {
        return reader.standaloneSet();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getCharacterEncodingScheme()
     */
    public String getCharacterEncodingScheme() {
        return reader.getCharacterEncodingScheme();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getPITarget()
     */
    public String getPITarget() {
        return reader.getPITarget();
    }

    /**
     * @return
     * @see javax.xml.stream.XMLStreamReader#getPIData()
     */
    public String getPIData() {
        return reader.getPIData();
    }
    
}
