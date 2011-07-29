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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.ExportingIterator;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;

/**
 * Reading from this input stream produces the export file in a lazy (and therefore memory efficient)
 * manner.
 *
 * @author Lukas Krejci
 */
public class ExportingInputStream extends InputStream {

    private static final Log LOG = LogFactory.getLog(ExportingInputStream.class);

    public static final String CONFIGURATION_EXPORT_ELEMENT = "configuration-export";
    public static final String ENTITIES_EXPORT_ELEMENT = "entities";
    public static final String ENTITY_EXPORT_ELEMENT = "entity";
    public static final String ERROR_MESSAGE_ELEMENT = "error-message";
    public static final String NOTES_ELEMENT = "notes";
    public static final String DATA_ELEMENT = "data";
    public static final String VALIDATOR_ELEMENT = "validator";
    public static final String ID_ATTRIBUTE = "id";
    public static final String CLASS_ATTRIBUTE = "class";
    
    private Set<Exporter<?, ?>> exporters;
    private Map<String, ExporterMessages> messagesPerExporter;
    private PipedInputStream inputStream;
    private PipedOutputStream exportOutput;
    private Thread exportRunner;
    private Throwable uncaughtExporterException;
    private boolean zipOutput;
    
    /**
     * Constructs a new exporting input stream with the default buffer size of 64KB that zips up
     * the results.
     * 
     * @see #ExportingInputStream(Set, Map, int, boolean)
     */
    public ExportingInputStream(Set<Exporter<?, ?>> exportersToUse, Map<String, ExporterMessages> messagesPerExporter)
        throws IOException {
        this(exportersToUse, messagesPerExporter, 65536, true);
    }

    /**
     * Constructs a new exporting input stream with the default buffer size of 64KB.
     * 
     * @param exportersToUse the exporters to invoke when producing the export file
     * @param messagesPerExporter a reference to a map of messages that the exporters will use to produce additional info about the export
     * @param size the size in bytes of the intermediate buffer
     * @param zip whether to zip the export data
     * @throws IOException on failure
     */
    public ExportingInputStream(Set<Exporter<?, ?>> exportersToUse, Map<String, ExporterMessages> messagesPerExporter,
        int size, boolean zip) throws IOException {
        exporters = exportersToUse;
        this.messagesPerExporter = messagesPerExporter;
        inputStream = new PipedInputStream(size);
        exportOutput = new PipedOutputStream(inputStream);
        zipOutput = zip;
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
            exportRunner.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    uncaughtExporterException = e;
                }
            });
            
            exportRunner.setContextClassLoader(Thread.currentThread().getContextClassLoader());
            
            exportRunner.start();
        }

        if (uncaughtExporterException != null) {
            throw new IOException("The exporter thread failed with an uncaught exception.", uncaughtExporterException);
        }
    }

    private void exporterMain() {
        XMLStreamWriter wrt = null;
        OutputStream out = null;
        try {
            XMLOutputFactory ofactory = XMLOutputFactory.newInstance();

            try {
                out = exportOutput;
                if (zipOutput) {                    
                    out = new GZIPOutputStream(out);
                }
                wrt = ofactory.createXMLStreamWriter(out, "UTF-8");
            } catch (XMLStreamException e) {
                LOG.error("Failed to create the XML stream writer to output the export file to.", e);
                return;
            }

            exportPrologue(wrt);
            
            for (Exporter<?, ?> exp : exporters) {
                exportSingle(wrt, exp);
            }
            
            exportEpilogue(wrt);         
            
            wrt.flush();
        } catch (Exception e) {
            LOG.error("Error while exporting.", e);
            throw new RuntimeException(e);
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
        wrt.setDefaultNamespace("urn:xmlns:rhq-configuration-export");
        wrt.writeStartDocument();
        wrt.writeStartElement(CONFIGURATION_EXPORT_ELEMENT);
        writeValidators(wrt);
    }

    /**
     * @param wrt
     */
    private void writeValidators(XMLStreamWriter wrt) throws XMLStreamException {
        Set<ConsistencyValidator> allValidators = new HashSet<ConsistencyValidator>();
        
        for(Exporter<?, ?> exp : exporters) {
            allValidators.addAll(exp.getRequiredValidators());
        }
        
        for(ConsistencyValidator cv : allValidators) {
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
     * @param exp
     * @return
     * @throws XMLStreamException 
     */
    private void exportSingle(XMLStreamWriter wrt, Exporter<?, ?> exp) throws XMLStreamException {
        ExporterMessages messages = new ExporterMessages();

        messagesPerExporter.put(exp.getClass().getName(), messages);
        
        wrt.writeStartElement(ENTITIES_EXPORT_ELEMENT);
        wrt.writeAttribute(ID_ATTRIBUTE, exp.getImporterType().getName());
        
        ExportingIterator<?> it = exp.getExportingIterator();

        messages.setPerEntityErrorMessages(new ArrayList<String>());
        messages.setPerEntityNotes(new ArrayList<String>());

        while (it.hasNext()) {
            it.next();

            wrt.writeStartElement(ENTITY_EXPORT_ELEMENT);
            
            try {
                wrt.writeStartElement(DATA_ELEMENT);
                it.export(new ExportWriter(wrt));
                wrt.writeEndElement();
                
                String notes = it.getNotes();
                
                if (notes != null) {
                    messages.getPerEntityNotes().add(notes);
                    wrt.writeStartElement(NOTES_ELEMENT);
                    wrt.writeCharacters(notes);
                    wrt.writeEndElement();
                }
            } catch (XMLStreamException e) {
                //there's not much we can do about these but to give up.
                throw e;
            } catch (Exception e) {
                String message = ThrowableUtil.getStackAsString(e);
                messages.getPerEntityErrorMessages().add(message);
                wrt.writeStartElement(ERROR_MESSAGE_ELEMENT);
                wrt.writeCharacters(message);
                wrt.writeEndElement();
            }
            
            wrt.writeEndElement(); //entity
        }
        
        String notes = exp.getNotes();
        
        messages.setExporterNotes(notes);
        
        if (notes != null) {
            wrt.writeStartElement(NOTES_ELEMENT);
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
}
