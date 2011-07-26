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

import org.rhq.enterprise.server.sync.NoSingleEntity;

/**
 * This is a simple entity matcher that can be used by importers that do not need
 * to find any matching entity in the database.
 *
 * @author Lukas Krejci
 */
public class NoSingleEntityMatcher<T> implements ExportedEntityMatcher<NoSingleEntity, T> {

    private static final NoSingleEntity ENTITY = new NoSingleEntity();
    
    @Override
    public NoSingleEntity findMatch(T object) {
        return ENTITY;
    }
}
