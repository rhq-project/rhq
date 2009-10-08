/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pc.content;

import java.util.Date;

import org.rhq.core.domain.content.PackageType;

/**
 * Describes one instance of a content discovery for a particular resource and a particular package type.
 *
 * @author Jason Dobies
 */
public class ScheduledContentDiscoveryInfo implements Comparable<ScheduledContentDiscoveryInfo> {
    // Attributes  --------------------------------------------

    private int resourceId;
    private PackageType packageType;
    private long interval;
    private long lastDiscovery;
    private long nextDiscovery;

    // Constructors  --------------------------------------------

    /**
     * Creates a new set of data to drive a content discovery.
     *
     * @param resourceId  resource against which to discover; must be a valid resource in the inventory
     * @param packageType type of content being discovered; cannot be <code>null</code>
     */
    public ScheduledContentDiscoveryInfo(int resourceId, PackageType packageType) {
        this.resourceId = resourceId;
        this.packageType = packageType;
        this.interval = packageType.getDiscoveryInterval();
    }

    // Accessors  --------------------------------------------

    public int getResourceId() {
        return resourceId;
    }

    public PackageType getPackageType() {
        return packageType;
    }

    public long getInterval() {
        return interval;
    }

    public long getNextDiscovery() {
        return nextDiscovery;
    }

    public void setNextDiscovery(long nextDiscovery) {
        this.nextDiscovery = nextDiscovery;
    }

    public long getLastDiscovery() {
        return lastDiscovery;
    }

    public void setLastDiscovery(long lastDiscovery) {
        this.lastDiscovery = lastDiscovery;
    }

    // Comparable Implementation  --------------------------------------------

    public int compareTo(ScheduledContentDiscoveryInfo other) {
        // Order by next discovery time first, then by resource ID

        int n = (nextDiscovery < other.getNextDiscovery()) ? -1 : ((nextDiscovery == other.getNextDiscovery()) ? 0 : 1);
        if (n != 0) {
            return n;
        }

        n = (resourceId < other.getResourceId()) ? -1 : ((resourceId == other.getResourceId()) ? 0 : 1);

        return n;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ScheduledContentDiscoveryInfo:");
        str.append("resource-id=[").append(this.resourceId);
        str.append("], package-type=[").append(this.packageType.getName());
        str.append("], interval=[").append(this.interval);
        str.append("], next-discovery=[").append(new Date(this.nextDiscovery));
        return str.toString();
    }
}