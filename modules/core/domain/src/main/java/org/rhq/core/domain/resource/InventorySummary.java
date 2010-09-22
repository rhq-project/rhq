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
package org.rhq.core.domain.resource;

import java.io.Serializable;

/**
 * A summary of inventoried {@link org.rhq.core.domain.resource.Resource}s and
 * {@link org.rhq.core.domain.resource.group.Group}s that are viewable by the specified user. The summary includes the
 * total number of platforms, servers, services, {@link org.rhq.core.domain.resource.group.CompatibleGroup}s, and
 * {@link org.rhq.core.domain.resource.group.MixedGroup}s. Only Resources with an inventory status of COMMITTED are
 * tallied.
 *
 * @author Ian Springer
 */
public class InventorySummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private int platformCount;
    private int serverCount;
    private int serviceCount;
    private int compatibleGroupCount;
    private int mixedGroupCount;
    private int groupDefinitionCount;
    private int softwareProductCount;
    private int softwareUpdateCount;
    private int scheduledMeasurementsPerMinute;

    public int getPlatformCount() {
        return platformCount;
    }

    public void setPlatformCount(int platformCount) {
        this.platformCount = platformCount;
    }

    public int getServerCount() {
        return serverCount;
    }

    public void setServerCount(int serverCount) {
        this.serverCount = serverCount;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public int getCompatibleGroupCount() {
        return compatibleGroupCount;
    }

    public void setCompatibleGroupCount(int compatibleGroupCount) {
        this.compatibleGroupCount = compatibleGroupCount;
    }

    public int getMixedGroupCount() {
        return mixedGroupCount;
    }

    public void setMixedGroupCount(int mixedGroupCount) {
        this.mixedGroupCount = mixedGroupCount;
    }

    public int getSoftwareProductCount() {
        return softwareProductCount;
    }

    public void setSoftwareProductCount(int softwareProductCount) {
        this.softwareProductCount = softwareProductCount;
    }

    public int getSoftwareUpdateCount() {
        return softwareUpdateCount;
    }

    public void setSoftwareUpdateCount(int softwareUpdateCount) {
        this.softwareUpdateCount = softwareUpdateCount;
    }

    public int getScheduledMeasurementsPerMinute() {
        return scheduledMeasurementsPerMinute;
    }

    public void setScheduledMeasurementsPerMinute(int scheduledMeasurementsPerMinute) {
        this.scheduledMeasurementsPerMinute = scheduledMeasurementsPerMinute;
    }

    public int getGroupDefinitionCount() {
        return groupDefinitionCount;
    }

    public void setGroupDefinitionCount(int groupDefinitionCount) {
        this.groupDefinitionCount = groupDefinitionCount;
    }
}