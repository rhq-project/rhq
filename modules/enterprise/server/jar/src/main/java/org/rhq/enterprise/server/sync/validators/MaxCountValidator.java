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
 * 
 *
 * @author Lukas Krejci
 */
public class MaxCountValidator<T> implements EntityValidator<T> {

    private int max;
    private int cnt;
    
    public MaxCountValidator(int max) {
        this.max = max;
    }
    
    @Override
    public void initialize(Subject subject, EntityManager entityManager) {
        cnt = 0;
    }

    @Override
    public void validateExportedEntity(T entity) throws ValidationException {
        if (++cnt > max) {
            throw new ValidationException("Entity " + entity + " is unexpected in the export file. At most " + max + " entites of this type are expected.");
        }
    }
}
