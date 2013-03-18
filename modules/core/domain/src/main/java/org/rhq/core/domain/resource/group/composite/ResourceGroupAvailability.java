/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite.GroupAvailabilityType;

/**
 * Each instance represents an availability interval for a group. Group availability is determined by looking at
 * the resource availabilities of the group members for the period of time in question.  The group availability is
 * determined in the following way, processed top to bottom:
 * <pre>
 * Member Availability |  Group Availability
 *   Empty Group       |    Grey / EMPTY
 *   All DOWN          |    Red / DOWN
 *   Some DOWN/UNKNOWN |    Yellow / WARN
 *   Some DISABLED     |    Orange / DISABLED
 *   All UP            |    Green / UP
 * </pre>
 * 
 * @author Jay Shaughnessy
 */
public class ResourceGroupAvailability implements Serializable {

    private static final long serialVersionUID = 1L;

    private int resourceGroupId;
    private GroupAvailabilityType groupAvailabilityType;
    private Long startTime;
    private Long endTime;

    /**
     *  for serialization purposes only, do not use this.
     */
    public ResourceGroupAvailability() {
    }

    public ResourceGroupAvailability(int resourceGroupId) {
        this(resourceGroupId, null, null, null);
    }

    public ResourceGroupAvailability(int resourceGroupId, GroupAvailabilityType groupAvailabilityType, Long startTime,
        Long endTime) {
        super();
        this.resourceGroupId = resourceGroupId;
        this.groupAvailabilityType = groupAvailabilityType;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getResourceGroupId() {
        return resourceGroupId;
    }

    public GroupAvailabilityType getGroupAvailabilityType() {
        return groupAvailabilityType;
    }

    public void setGroupAvailabilityType(GroupAvailabilityType groupAvailabilityType) {
        this.groupAvailabilityType = groupAvailabilityType;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

}