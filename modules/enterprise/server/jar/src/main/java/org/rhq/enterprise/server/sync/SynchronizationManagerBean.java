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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.sync.ExportReport;
import org.rhq.core.domain.sync.ConsistencyValidatorFailureReport;
import org.rhq.core.domain.sync.ExportWrapper;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.domain.sync.ImportReport;
import org.rhq.core.domain.sync.ImporterConfiguration;
import org.rhq.core.domain.sync.ImporterConfigurationDefinition;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.importers.ExportedEntityMatcher;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Stateless
public class SynchronizationManagerBean implements SynchronizationManagerLocal, SynchronizationManagerRemote {

    private static final Log LOG = LogFactory.getLog(SynchronizationManagerBean.class);
    
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;
    
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ExportReport exportAllSubsystems(Subject subject) {
        ExportWrapper localExport = exportAllSubsystemsLocally(subject);

        byte[] buffer = new byte[65536];

        ByteArrayOutputStream out = new ByteArrayOutputStream(10240); //10KB is a reasonable minimum size of an export

        try {
            int cnt = 0;
            while ((cnt = localExport.getExportFile().read(buffer)) != -1) {
                out.write(buffer, 0, cnt);
            }

            return new ExportReport(localExport.getMessagesPerExporter(), out.toByteArray());
        } catch (Exception e) {
            return new ExportReport(e.getMessage());
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                //this doesn't happen - out is backed by just an array
                LOG.error("Closing a byte array output stream failed. This should never happen.");
            }

            try {
                localExport.getExportFile().close();
            } catch (Exception e) {
                LOG.warn("Failed to close the export file stream.", e);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ExportWrapper exportAllSubsystemsLocally(Subject subject) {
        Set<Exporter<?, ?>> allExporters = new HashSet<Exporter<?, ?>>();
        Map<String, ExporterMessages> messages = new HashMap<String, ExporterMessages>();

        for (SynchronizedEntity e : SynchronizedEntity.values()) {
            Exporter<?, ?> exporter = e.getExporter();
            exporter.init(subject);
            allExporters.add(exporter);
        }

        try {
            return new ExportWrapper(messages, new ExportingInputStream(allExporters, messages));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize the export.", e);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void importAllSubsystems(Subject subject, InputStream exportFile,
        List<ImporterConfiguration> configurations) throws ValidationException, ImportException {
        try {
            processExportFile(subject, exportFile, true, getConfigPerImporter(configurations));
        } catch (XMLStreamException e) {
            throw new ImportException("Failed import due to XML parsing error.", e);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void importAllSubsystems(Subject subject, byte[] exportFile,
        List<ImporterConfiguration> configurations) throws ValidationException, ImportException {
        importAllSubsystems(subject, new ByteArrayInputStream(exportFile), configurations);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void validate(Subject subject, InputStream exportFile) throws ValidationException {
        try {
            processExportFile(subject, exportFile, false, Collections.<String, Configuration>emptyMap());
        } catch (XMLStreamException e) {
            throw new ValidationException("Failed to parse the export file.", e);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void validate(Subject subject, byte[] exportFile) throws ValidationException {
        validate(subject, new ByteArrayInputStream(exportFile));
    }

    @Override
    public ImporterConfigurationDefinition getImporterConfigurationDefinition(String importerClass) {
        try {
            Class<?> cls = Class.forName(importerClass);
            if (!Importer.class.isAssignableFrom(cls)) {
                LOG.debug("Supplied importer class does not implement the importer interface: '" + importerClass + "'.");
                return null;
            }

            Importer<?, ?> imp = (Importer<?, ?>) cls.newInstance();

            return new ImporterConfigurationDefinition(importerClass, imp.getImportConfigurationDefinition());
        } catch (ClassNotFoundException e) {
            LOG.debug("Supplied importer class is invalid: '" + importerClass + "'.", e);
            return null;
        } catch (Exception e) {
            LOG.error("Failed to instantiate the importer '" + importerClass + "'. This should not happen.");
            throw new IllegalStateException("Failed to instantiate importer '" + importerClass + ".", e);
        }
    }

    @Override
    public List<ImporterConfigurationDefinition> getConfigurationDefinitionOfAllImporters() {
        List<ImporterConfigurationDefinition> ret = new ArrayList<ImporterConfigurationDefinition>();

        for (SynchronizedEntity e : SynchronizedEntity.values()) {
            Importer<?, ?> imp = e.getImporter();
            ret.add(new ImporterConfigurationDefinition(imp.getClass().getName(), imp
                .getImportConfigurationDefinition()));
        }

        return ret;
    }

    private void processExportFile(Subject subject, InputStream exportFile, boolean doImport, Map<String, Configuration> importerConfigs) throws ValidationException,
        ImportException, XMLStreamException {
        XMLStreamReader rdr = XMLInputFactory.newInstance().createXMLStreamReader(exportFile);

        Set<ConsistencyValidatorFailureReport> failures = new HashSet<ConsistencyValidatorFailureReport>();

        while (rdr.hasNext()) {
            switch (rdr.next()) {
            case XMLStreamReader.START_ELEMENT:
                String tagName = rdr.getName().getLocalPart();
                if (ExportingInputStream.VALIDATOR_ELEMENT.equals(tagName)) {
                    try {
                        validateSingle(rdr);
                    } catch (Exception e) {
                        String validatorClass = rdr.getAttributeValue(null, ExportingInputStream.CLASS_ATTRIBUTE);
                        failures.add(new ConsistencyValidatorFailureReport(validatorClass, printExceptionToString(e)));
                    }
                } else if (doImport && ExportingInputStream.ENTITIES_EXPORT_ELEMENT.equals(tagName)) {
                    try {
                        importSingle(subject, importerConfigs, rdr);
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
    }

    private void validateSingle(XMLStreamReader rdr) throws Exception {
        String validatorClassName = rdr.getAttributeValue(null, ExportingInputStream.CLASS_ATTRIBUTE);
        ConsistencyValidator validator = instantiate(
            validatorClassName,
            ConsistencyValidator.class,
            "The validator class denoted in the export file ('%s') does not implement the ConsistencyValidator interface. This should not happen.");

        //move into the configuration of the validator 
        rdr.next();

        //perform the validation
        validator.initializeValidation(new ExportReader(rdr));
        validator.validate();

        //now skip everything in the XML until the next element
        while (rdr.hasNext()) {
            int state = rdr.nextTag();
            if (state == XMLStreamReader.START_ELEMENT) {
                break;
            }
        }
    }

    private <E, X> void importSingle(Subject subject, Map<String, Configuration> importConfigs, XMLStreamReader rdr) throws Exception {
        String importerClassName = rdr.getAttributeValue(null, ExportingInputStream.ID_ATTRIBUTE);

        @SuppressWarnings("unchecked")
        Importer<E, X> importer = instantiate(importerClassName, Importer.class,
            "The importer denoted in the export file ('%s') does not implement the importer interface. This should not happen.");

        importer.init(subject, entityManager, importConfigs.get(importerClassName));
        
        ExportedEntityMatcher<E, X> matcher = importer.getExportedEntityMatcher();
        
        while(rdr.hasNext()) {
            boolean bailout = false;
            switch(rdr.next()) {
            case XMLStreamConstants.START_ELEMENT:
                if (ExportingInputStream.DATA_ELEMENT.equals(rdr.getName().getLocalPart())) {
                    rdr.next();
                    X exportedEntity = importer.unmarshallExportedEntity(new ExportReader(rdr));
                    E entity = matcher == null ? null : matcher.findMatch(exportedEntity);
                    importer.update(entity, exportedEntity);
                }
                break;
            case XMLStreamConstants.END_ELEMENT:
                if (ExportingInputStream.ENTITIES_EXPORT_ELEMENT.equals(rdr.getName().getLocalPart())) {
                    bailout = true;
                }
            }
            
            if (bailout) {
                break;
            }
        }
        
        importer.finishImport();
    }

    private static String printExceptionToString(Throwable t) {
        StringWriter str = new StringWriter();
        PrintWriter wrt = new PrintWriter(str);
        t.printStackTrace(wrt);
        return str.toString();
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
    
    private Map<String, Configuration> getConfigPerImporter(List<ImporterConfiguration> list) {
        Map<String, Configuration> ret = new HashMap<String, Configuration>();
        
        for(ImporterConfiguration ic : list) {
            ret.put(ic.getImporterClassName(), ic.getConfiguration());
        }
        
        return ret;
    }
}
