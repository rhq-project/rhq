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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.ExportingIterator;
import org.rhq.enterprise.server.sync.util.IndentingXMLStreamWriter;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;
import org.rhq.enterprise.server.xmlschema.ConfigurationInstanceDescriptorUtil;

/**
 * Reading from this input stream produces the export file in a lazy (and therefore memory efficient)
 * manner.
 *
 * @author Lukas Krejci
 */
public class ExportingInputStream extends InputStream {

    private static final Log LOG = LogFactory.getLog(ExportingInputStream.class);

    public static final String CONFIGURATION_NAMESPACE = "urn:xmlns:rhq-configuration";
    public static final String EXPORT_NAMESPACE = "urn:xmlns:rhq-configuration-export";

    public static final String CONFIGURATION_EXPORT_ELEMENT = "configuration-export";
    public static final String ENTITIES_EXPORT_ELEMENT = "entities";
    public static final String ENTITY_EXPORT_ELEMENT = "entity";
    public static final String ERROR_MESSAGE_ELEMENT = "error-message";
    public static final String NOTES_ELEMENT = "notes";
    public static final String DATA_ELEMENT = "data";
    public static final String VALIDATOR_ELEMENT = "validator";
    public static final String DEFAULT_CONFIGURATION_ELEMENT = "default-configuration";
    public static final String ID_ATTRIBUTE = "id";
    public static final String CLASS_ATTRIBUTE = "class";

    private Set<Synchronizer<?, ?>> synchronizers;
    private Map<String, ExporterMessages> messagesPerExporter;
    private PipedInputStream inputStream;
    private PipedOutputStream exportOutput;
    private Thread exportRunner;
    private Throwable unexpectedExporterException;
    private boolean zipOutput;

    private Marshaller configurationMarshaller;

    /**
     * Constructs a new exporting input stream with the default buffer size of 64KB that zips up
     * the results.
     * 
     * @see #ExportingInputStream(Set, Map, int, boolean)
     */
    public ExportingInputStream(Set<Synchronizer<?, ?>> synchronizers, Map<String, ExporterMessages> messagesPerExporter)
        throws IOException {
        this(synchronizers, messagesPerExporter, 65536, true);
    }

