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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.sync.ConsistencyValidatorFailureReport;
import org.rhq.core.domain.sync.ExportReport;
import org.rhq.core.domain.sync.ExportWrapper;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.domain.sync.ImportConfiguration;
import org.rhq.core.domain.sync.ImportConfigurationDefinition;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.sync.importers.ExportedEntityMatcher;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;
import org.rhq.enterprise.server.sync.validators.InconsistentStateException;

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
        Set<Synchronizer<?, ?>> allSynchronizers = new HashSet<Synchronizer<?, ?>>();
        Map<String, ExporterMessages> messages = new HashMap<String, ExporterMessages>();

        for (SynchronizedEntity e : SynchronizedEntity.values()) {
            Synchronizer<?, ?> synchronizer = e.getSynchronizer();
            synchronizer.initialize(subject, entityManager);
            allSynchronizers.add(synchronizer);
        }

        try {
            return new ExportWrapper(messages, new ExportingInputStream(allSynchronizers, messages));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize the export.", e);
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void importAllSubsystems(Subject subject, InputStream exportFile, List<ImportConfiguration> configurations)
        throws ValidationException, ImportException {

        File tmpFile = null;
        FileOutputStream tmpFileOut = null;
        try {
            tmpFile = File.createTempFile("rhq-synchronization", "tmp");
            tmpFileOut = new FileOutputStream(tmpFile);

            StreamUtil.copy(exportFile, tmpFileOut);

            tmpFileOut.close();
        } catch (IOException e) {
            throw new ImportException("Failed to copy the exportFile to a temporary location.", e);
        } finally {
            StreamUtil.safeClose(tmpFileOut);
        }

        InputStream in = null;
        try {
            in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(tmpFile)));
            validateExport(subject, in);
            in.close();

            in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(tmpFile)));            
            importExportFile(subject, in, getConfigPerImporter(configurations));
        } catch (XMLStreamException e) {
            throw new ImportException("Failed import due to XML parsing error.", e);
        } catch (IOException e) {
            throw new ImportException("The provided file is not a gzipped XML.", e);
        } finally {
            StreamUtil.safeClose(in);
            tmpFile.delete();
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void importAllSubsystems(Subject subject, byte[] exportFile, List<ImportConfiguration> configurations)
        throws ValidationException, ImportException {
        importAllSubsystems(subject, new ByteArrayInputStream(exportFile), configurations);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void validate(Subject subject, InputStream exportFile) throws ValidationException {
        try {
            validateExport(subject, exportFile);
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
    public ImportConfigurationDefinition getImportConfigurationDefinition(String synchronizerClass) {
        try {
            Class<?> cls = Class.forName(synchronizerClass);
            if (!Synchronizer.class.isAssignableFrom(cls)) {
                LOG.debug("Supplied synchronizer class does not implement the synchronizer interface: '" + synchronizerClass + "'.");
                return null;
            }

            Synchronizer<?, ?> syn = (Synchronizer<?, ?>) cls.newInstance();
            
            return new ImportConfigurationDefinition(synchronizerClass, syn.getImporter().getImportConfigurationDefinition());
        } catch (ClassNotFoundException e) {
            LOG.debug("Supplied synchronizer class is invalid: '" + synchronizerClass + "'.", e);
            return null;
        } catch (Exception e) {
            LOG.error("Failed to instantiate the synchronizer '" + synchronizerClass + "'. This should not happen.");
            throw new IllegalStateException("Failed to instantiate synchronizer '" + synchronizerClass + ".", e);
        }
    }

    @Override
    public List<ImportConfigurationDefinition> getImportConfigurationDefinitionOfAllSynchronizers() {
        List<ImportConfigurationDefinition> ret = new ArrayList<ImportConfigurationDefinition>();

        for (SynchronizedEntity e : SynchronizedEntity.values()) {
            Synchronizer<?, ?> syn = e.getSynchronizer();
            ret.add(new ImportConfigurationDefinition(syn.getClass().getName(), syn.getImporter()
                .getImportConfigurationDefinition()));
        }

        return ret;
    }

    private void validateExport(Subject subject, InputStream exportFile) throws ValidationException, XMLStreamException {
        XMLStreamReader rdr = XMLInputFactory.newInstance().createXMLStreamReader(exportFile);

        try {
            Set<ConsistencyValidatorFailureReport> failures = new HashSet<ConsistencyValidatorFailureReport>();
            Set<ConsistencyValidator> executedValidators = new HashSet<ConsistencyValidator>();

            while (rdr.hasNext()) {
                switch (rdr.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    String tagName = rdr.getName().getLocalPart();
                    if (ExportingInputStream.VALIDATOR_ELEMENT.equals(tagName)) {
                        ConsistencyValidator validator = null;
                        String validatorClass = rdr.getAttributeValue(null, ExportingInputStream.CLASS_ATTRIBUTE);
                        try {
                            validator = validateSingle(rdr, subject);
                        } catch (Exception e) {
                            failures.add(new ConsistencyValidatorFailureReport(validatorClass,
                                printExceptionToString(e)));
                        }
                        if (validator != null) {
                            executedValidators.add(validator);
                        }
                    } else if (ExportingInputStream.ENTITIES_EXPORT_ELEMENT.equals(tagName)) {
                        //all the validators preceed the importers in the export file so we can now check if the
                        //validator was already run
                        String synchronizerClass = rdr.getAttributeValue(null, ExportingInputStream.ID_ATTRIBUTE);
                        try {
                            failures.addAll(validateEntities(rdr, subject, executedValidators));
                        } catch (Exception e) {
                            throw new ValidationException(
                                "Validation failed unexpectedly while processing the entities exported by the synchronizer '"
                                    + synchronizerClass + "'.", e);
                        }
                    }
                }
            }

            if (!failures.isEmpty()) {
                throw new ValidationException(failures);
            }
        } finally {
            rdr.close();
        }
    }

    private Set<ConsistencyValidatorFailureReport> validateEntities(XMLStreamReader rdr, Subject subject,
        Set<ConsistencyValidator> executedValidators) throws Exception {
        String synchronizerClass = rdr.getAttributeValue(null, ExportingInputStream.ID_ATTRIBUTE);
        HashSet<ConsistencyValidatorFailureReport> ret = new HashSet<ConsistencyValidatorFailureReport>();

        Synchronizer<?, ?> synchronizer = instantiate(synchronizerClass, Synchronizer.class,
            "The id attribute of entities doesn't correspond to a class implementing the Synchronizer interface.");

        synchronizer.initialize(subject, entityManager);

        Importer<?, ?> importer = synchronizer.getImporter();

        //pre-init the validators so that we can reuse them in the loop below
        Set<ConsistencyValidator> validators = synchronizer.getRequiredValidators();
        for(ConsistencyValidator v : validators) {
            v.initialize(subject, entityManager);
            
            if (!executedValidators.contains(v)) {
                ret.add(new ConsistencyValidatorFailureReport(v.getClass().getName(), "The validator '"
                    + v.getClass().getName() + "' is required by the synchronizer '" + synchronizerClass
                    + "' but was not found in the export file."));
            }
        }
        
        //don't bother checking if there are inconsistencies in the export file
        if (!ret.isEmpty()) {
            return ret;
        }
        
        while (rdr.hasNext()) {
            boolean bailout = false;
            switch (rdr.next()) {
            case XMLStreamConstants.START_ELEMENT:
                if (ExportingInputStream.DATA_ELEMENT.equals(rdr.getName().getLocalPart())) {
                    rdr.nextTag();
                    Object exportedEntity = importer.unmarshallExportedEntity(new ExportReader(rdr));
                    Class<?> exportedEntityClass = exportedEntity.getClass();
                    
                    for (ConsistencyValidator validator : validators) {
                        boolean shouldCheck = false;
                        for (Class<?> cls : validator.getValidatedEntityTypes()) {
                            if (cls.isAssignableFrom(exportedEntityClass)) {
                                shouldCheck = true;
                                break;
                            }
                        }

                        if (!shouldCheck) {
                            //ok, this validator doesn't want to validate this type of entity
                            continue;
                        }

                        try {
                            validator.validateExportedEntity(exportedEntity);
                        } catch (Exception e) {
                            ValidationException v = new ValidationException("Failed to validate entity ["
                                + exportedEntity + "]", e);
                            ret.add(new ConsistencyValidatorFailureReport(validator.getClass().getName(),
                                printExceptionToString(v)));
                        }
                    }
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

        return ret;
    }

    private void importExportFile(Subject subject, InputStream exportFile,
        Map<String, Configuration> importerConfigs) throws ImportException, XMLStreamException {
        XMLStreamReader rdr = XMLInputFactory.newInstance().createXMLStreamReader(exportFile);

        while (rdr.hasNext()) {
            switch (rdr.next()) {
            case XMLStreamReader.START_ELEMENT:
                String tagName = rdr.getName().getLocalPart();
                if (ExportingInputStream.ENTITIES_EXPORT_ELEMENT.equals(tagName)) {
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

    private ConsistencyValidator validateSingle(XMLStreamReader rdr, Subject subject) throws InstantiationException,
        IllegalAccessException, ClassNotFoundException, XMLStreamException, InconsistentStateException {
        String validatorClassName = rdr.getAttributeValue(null, ExportingInputStream.CLASS_ATTRIBUTE);
        ConsistencyValidator validator = instantiate(
            validatorClassName,
            ConsistencyValidator.class,
            "The validator class denoted in the export file ('%s') does not implement the ConsistencyValidator interface. This should not happen.");

        //init the validator
        validator.initialize(subject, entityManager);

        //perform the validation
        validator.initializeExportedStateValidation(new ExportReader(rdr));
        validator.validateExportedState();

        return validator;
    }

    private <E, X> void importSingle(Subject subject, Map<String, Configuration> importConfigs, XMLStreamReader rdr)
        throws Exception {
        String synchronizerClassName = rdr.getAttributeValue(null, ExportingInputStream.ID_ATTRIBUTE);

        @SuppressWarnings("unchecked")
        Synchronizer<E, X> synchronizer = instantiate(synchronizerClassName, Synchronizer.class,
            "The synchronizer denoted in the export file ('%s') does not implement the importer interface. This should not happen.");

        synchronizer.initialize(subject, entityManager);
                
        Importer<E, X> importer = synchronizer.getImporter();
        importer.configure(importConfigs.get(synchronizerClassName));

        ExportedEntityMatcher<E, X> matcher = importer.getExportedEntityMatcher();

        while (rdr.hasNext()) {
            boolean bailout = false;
            switch (rdr.next()) {
            case XMLStreamConstants.START_ELEMENT:
                if (ExportingInputStream.DATA_ELEMENT.equals(rdr.getName().getLocalPart())) {
                    rdr.nextTag();
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

    private Map<String, Configuration> getConfigPerImporter(List<ImportConfiguration> list) {
        Map<String, Configuration> ret = new HashMap<String, Configuration>();

        if (list != null) {
            for (ImportConfiguration ic : list) {
                ret.put(ic.getSynchronizerClassName(), ic.getConfiguration());
            }
        }

        return ret;
    }
}
