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

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.sync.ExportReport;
import org.rhq.core.domain.sync.ExportWrapper;
import org.rhq.core.domain.sync.ImportConfiguration;
import org.rhq.core.domain.sync.ImportConfigurationDefinition;
import org.rhq.core.domain.sync.ImportReport;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Local
public interface SynchronizationManagerLocal {

    /**
     * This method returns an export wrapper that contains the structure to contain
     * the future messages from the export process and an input stream that is going
     * to create the export (and fill in the messages) while it is being read.
     * <p>
     * This minimizes the memory needed to hold the export data to just 64K (the size of the buffer
     * to hold the data). The memory consumption will of course be also determined by the amount of
     * data being read from the database but the point is that not all the data will be held in memory
     * (because the exporters for various subsystems will be called in sequence) and that the data doesn't
     * have to reside in the memory in two forms - the internal datastructures AND the serialized form
     * of the export file that it is going to be transfered as.
     * 
     * @param subject the logged in user that has {@link Permission#MANAGE_INVENTORY} permission
     * 
     * @return a wrapper using which one can read the export file "lazily".
     */
    ExportWrapper exportAllSubsystemsLocally(Subject subject);
    
    void validate(Subject subject, InputStream exportFile) throws ValidationException;
    
    ImportReport importAllSubsystems(Subject subject, InputStream exportFile, List<ImportConfiguration> importerConfigurations) throws ValidationException, ImportException;
    
    /**
     * <b>Provided for testability reasons. DON'T USE OUTSIDE TESTS.</b> 
     * <p>
     * Using this method one can provide a custom factory
     * for {@link Synchronizer}s. This way one can export/import different types of entities
     * than by default.
     * 
     * @param factory
     */
    void setSynchronizerFactory(SynchronizerFactory factory);
    
    //-------- THE FOLLOWING METHODS ARE SHARED WITH THE REMOTE INTERFACE ------------
    
    /**
     * Don't use this method if you access it from the same JVM.
     * The {@link #exportAllSubsystemsLocally()} is more memory efficient.
     * <p>
     * This method executes the export of all subsystems and serializes the data
     * into an byte array.
     * 
     * @see SynchronizationManagerRemote#exportAllSubsystems()
     */
    ExportReport exportAllSubsystems(Subject subject);

    /**
     * @see SynchronizationManagerRemote#validate(Subject, byte[]) 
     */
    void validate(Subject subject, byte[] exportFile) throws ValidationException;

    /**
     * @see SynchronizationManagerRemote#getImportConfigurationDefinition(String) 
     */
    ImportConfigurationDefinition getImportConfigurationDefinition(Subject subject, String synchronizerClass);
    
    /**
     * @see SynchronizationManagerRemote#getImportConfigurationDefinitionOfAllSynchronizers()
     */
    List<ImportConfigurationDefinition> getImportConfigurationDefinitionOfAllSynchronizers(Subject subject);
    
    /**
     * @see SynchronizationManagerRemote#importAllSubsystems(Subject, byte[], Set)
     */
    ImportReport importAllSubsystems(Subject subject, byte[] exportFile, List<ImportConfiguration> importerConfigurations) throws ValidationException, ImportException;
}
