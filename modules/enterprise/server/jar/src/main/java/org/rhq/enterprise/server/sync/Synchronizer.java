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

import java.util.Set;

import javax.persistence.EntityManager;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;

/**
 *  Represents a synchronizer that can export and import entities.
 *
 * @param <Entity> the domain model entity being synchronized
 * @param <ExportedEntity> the type that is used to represent the entity in the export file
 * 
 * @author Lukas Krejci
 */
public interface Synchronizer<Entity, ExportedEntity> {

    /**
     * Initializes the synchronizer so that it can access database
     * and is authorization aware.
     * 
     * @param subject the currently logged in user
     * @param entityManager the entity manager to access the database with (if not using one
     * of the RHQ's SLSBs)
     */
    void initialize(Subject subject, EntityManager entityManager);
    
    /**
     * The exporter to use to export the entities.
     */
    Exporter<Entity, ExportedEntity> getExporter();

    /**
     * The importer to import the entities with.
     */
    Importer<Entity, ExportedEntity> getImporter();
    
    /**
     * The set of validators that are required to validate the 
     * state of the export or the current installation during import.
     * <p>
     * The validators need not to be {@link ConsistencyValidator#initialize(Subject, EntityManager) initialized}.
     */
    Set<ConsistencyValidator> getRequiredValidators();
}
