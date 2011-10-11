/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

/**
 * To support pluggable drift server implementations this Interface provides the contract required for
 * defining and persisting each Drift entry in a DriftChangeSet.  
 *
 * @param <C> A server plugin's DriftChangeSet implementation
 * @param <F> A server plugin's DriftFile implementation
 *  
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
public interface Drift<C extends DriftChangeSet<?>, F extends DriftFile> {
    String getId();

    void setId(String id);

    Long getCtime();

    C getChangeSet();

    void setChangeSet(C changeSet);

    DriftCategory getCategory();

    void setCategory(DriftCategory category);

    String getPath();

    void setPath(String path);

    String getDirectory();

    void setDirectory(String directory);

    F getOldDriftFile();

    void setOldDriftFile(F oldDriftFile);

    F getNewDriftFile();

    void setNewDriftFile(F newDriftFile);
}
