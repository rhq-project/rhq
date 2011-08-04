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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;

/**
 * An implementation of this interface is able match instances of given type with some objects
 * in the inventory.
 * <p>
 * This is used during the configuration synchronization where different types of entities are 
 * being obtained from the export file and it is necessary to find matches of them in the database
 * that is being imported to.
 * <p>
 * There is a default set of matchers defined in the server for basic types of entitites like {@link Subject}s,
 * {@link Role}s, etc. but different export/import participants (like the {@link AlertSender alert senders}) can 
 * contribute new types of matchers for the data that they are exporting.
 * 
 * @author Lukas Krejci
 */
public interface ExportedEntityMatcher<Entity, ExportedType> {

    /**
     * Tries to find a match for given object in the data available in the database.
     * The returned object is not necessarily of the same type is the provided object
     * because the given object might function as some kind of "proxy" object that represents
     * the data better in the sync scenarios.
     * 
     * @param object
     * @return
     */
    Entity findMatch(ExportedType object);
}
