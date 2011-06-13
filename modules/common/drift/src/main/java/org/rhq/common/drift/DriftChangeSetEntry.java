/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.common.drift;

import org.rhq.core.domain.drift.DriftCategory;

/**
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class DriftChangeSetEntry {

    private String path;
    private Integer size = 0;
    private DriftCategory category;
    // required for FILE_CHANGED and FILE_REMOVED, null for FILE_ADDED
    private String oldSha256;
    // required for FILE_CHANGED and FILE_ADDED, null for FILE_REMOVED    
    private String newSha256;

    public DriftChangeSetEntry(Integer size, String path, DriftCategory category, String oldSha256, String newSha256) {
        super();
        this.size = size;
        this.path = path;
        this.category = category;
        this.oldSha256 = oldSha256;
        this.newSha256 = newSha256;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public DriftCategory getCategory() {
        return category;
    }

    public void setCategory(DriftCategory category) {
        this.category = category;
    }

    public String getOldSha256() {
        return oldSha256;
    }

    public void setOldSha256(String oldSha256) {
        this.oldSha256 = oldSha256;
    }

    public String getNewSha256() {
        return newSha256;
    }

    public void setNewSha256(String newSha256) {
        this.newSha256 = newSha256;
    }

    @Override
    public String toString() {
        return "DriftChangeSetEntry [category=" + category + ", newSha256=" + newSha256 + ", oldSha256=" + oldSha256
            + ", path=" + path + "]";
    }

}
