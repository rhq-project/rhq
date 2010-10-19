/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.jpa;

import org.rhq.helpers.perftest.support.dbunit.EntityRelationshipFilter;

/**
 * Implementations of this interface can tell the EntityRelationshipFilter
 * whether to traverse the entity dependency graph down a particular relationship.
 * 
 * This is used in the {@link EntityRelationshipFilter} when determining the tables
 * to include in the output.
 * 
 * @author Lukas Krejci
 */
public interface DependencyInclusionResolver {

    /**
     * @param edge
     * @return true if the edge should be traversed, false otherwise
     */
    boolean isValid(Edge edge);
}