    /**
     * Constructs a new exporting input stream with the default buffer size of 64KB.
     * 
     * @param synchronizers the synchronizers to invoke when producing the export file
     * @param messagesPerExporter a reference to a map of messages that the exporters will use to produce additional info about the export
     * @param size the size in bytes of the intermediate buffer
     * @param zip whether to zip the export data
     * @throws IOException on failure
     */
    public ExportingInputStream(Set<Synchronizer<?, ?>> synchronizers,
        Map<String, ExporterMessages> messagesPerExporter, int size, boolean zip) throws IOException {
        this.synchronizers = synchronizers;
        this.messagesPerExporter = messagesPerExporter;
        inputStream = new PipedInputStream(size);
        exportOutput = new PipedOutputStream(inputStream);
        zipOutput = zip;

        try {
            JAXBContext context = JAXBContext.newInstance(DefaultImportConfigurationDescriptor.class);

            configurationMarshaller = context.createMarshaller();
            configurationMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            configurationMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            configurationMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read() throws IOException {
        checkState();
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        checkState();
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkState();
        return inputStream.read(b, off, len);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        checkState();
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        //the lack of checkState() is intentional here
        //because the return value is only dependent
        //on the PipedInputStream and no interaction
        //with the PipedOutputStream is necessary
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        exportRunner.interrupt();
        inputStream.close();
        exportOutput.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    private void checkState() throws IOException {
        if (exportRunner == null) {
            exportRunner = new Thread(new Runnable() {
                public void run() {
                    exporterMain();
                }
            });

            exportRunner.setDaemon(true);
            exportRunner.setName("Configuration Export Thread");

            exportRunner.setContextClassLoader(Thread.currentThread().getContextClassLoader());

            exportRunner.start();
        }

        if (unexpectedExporterException != null) {
            throw new IOException("The exporter thread failed with an uncaught exception.", unexpectedExporterException);
        }
    }

    private void exporterMain() {
        XMLStreamWriter wrt = null;
        OutputStream out = null;
        try {
            XMLOutputFactory ofactory = XMLOutputFactory.newInstance();
            ofactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
            
            try {
                out = exportOutput;
                if (zipOutput) {
                    out = new GZIPOutputStream(out);
                }
                wrt = new IndentingXMLStreamWriter(ofactory.createXMLStreamWriter(out, "UTF-8"));
                //wrt = ofactory.createXMLStreamWriter(out, "UTF-8");
            } catch (XMLStreamException e) {
                LOG.error("Failed to create the XML stream writer to output the export file to.", e);
                return;
            }

            exportPrologue(wrt);

            for (Synchronizer<?, ?> exp : synchronizers) {
                exportSingle(wrt, exp);
            }

            exportEpilogue(wrt);

            wrt.flush();
        } catch (Exception e) {
            LOG.error("Error while exporting.", e);
            unexpectedExporterException = e;
        } finally {
            if (wrt != null) {
                try {
                    wrt.close();
                } catch (XMLStreamException e) {
                    LOG.warn("Failed to close the exporter XML stream.", e);
                }
            }
            safeClose(out);
        }
    }

    /**
     * @param wrt
     * @throws XMLStreamException 
     */
    private void exportPrologue(XMLStreamWriter wrt) throws XMLStreamException {
        wrt.setDefaultNamespace(EXPORT_NAMESPACE);
        wrt.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, EXPORT_NAMESPACE);
        wrt.setPrefix("ci", ConfigurationInstanceDescriptorUtil.NS_CONFIGURATION_INSTANCE);
        wrt.setPrefix("c", CONFIGURATION_NAMESPACE);
        
        NamespaceContext nsContext = new NamespaceContext() {

            //this map has to correspond to the namespaces defined in the code above
            private final Map<String, String> PREFIXES = new HashMap<String, String>();
            {
                PREFIXES.put(XMLConstants.DEFAULT_NS_PREFIX, EXPORT_NAMESPACE);
                PREFIXES.put("ci", ConfigurationInstanceDescriptorUtil.NS_CONFIGURATION_INSTANCE);
                PREFIXES.put("c", CONFIGURATION_NAMESPACE);
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                String prefix = getPrefix(namespaceURI);
                if (prefix == null) {
                    return Collections.<String> emptySet().iterator();
                } else {
                    return Collections.singleton(prefix).iterator();
                }
            }

            @Override
            public String getPrefix(String namespaceURI) {
                if (namespaceURI == null) {
                    throw new IllegalArgumentException();
                } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
                    return XMLConstants.XMLNS_ATTRIBUTE;
                } else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
                    return XMLConstants.XML_NS_PREFIX;
                } else {
                    String prefix = null;
                    for (Map.Entry<String, String> e : PREFIXES.entrySet()) {
                        String p = e.getKey();
                        String namespace = e.getValue();

                        if (namespaceURI.equals(namespace)) {
                            prefix = p;
                            break;
                        }
                    }

                    return prefix;
                }
            }

            @Override
            public String getNamespaceURI(String prefix) {
                return PREFIXES.get(prefix);
            }
        };

        wrt.setNamespaceContext(nsContext);
        
        wrt.writeStartDocument();
        wrt.writeStartElement(EXPORT_NAMESPACE, CONFIGURATION_EXPORT_ELEMENT);
        wrt.writeNamespace("ci", ConfigurationInstanceDescriptorUtil.NS_CONFIGURATION_INSTANCE);
        wrt.writeNamespace("c", CONFIGURATION_NAMESPACE);

        writeValidators(wrt);
    }

    /**
     * @param wrt
     */
    private void writeValidators(XMLStreamWriter wrt) throws XMLStreamException {
        Set<ConsistencyValidator> allValidators = new HashSet<ConsistencyValidator>();

        for (Synchronizer<?, ?> syn : synchronizers) {
            allValidators.addAll(syn.getRequiredValidators());
        }

        for (ConsistencyValidator cv : allValidators) {
            wrt.writeStartElement(VALIDATOR_ELEMENT);
            wrt.writeAttribute(CLASS_ATTRIBUTE, cv.getClass().getName());
            cv.exportState(new ExportWriter(wrt));
            wrt.writeEndElement();
        }
    }

