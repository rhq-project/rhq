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

import org.rhq.core.domain.auth.Subject;

/**
 * Implementations of this interface are used to import entities into the database.
 * <p>
 * The implementations MUST provide a no-arg public constructor.
 *
 * @author Lukas Krejci
 */
public interface Importer<Entity, ExportedType> {

    /**
     * Initializes the importer.
     * 
     * @param subject the current user
     */
    void init(Subject subject);

    /**
     * Returns an entity matcher that can match the entities from the export file
     * with the real entities in the database.
     *  
     * @param entityManager the entity manager the matcher can use to find the entities in the database.
     * 
     * @return
     */
    ExportedEntityMatcher<Entity, ExportedType> getExportedEntityMatcher(EntityManager entityManager);

    /**
     * Updates the entity with the data from the export.
     * <p>
     * This method is responsible for persisting the entity in the database
     * using the provided entityManager.
     * 
     * @param entity the entity to persist (may be null if the {@link #getExportedEntityMatcher(EntityManager)} returned null of if the entity matcher didn't find a match)
     * @param exportedEntity the entity found in the export file that should be used to update the entity in the database
     * @param entityManager the entity manager to use to persist the entity to the database
     */
    void update(Entity entity, ExportedType exportedEntity, EntityManager entityManager);
}
