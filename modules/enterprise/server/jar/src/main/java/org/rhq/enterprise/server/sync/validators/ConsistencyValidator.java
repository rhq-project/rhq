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

package org.rhq.enterprise.server.sync.validators;

import java.util.Set;

import javax.persistence.EntityManager;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.sync.ExportWriter;
import org.rhq.enterprise.server.sync.ValidationException;

/**
 * Implementations of this interface can export a state and validate that
 * the exported state is consistent with the environment the export is being
 * imported to.
 * <p>
 * Think about the necessity of the two RHQ installations having the same plugins
 * deployed so that the metric templates can be fully matched between them. 
 * <p>
 * The validators are required to have a public no-arg constructor. If some configuration
 * settings are required to be transfered over to the target installation, the implementation
 * is free to include such data in the XML during the {@link #exportState(ExportWriter)} method.
 * 
 * @author Lukas Krejci
 */
public interface ConsistencyValidator {

    /**
     * Initializes the validator with the current authentication info and access to database.
     * This method is only called during import.
     * 
     * @param subject the currently authenticated user
     * @param entityManager the entity manager that can be used to access the database if the 
     * validator needs to do so.
     */
    void initialize(Subject subject, EntityManager entityManager);
    
    /**
     * Exports the state this checker needs validated later on.
     * This method is being called during the export to capture that
     * aspects of system that need to be satisfied on the target
     * system.
     * 
     * @param writer
     * @throws XMLStreamException
     */
    void exportState(ExportWriter writer) throws XMLStreamException;
    
    /**
     * This method initializes the consistency checker to perform the
     * {@link #validateExportedState()} method later on.
     * <p>
     * This method is called during import and the reader points to a structure
     * previously stored by the {@link #exportState(ExportWriter)} method on 
     * the source RHQ installation.
     * 
     * @param reader
     * @throws XMLStreamException
     */
    void initializeExportedStateValidation(ExportReader reader) throws XMLStreamException;
    
    /**
     * Validates that the current RHQ installation is consistent with the state
     * mandated during the export.
     * <p>
     * This method is only ever called after the {@link #initializeExportedStateValidation(XMLStreamReader)} 
     * is invoked.
     * 
     * @throws InconsistentStateException in case of failed consistency check
     */
    void validateExportedState() throws InconsistentStateException;
    
    /**
     * Returns the types of exported entities this validator can validate before export.
     * This set can be empty, which would mean it can only validate the current state.
     */
    Set<Class<?>> getValidatedEntityTypes();
    
    /**
     * Validates a single entity before it is imported.
     * The supplied entity has one of the types returned by {@link #getValidatedEntityTypes()}.
     * 
     * @param entity
     */
    void validateExportedEntity(Object entity) throws ValidationException;
    
    /**
     * Implementations of the consistency validator interface should reimplement the
     * {@link Object#equals(Object)} method so that two identically configured 
     * validators are equal to each other.
     * 
     * @param other
     * @return
     */
    boolean equals(Object other);
    
    /**
     * The {@link ConsistencyValidator} interface mandates a certain behavior of
     * the {@link #equals(Object)} method and therefore the <code>hashCode</code>
     * should be reimplemented accordingly.
     *  
     * @return
     */
    int hashCode();
}
