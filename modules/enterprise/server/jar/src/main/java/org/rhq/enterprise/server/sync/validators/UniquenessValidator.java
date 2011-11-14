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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.sync.ValidationException;

/**
 * This validator merely checks that there are no duplicates in the export file.
 *
 * @author Lukas Krejci
 */
public class UniquenessValidator<T> implements EntityValidator<T> {
    
    private Set<T> alreadyCheckedEntities;
    
    @Override
    public void initialize(Subject subject, EntityManager entityManager) {
        alreadyCheckedEntities = new HashSet<T>();
    }

    @Override
    public void validateExportedEntity(T entity) throws ValidationException {
        if (!alreadyCheckedEntities.add(entity)) {
            throw new ValidationException("Was the export file manually updated? The entity " + entity + " has been seen multiple times.");
        }
    }
}
