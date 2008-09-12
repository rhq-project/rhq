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
package org.rhq.core.domain.cluster;

/**
 * @author Joseph Marques
 */
public enum PartitionEventType {

    AGENT_REGISTRATION(false), //
    AGENT_JOIN(false), //
    AGENT_LEAVE(false), //

    SERVER_JOIN(true), // Server becomes available, either newly created or restarted in non-maintenance mode
    SERVER_DOWN(true), // Server crashes, is shut down normally
    SERVER_DELETION(true), //
    SERVER_COMPUTE_POWER_CHANGE(true), //

    AFFINITY_GROUP_CHANGE(true), //

    USER_INITIATED_PARTITION(true), // Admin requested via HAAC
    SYSTEM_INITIATED_PARTITION(true), // Load imbalance (maybe should be an explicit type for each system initiated type)

    MAINTENANCE_MODE_AGENT(false), // future
    MAINTENANCE_MODE_SERVER(true); // 

    private final boolean cloudPartitionEvent;

    PartitionEventType(boolean cloudPartitionEvent) {
        this.cloudPartitionEvent = cloudPartitionEvent;
    }

    /** 
     * @return true if this event type forces a partition of all agents. false if this is a single agent event.
     */
    public boolean isCloudPartitionEvent() {
        return cloudPartitionEvent;
    }

}
