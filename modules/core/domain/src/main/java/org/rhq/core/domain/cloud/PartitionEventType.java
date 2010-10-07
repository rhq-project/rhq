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
package org.rhq.core.domain.cloud;

/**
 * @author Jay Shaughnessy
 * @author Joseph Marques
 */
public enum PartitionEventType {

    AGENT_REGISTRATION(false), // Agent registers with server defined in setup, or previously connected server from server list. 
    AGENT_CONNECT(false), // Agent connects to server on server list
    AGENT_SHUTDOWN(false), // Agent notifies server of agent shutdown
    AGENT_LEAVE(false), // Not currently used - should this be worked into suspect agent logic?

    SERVER_DELETION(true), //
    SERVER_COMPUTE_POWER_CHANGE(true), // Not yet implemented
    OPERATION_MODE_CHANGE(true), //

    AGENT_AFFINITY_GROUP_ASSIGN(true), // An agent was assigned to an affinity group
    AGENT_AFFINITY_GROUP_REMOVE(true), // An agent was set to have no affinity group
    SERVER_AFFINITY_GROUP_ASSIGN(true), // A server was assigned to an affinity group
    SERVER_AFFINITY_GROUP_REMOVE(true), // A server was set to have no affinity group
    AFFINITY_GROUP_CHANGE(true), // Some affinity group change has happened (one or more of ASSIGN/REMOVE) 
    AFFINITY_GROUP_DELETE(true), // An affinity group has been removed with, most likely associated agent/server removals 

    ADMIN_INITIATED_PARTITION(true), // Admin requested via HAAC
    SYSTEM_INITIATED_PARTITION(true); // Load imbalance (maybe should be an explicit type for each system initiated type)

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
