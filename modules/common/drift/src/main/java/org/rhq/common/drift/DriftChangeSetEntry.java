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

    private String sha256;
    private String path;
    private Integer size = 0;
    private DriftCategory category;

    public DriftChangeSetEntry(Integer size, String path, DriftCategory category, String sha256) {
        super();
        this.size = size;
        this.path = path;
        this.category = category;
        this.sha256 = sha256;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
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

    @Override
    public String toString() {
        return "DriftChangeSetEntry [category=" + category + ", path=" + path + ", sha256=" + sha256 + "]";
    }

}
