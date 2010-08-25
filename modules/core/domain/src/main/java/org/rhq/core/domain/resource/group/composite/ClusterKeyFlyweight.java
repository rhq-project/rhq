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
package org.rhq.core.domain.resource.group.composite;

import java.io.Serializable;

/**
 * @author Greg Hinkle
 */
public class ClusterKeyFlyweight implements Serializable {

    private int resourceTypeId;
    private String resourceKey;

    public ClusterKeyFlyweight() {
    }

    public ClusterKeyFlyweight(int resourceTypeId, String resourceKey) {
        this.resourceTypeId = resourceTypeId;
        this.resourceKey = resourceKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClusterKeyFlyweight node = (ClusterKeyFlyweight) o;

        if (resourceTypeId != node.resourceTypeId) return false;
        if (resourceKey != null ? !resourceKey.equals(node.resourceKey) : node.resourceKey != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = resourceTypeId;
        result = 31 * result + (resourceKey != null ? resourceKey.hashCode() : 0);
        return result;
    }

    public int getResourceTypeId() {
        return resourceTypeId;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    @Override
    public String toString() {
        return "ClusterKeyFlyweight{" +
                "resourceTypeId=" + resourceTypeId +
                ", resourceKey='" + resourceKey + '\'' +
                '}';
    }
}