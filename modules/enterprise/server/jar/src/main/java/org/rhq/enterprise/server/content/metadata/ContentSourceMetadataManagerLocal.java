/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.content.metadata;

import java.util.Set;
import javax.ejb.Local;
import org.rhq.core.domain.content.ContentSourceType;

@Local
public interface ContentSourceMetadataManagerLocal {
    /**
     * Registers content source types. This will updates all existing content source type definitions so they match with
     * the given types passed in. If any types used to exist but no longer do (and have no foreign key relationships
     * with other existing objects), they will be removed.
     *
     * @param typesToRegister the types that our content source subsystem should have
     */
    void registerTypes(Set<ContentSourceType> typesToRegister);
}