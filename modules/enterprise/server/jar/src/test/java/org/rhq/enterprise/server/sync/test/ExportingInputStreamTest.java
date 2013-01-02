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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Expectations;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.sync.ExportingInputStream;
import org.rhq.enterprise.server.sync.ImportException;
import org.rhq.enterprise.server.sync.NoSingleEntity;
import org.rhq.enterprise.server.sync.SynchronizationConstants;
import org.rhq.enterprise.server.sync.Synchronizer;
import org.rhq.enterprise.server.sync.exporters.AbstractDelegatingExportingIterator;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.ExportingIterator;
import org.rhq.enterprise.server.sync.exporters.JAXBExportingIterator;
import org.rhq.enterprise.server.sync.importers.ExportedEntityMatcher;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;
import org.rhq.enterprise.server.sync.validators.EntityValidator;
import org.rhq.test.JMockTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class ExportingInputStreamTest extends JMockTest {

    private static final Log LOG = LogFactory.getLog(ExportingInputStreamTest.class);
    
    private static class ListToStringExporter<T> implements Exporter<NoSingleEntity, T> {

        List<T> valuesToExport;
        
        public static final String NOTE_PREFIX = "Wow, I just exported an item from a list: ";
        
        private class Iterator extends AbstractDelegatingExportingIterator<T, T> {
            public Iterator() {
                super(valuesToExport.iterator());
            }
            
            public void export(ExportWriter output) throws XMLStreamException {
                output.writeStartElement("datum");
                output.writeCharacters(getCurrent().toString());
                output.writeEndElement();
            }
            
            public String getNotes() {
                return NOTE_PREFIX + getCurrent();
            }
            
            protected T convert(T object) {
                return object;
            }
        }
        
        public ListToStringExporter(List<T> valuesToExport) {
            this.valuesToExport = valuesToExport;
        }
                
        public ExportingIterator<T> getExportingIterator() {
            return new Iterator();
        }

        public String getNotes() {
            return valuesToExport.toString();
        }        
    }
    
    private static class DummyImporter<T> implements Importer<NoSingleEntity, T> {

        @Override
        public ConfigurationDefinition getImportConfigurationDefinition() {
            return null;
        }

        @Override
        public void configure(Configuration importConfiguration) {
        }

        @Override
        public ExportedEntityMatcher<NoSingleEntity, T> getExportedEntityMatcher() {
            return null;
        }

        @Override
        public void update(NoSingleEntity entity, T exportedEntity) throws Exception {
        }

        @Override
        public T unmarshallExportedEntity(ExportReader reader) throws XMLStreamException {
            return null;
        }

        @Override
        public String finishImport() throws Exception {
            return null;
        }

        @Override
        public Set<EntityValidator<T>> getEntityValidators() {
            return Collections.emptySet();
        }
        
    }
    
    private static class ListToStringSynchronizer<T> implements Synchronizer<NoSingleEntity, T> {
        private List<T> list;
        
        public ListToStringSynchronizer(List<T> list) {
            this.list = list;
        }
        
        @Override
        public Exporter<NoSingleEntity, T> getExporter() {
            return new ListToStringExporter<T>(list);
        }
        
        @Override
        public Importer<NoSingleEntity, T> getImporter() {
            return new DummyImporter<T>();
        }
        
        @Override
        public Set<ConsistencyValidator> getRequiredValidators() {
            return Collections.emptySet();            
        }
        
        @Override
        public void initialize(Subject subject, EntityManager entityManager) {
        }
    }
    
    private static class StringListSynchronizer extends ListToStringSynchronizer<String> {
        public StringListSynchronizer(List<String> list) {
            super(list);
        }
    }
    
    private static class IntegerListSynchronizer extends ListToStringSynchronizer<Integer> {
        public IntegerListSynchronizer(List<Integer> list) {
            super(list);
        }
    }
    
    public static class Entity {
        public int value;
        
        public Entity(int value) {
            this.value = value;
        }
    } 
    
    @XmlRootElement(name = "exported-entity")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ExportedEntity {
        
        @XmlAttribute
        public int property;
    }
    
    public static class JAXBExporter implements Exporter<Entity, ExportedEntity> {

        private static class JAXBIterator extends JAXBExportingIterator<ExportedEntity, Entity> {

            public JAXBIterator(java.util.Iterator<Entity> sourceIterator) {
                super(sourceIterator, ExportedEntity.class);
            }
            
            @Override
            protected ExportedEntity convert(Entity object) {
                ExportedEntity ret = new ExportedEntity();
                ret.property = object.value;
                
                return ret;
            }

            @Override
            public String getNotes() {
                return null;
            }
        }
        
        @Override
        public ExportingIterator<ExportedEntity> getExportingIterator() {
            List<Entity> data = new ArrayList<Entity>();
            for(int i = 0; i < 4; ++i) {
                data.add(new Entity(i));
            }
            
            return new JAXBIterator(data.iterator());
        }

        @Override
        public String getNotes() {
            return null;
        }
        
    }
    
    public static class JAXBImporter implements Importer<Entity, ExportedEntity> {

        private Unmarshaller unmarshaller;
        {
            try {
                JAXBContext context = JAXBContext.newInstance(ExportedEntity.class);
                unmarshaller = context.createUnmarshaller();
            } catch (JAXBException e) {
                throw new IllegalStateException(e);
            }
        }
        
        @Override
        public ConfigurationDefinition getImportConfigurationDefinition() {
            return null;
        }

        @Override
        public void configure(Configuration importConfiguration) {
        }

        @Override
        public ExportedEntityMatcher<Entity, ExportedEntity> getExportedEntityMatcher() {
            return new ExportedEntityMatcher<Entity, ExportedEntity>() {
                @Override
                public Entity findMatch(ExportedEntity object) {
                    return new Entity(object.property);
                }
            };
        }

        @Override
        public Set<EntityValidator<ExportedEntity>> getEntityValidators() {
            return Collections.emptySet();
        }

        @Override
        public void update(Entity entity, ExportedEntity exportedEntity) throws Exception {
            entity.value = exportedEntity.property;
        }

        @Override
        public ExportedEntity unmarshallExportedEntity(ExportReader reader) throws XMLStreamException {
            try {
                return (ExportedEntity) unmarshaller.unmarshal(reader);
            } catch (JAXBException e) {
                throw new XMLStreamException(e);
            }
        }

        @Override
        public String finishImport() throws Exception {
            return null;
        }
        
    }
    
    public static class JAXBSynchronizer implements Synchronizer<Entity, ExportedEntity> {

        @Override
        public void initialize(Subject subject, EntityManager entityManager) {
        }

        @Override
        public Exporter<Entity, ExportedEntity> getExporter() {
            return new JAXBExporter();
        }

        @Override
        public Importer<Entity, ExportedEntity> getImporter() {
            return new JAXBImporter();
        }

        @Override
        public Set<ConsistencyValidator> getRequiredValidators() {
            return Collections.emptySet();
        }
        
    }

    public void testSucessfulExport() throws Exception {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        
        StringListSynchronizer ex1 = new StringListSynchronizer(list1);
        IntegerListSynchronizer ex2 = new IntegerListSynchronizer(list2);
        
        Set<Synchronizer<?, ?>> exporters = this.<Synchronizer<?, ?>>asSet(ex1, ex2);
        
        InputStream export = new ExportingInputStream(exporters, new HashMap<String, ExporterMessages>(), 1024, false);
        
        String exportContents = readAll(new InputStreamReader(export, "UTF-8"));
        
        LOG.info("Export contents:\n" + exportContents);
        
        export = new ByteArrayInputStream(exportContents.getBytes("UTF-8"));
        
        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
        Document doc = bld.parse(export);
        
        Element root = doc.getDocumentElement();
        
        assertEquals(SynchronizationConstants.CONFIGURATION_EXPORT_ELEMENT, root.getNodeName());
        
        NodeList entities = root.getElementsByTagName(SynchronizationConstants.ENTITIES_EXPORT_ELEMENT);
        assertEquals(entities.getLength(), 2, "Unexpected number of entities elements");
        
        Element export1 = (Element) entities.item(0);
        Element export2 = (Element) entities.item(1);
        
        assertEquals(export1.getAttribute("id"), StringListSynchronizer.class.getName());
        assertEquals(export2.getAttribute("id"), IntegerListSynchronizer.class.getName());

        String[] expectedNotes = new String[] {list1.toString(), list2.toString()};
        
        for(int i = 0, elementIndex = 0; i < root.getChildNodes().getLength(); ++i) {
            Node node = root.getChildNodes().item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            
            Element entitiesElement = (Element) node;
            
            assertEquals(entitiesElement.getNodeName(), SynchronizationConstants.ENTITIES_EXPORT_ELEMENT);
            
            NodeList errorMessages = entitiesElement.getElementsByTagName(SynchronizationConstants.ERROR_MESSAGE_ELEMENT);
            assertEquals(errorMessages.getLength(), 0, "Unexpected number of error message elements in an entities export.");
            
            Node note = getDirectChildByTagName(entitiesElement, SynchronizationConstants.NOTES_ELEMENT);
            
            assertNotNull(note, "Couldn't find exporter notes.");
            
            String notesText = ((Element)note).getTextContent();
            assertEquals(notesText, expectedNotes[elementIndex], "Unexpected notes for entities.");
            
            NodeList entityElements = entitiesElement.getElementsByTagName(SynchronizationConstants.ENTITY_EXPORT_ELEMENT);
            
            assertEquals(entityElements.getLength(), 3, "Unexpected number of exported entities.");
            
            for(int j = 0; j < entityElements.getLength(); ++j) {
                Element entityElement = (Element) entityElements.item(j);
                
                errorMessages = entityElement.getElementsByTagName(SynchronizationConstants.ERROR_MESSAGE_ELEMENT);
                assertEquals(errorMessages.getLength(), 0, "Unexpected number of error message elements in an entity.");
                
                note = getDirectChildByTagName(entityElement, SynchronizationConstants.NOTES_ELEMENT);
                assertNotNull(note, "Could not find notes for an exported entity.");
                
                Node data = getDirectChildByTagName(entityElement, SynchronizationConstants.DATA_ELEMENT);
                assertNotNull(data, "Could not find data element in the entity.");
                
                Node datum = getDirectChildByTagName(data, "datum");
                assertNotNull(datum, "Could not find the exported datum element containing the actual data.");

                String datumText = ((Element) datum).getTextContent();
                notesText = ((Element)note).getTextContent();
                 
                assertEquals(notesText, ListToStringExporter.NOTE_PREFIX + datumText,
                    "Unexpected discrepancy between data and notes in the export.");
            }
            
            ++elementIndex;
        }
    }
    
    @Test
    public void testJAXBHandled() throws Exception {
        JAXBSynchronizer sync = new JAXBSynchronizer();
        
        Set<Synchronizer<?, ?>>syncs = this.<Synchronizer<?, ?>> asSet(sync);

        InputStream export = new ExportingInputStream(syncs, new HashMap<String, ExporterMessages>(), 1024, false);

        String exportContents = readAll(new InputStreamReader(export, "UTF-8"));
        
        XMLStreamReader rdr = XMLInputFactory.newInstance().createXMLStreamReader(
            new ByteArrayInputStream(exportContents.getBytes(Charset.forName("UTF-8"))));

        try {
            while (rdr.hasNext()) {
                switch (rdr.next()) {
                case XMLStreamReader.START_ELEMENT:
                    String tagName = rdr.getName().getLocalPart();
                    if (SynchronizationConstants.ENTITIES_EXPORT_ELEMENT.equals(tagName)) {
                        try {
                            importSingle(rdr);
                        } catch (Exception e) {
                            //fail fast on the import errors... This runs in a single transaction
                            //so all imports done so far will get rolled-back.
                            //(Even if we change our minds later and run a transaction per importer
                            //we should fail fast to prevent further damage due to possible
                            //constraint violations in the db, etc.)
                            throw new ImportException("Import failed.", e);
                        }
                    }
                    break;
                }
            }
        } finally {
            rdr.close();
        }
    }

    private <E, X> void importSingle(XMLStreamReader rdr)
        throws Exception {
        String synchronizerClassName = rdr.getAttributeValue(null, SynchronizationConstants.ID_ATTRIBUTE);

        @SuppressWarnings("unchecked")
        Synchronizer<E, X> synchronizer = instantiate(synchronizerClassName, Synchronizer.class,
            "The synchronizer denoted in the export file ('%s') does not implement the importer interface. This should not happen.");

        Importer<E, X> importer = synchronizer.getImporter();

        ExportedEntityMatcher<E, X> matcher = importer.getExportedEntityMatcher();

        //the passed in configuration has precedence over the default one inlined in 
        //the config file.
        while (rdr.hasNext()) {
            boolean bailout = false;
            switch (rdr.next()) {
            case XMLStreamConstants.START_ELEMENT:
                if (SynchronizationConstants.DATA_ELEMENT.equals(rdr.getName().getLocalPart())) {
                    
                    rdr.nextTag();
                    X exportedEntity = importer.unmarshallExportedEntity(new ExportReader(rdr));
                    E entity = matcher == null ? null : matcher.findMatch(exportedEntity);
                    importer.update(entity, exportedEntity);
                }
                break;
            case XMLStreamConstants.END_ELEMENT:
                if (SynchronizationConstants.ENTITIES_EXPORT_ELEMENT.equals(rdr.getName().getLocalPart())) {
                    bailout = true;
                }
            }

            if (bailout) {
                break;
            }
        }
    }
    
    private <T> T instantiate(String className, Class<T> desiredClass, String notAssignableErrorMessage)
        throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class<?> cls = Class.forName(className);
        if (!desiredClass.isAssignableFrom(cls)) {
            throw new IllegalStateException(String.format(notAssignableErrorMessage, className, desiredClass.getName()));
        }

        Object instance = cls.newInstance();

        return desiredClass.cast(instance);
    }
    
    @Test(expectedExceptions = IOException.class)
    public void testExceptionHandling_Exporter_getExportingIterator() throws Exception {
        final Exporter<?, ?> failingExporter = context.mock(Exporter.class);
        
        final Synchronizer<?, ?> syncer = context.mock(Synchronizer.class);
        
        context.checking(new Expectations() {
            {
                RuntimeException failure = new RuntimeException("Injected failure");
                              
                allowing(failingExporter).getExportingIterator();
                will(throwException(failure));
                
                allowing(syncer).getRequiredValidators();
                will(returnValue(Collections.emptySet()));
                
                allowing(syncer).getExporter();
                will(returnValue(failingExporter));
            }
        });
        
        Set<Synchronizer<?, ?>> syncers = this.<Synchronizer<?, ?>>asSet(syncer);
        
        InputStream export = new ExportingInputStream(syncers, new HashMap<String, ExporterMessages>(), 1024, false);

        readAll(new InputStreamReader(export, "UTF-8"));

        //this should never be invoked, because reading the input stream should cause the exporter
        //to fail...
        
        fail("Successfully read the export even though one of the exporters threw an exception when asked for the exported entity iterator.");
    }
    
    @Test(expectedExceptions = IOException.class)
    public void testExceptionHandling_ExportingIterator_next() throws Exception {
        final ExportingIterator<?> iterator = context.mock(ExportingIterator.class);        
        final Exporter<?, ?> exporter = context.mock(Exporter.class);
        final Importer<?, ?> importer = context.mock(Importer.class);
        final Synchronizer<?, ?> syncer = context.mock(Synchronizer.class);
        
        context.checking(new Expectations() {
            {
                RuntimeException failure = new RuntimeException("Injected failure");
                          
                allowing(iterator).hasNext();
                will(returnValue(true));
                
                allowing(iterator).next();
                will(onConsecutiveCalls(returnValue("Success"), throwException(failure)));
                
                allowing(iterator).export(with(any(ExportWriter.class)));
                
                allowing(iterator).getNotes();
                
                allowing(exporter).getExportingIterator();
                will(returnValue(iterator));
                
                allowing(exporter).getNotes();
                
                allowing(syncer).getRequiredValidators();
                will(returnValue(Collections.emptySet()));
                
                allowing(syncer).getExporter();
                will(returnValue(exporter));
                
                allowing(syncer).getImporter();
                will(returnValue(importer));
                
                allowing(importer).getImportConfigurationDefinition();
            }
        });
        
        Set<Synchronizer<?, ?>> syncers = this.<Synchronizer<?, ?>>asSet(syncer);
        
        InputStream export = new ExportingInputStream(syncers, new HashMap<String, ExporterMessages>(), 1024, false);

        readAll(new InputStreamReader(export, "UTF-8"));

        //this should never be invoked, because reading the input stream should cause the exporter
        //to fail...
        
        fail("Successfully read the export even though one of the exporters threw an exception when asked for the next exported entity.");
    }
    
    public void testExceptionHandling_ExportingIterator_export() throws Exception {
        final ExportingIterator<?> iterator = context.mock(ExportingIterator.class);        
        final Exporter<?, ?> exporter = context.mock(Exporter.class);
        final Importer<?, ?> importer = context.mock(Importer.class);
        final Synchronizer<?, ?> syncer = context.mock(Synchronizer.class);
        
        context.checking(new Expectations() {
            {
                RuntimeException failure = new RuntimeException("Injected failure");
                          
                allowing(iterator).hasNext();
                will(onConsecutiveCalls(returnValue(true), returnValue(true), returnValue(false)));
                
                allowing(iterator).next();
                
                allowing(iterator).export(with(any(ExportWriter.class)));
                will(onConsecutiveCalls(returnValue(null), throwException(failure)));
                
                allowing(iterator).getNotes();
                
                allowing(exporter).getExportingIterator();
                will(returnValue(iterator));
                
                allowing(exporter).getNotes();
                
                allowing(syncer).getRequiredValidators();
                will(returnValue(Collections.emptySet()));
                
                allowing(syncer).getExporter();
                will(returnValue(exporter));
                
                allowing(syncer).getImporter();
                will(returnValue(importer));
                
                allowing(importer).getImportConfigurationDefinition();
            }
        });
        
        Set<Synchronizer<?, ?>> syncers = this.<Synchronizer<?, ?>>asSet(syncer);
        
        InputStream export = new ExportingInputStream(syncers, new HashMap<String, ExporterMessages>(), 1024, false);

        String exportContents = readAll(new InputStreamReader(export, "UTF-8"));

        LOG.warn("Export contents:\n" + exportContents);

        export = new ByteArrayInputStream(exportContents.getBytes("UTF-8"));

        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        
        Document doc = bld.parse(export);
        
        Element root = doc.getDocumentElement();
        
        NodeList entities = root.getElementsByTagName(SynchronizationConstants.ENTITY_EXPORT_ELEMENT);
        
        assertEquals(entities.getLength(), 2, "Unexpected number of exported elements");
        
        //get the entity with the error
        Element failedEntity = (Element) entities.item(1);
        Node errorMessage = getDirectChildByTagName(failedEntity, SynchronizationConstants.ERROR_MESSAGE_ELEMENT);
        assertNotNull(errorMessage, "Could not find the error-message element at the entity that failed to export.");
    }

    private <T> LinkedHashSet<T> asSet(T... ts) {
        LinkedHashSet<T> ret = new LinkedHashSet<T>();
        for (T t : ts) {
            ret.add(t);
        }
        
        return ret;
    }
    
    private static String readAll(Reader rdr) throws IOException {
        try {
            StringBuilder bld = new StringBuilder();
            int c;
            while((c = rdr.read()) != -1) {
                bld.append((char) c);
            }
            
            return bld.toString();
        } finally {
            rdr.close();
        }
    }
    
    private static Node getDirectChildByTagName(Node node, String tagName) {
        for(int i = 0; i < node.getChildNodes().getLength(); ++i) {
            Node n = node.getChildNodes().item(i);
            if (n.getNodeName().equals(tagName)) {
                return n;
            }
        }
     
        return null;
    }
}
