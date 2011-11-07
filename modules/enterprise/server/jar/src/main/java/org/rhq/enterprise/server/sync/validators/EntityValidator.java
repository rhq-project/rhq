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

import javax.persistence.EntityManager;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.sync.ValidationException;

/**
 * Entity validators are declared by importers to validate the entities in the export
 * file before it gets imported.
 *
 * @author Lukas Krejci
 */
public interface EntityValidator<T> {

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
     * Validates a single entity before it is imported.
     * The supplied entity has one of the types returned by {@link #getValidatedEntityTypes()}.
     * 
     * @param entity
     */
    void validateExportedEntity(T entity) throws ValidationException;
}