    /**
     * @param wrt
     * @throws XMLStreamException 
     */
    private void exportEpilogue(XMLStreamWriter wrt) throws XMLStreamException {
        wrt.writeEndDocument();
    }

    /**
     * @param wrt
     * @param syn
     * @return
     * @throws XMLStreamException 
     */
    private void exportSingle(XMLStreamWriter wrt, Synchronizer<?, ?> syn) throws XMLStreamException {
        ExporterMessages messages = new ExporterMessages();

        messagesPerExporter.put(syn.getClass().getName(), messages);

        wrt.writeStartElement(EXPORT_NAMESPACE, ENTITIES_EXPORT_ELEMENT);
        wrt.writeAttribute(ID_ATTRIBUTE, syn.getClass().getName());

        Exporter<?, ?> exp = syn.getExporter();
        ExportingIterator<?> it = exp.getExportingIterator();

        DefaultImportConfigurationDescriptor importConfig = getDefaultImportConfiguraton(syn);

        if (importConfig != null) {
            try {
                configurationMarshaller.marshal(importConfig, wrt);
            } catch (JAXBException e) {
                throw new XMLStreamException(e);
            }
        }

        messages.setPerEntityErrorMessages(new ArrayList<String>());
        messages.setPerEntityNotes(new ArrayList<String>());

        while (it.hasNext()) {
            it.next();

            wrt.writeStartElement(EXPORT_NAMESPACE, ENTITY_EXPORT_ELEMENT);

            wrt.writeStartElement(EXPORT_NAMESPACE, DATA_ELEMENT);

            Exception exportError = null;
            try {
                it.export(new ExportWriter(wrt));
            } catch (XMLStreamException e) {
                //there's not much we can do about these but to give up.
                throw e;
            } catch (Exception e) {
                exportError = e;
            }

            wrt.writeEndElement(); //data

            if (exportError == null) {
                String notes = it.getNotes();

                if (notes != null) {
                    messages.getPerEntityNotes().add(notes);
                    wrt.writeStartElement(EXPORT_NAMESPACE, NOTES_ELEMENT);
                    wrt.writeCharacters(notes);
                    wrt.writeEndElement();
                }
            } else {
                String message = ThrowableUtil.getStackAsString(exportError);
                messages.getPerEntityErrorMessages().add(message);
                wrt.writeStartElement(EXPORT_NAMESPACE, ERROR_MESSAGE_ELEMENT);
                wrt.writeCharacters(message);
                wrt.writeEndElement();
            }

            wrt.writeEndElement(); //entity
        }

        String notes = exp.getNotes();

        messages.setExporterNotes(notes);

        if (notes != null) {
            wrt.writeStartElement(EXPORT_NAMESPACE, NOTES_ELEMENT);
            wrt.writeCharacters(notes);
            wrt.writeEndElement();
        }

        wrt.writeEndElement(); //entities
    }

    private static void safeClose(Closeable str) {
        try {
            if (str != null) {
                str.close();
            }
        } catch (IOException e) {
            LOG.error("Failed to close an output stream. This shouldn't happen.", e);
        }
    }

    private DefaultImportConfigurationDescriptor getDefaultImportConfiguraton(Synchronizer<?, ?> synchronizer) {
        DefaultImportConfigurationDescriptor ret = null;

        ConfigurationDefinition def = synchronizer.getImporter().getImportConfigurationDefinition();

        if (def != null) {
            ConfigurationTemplate template = def.getDefaultTemplate();
            if (template != null) {
                ret = DefaultImportConfigurationDescriptor.create(ConfigurationInstanceDescriptorUtil
                    .createConfigurationInstance(def, template.getConfiguration()));
            }
        }

        return ret;
    }
}
