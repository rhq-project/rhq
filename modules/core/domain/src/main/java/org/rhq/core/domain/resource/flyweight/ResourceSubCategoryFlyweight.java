/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.domain.resource.flyweight;

import java.io.Serializable;

/**
 * Flyweight representation of a resource sub category. Only id, name and parent are preserved.
 *
 * @author Lukas Krejci
 */
@Deprecated
public class ResourceSubCategoryFlyweight implements Serializable {

    private static final long serialVersionUID = 2L;

    @Deprecated
    public ResourceSubCategoryFlyweight() {
    }

    @Deprecated
    public int getId() {
        return 0;
    }

    @Deprecated
    public void setId(int id) {
    }

    @Deprecated
    public String getName() {
        return null;
    }

    @Deprecated
    public void setName(String name) {
    }

    @Deprecated
    public ResourceSubCategoryFlyweight getParentSubCategory() {
        return null;
    }

    @Deprecated
    public void setParentSubCategory(ResourceSubCategoryFlyweight parentSubCategory) {
    }

    @Override
    public boolean equals(Object o) {
        return true;
    }
}
