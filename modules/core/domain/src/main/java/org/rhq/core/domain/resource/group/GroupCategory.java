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
package org.rhq.core.domain.resource.group;

/**
 * A JON group category.
 */
public enum GroupCategory {
    COMPATIBLE("compatible group"), MIXED("mixed group");

    private final String displayName;

    GroupCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * A Java bean style getter to allow us to access the enum name from JSPs (e.g. ${groupCategory.name}).
     *
     * @return the enum name
     */
    public String getName() {
        return name();
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}