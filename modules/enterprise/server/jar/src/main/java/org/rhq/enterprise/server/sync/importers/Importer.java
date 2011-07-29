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

package org.rhq.enterprise.server.sync.importers;

import javax.persistence.EntityManager;
import javax.xml.stream.XMLStreamException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.server.sync.ExportReader;

/**
 * Implementations of this interface are used to import entities into the database.
 * <p>
 * The implementations MUST provide a no-arg public constructor.
 *
 * @author Lukas Krejci
 */
public interface Importer<Entity, ExportedType> {

    /**
     * A configuration definition describing the configuration of the importer and
     * the default values for individual properties.
     */
    ConfigurationDefinition getImportConfigurationDefinition();
    
    /**
     * Initializes the importer.
     * 
     * @param subject the current user
     * @param entityManager the entity manager the importer can use to persist entities
     * @param importConfiguration the configuration of the import as defined by the {@link #getImportConfigurationDefinition()}
     */    
    void init(Subject subject, EntityManager entityManager, Configuration importConfiguration);

    /**
     * Returns an entity matcher that can match the entities from the export file
     * with the real entities in the database.
     * 
     * @return
     */
    ExportedEntityMatcher<Entity, ExportedType> getExportedEntityMatcher();

    /**
     * Updates the entity with the data from the export.
     * <p>
     * This method is responsible for persisting the entity in the database
     * using the provided entityManager. Note that the actual persist can
     * also be delayed until the {@link #finishImport()} method is called
     * so that the importer can take advantage of batching.
     * 
     * @param entity the entity to persist (may be null if the {@link #getExportedEntityMatcher()} returned null of if the entity matcher didn't find a match)
     * @param exportedEntity the entity found in the export file that should be used to update the entity in the database
     */
    void update(Entity entity, ExportedType exportedEntity) throws Exception;
    
    /**
     * Unmarshalls an entity from the provided reader.
     * 
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    ExportedType unmarshallExportedEntity(ExportReader reader) throws XMLStreamException;
    
    /**
     * Finishes the import. This method is called after all entities from the export file
     * have been {@link #update(Object, Object) updated}.
     * <p>
     * This is useful for importers that need to batch the updates to the database.
     */
    void finishImport() throws Exception;
}
