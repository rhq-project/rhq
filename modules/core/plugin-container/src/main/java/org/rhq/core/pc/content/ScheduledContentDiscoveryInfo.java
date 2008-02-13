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
package org.rhq.core.pc.content;

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
        return "ScheduledContentDiscoveryInfo[ResourceId=" + this.resourceId + ", PackageType="
            + this.packageType.getName() + ", Interval=" + interval + ", NextDiscovery=" + nextDiscovery + ", Now="
            + System.currentTimeMillis() + "]";
    }
}