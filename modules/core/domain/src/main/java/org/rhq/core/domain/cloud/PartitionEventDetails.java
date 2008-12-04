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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Agent;

/**
 * An object to capture when the infrastructure used for high availability
 * reconfigures itself for some reason.  This object will store a snapshot
 * of the topology, so you can go back in history and figure out which 
 * servers were connected to which agents at any given point in time.
 * 
 * @author jmarques
 *
 */
@Entity(name = "PartitionEventDetails")
@NamedQueries //
( { @NamedQuery(name = PartitionEventDetails.QUERY_FIND_BY_EVENT_ID, query = "" //
    + "SELECT ped " //
    + "  FROM PartitionEventDetails ped " //
    + " WHERE ped.partitionEvent.id = :eventId ") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_PARTITION_DETAILS_ID_SEQ")
@Table(name = "RHQ_PARTITION_DETAILS")
public class PartitionEventDetails implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_EVENT_ID = "PartitionEventDetails.findByEventId";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @JoinColumn(name = "PARTITION_EVENT_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    protected PartitionEvent partitionEvent;

    @Column(name = "AGENT_NAME", nullable = false)
    protected String agentName;

    @Column(name = "SERVER_NAME", nullable = false)
    protected String serverName;

    // required for JPA
    protected PartitionEventDetails() {
    }

    public PartitionEventDetails(PartitionEvent partitionEvent, Agent agent, Server server) {
        this.partitionEvent = partitionEvent;
        this.agentName = agent.getName();
        this.serverName = server.getName();
    }

    public PartitionEvent getPartitionEvent() {
        return partitionEvent;
    }

    public void setPartitionEvent(PartitionEvent partitionEvent) {
        this.partitionEvent = partitionEvent;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agentName == null) ? 0 : agentName.hashCode());
        result = prime * result + ((partitionEvent == null) ? 0 : partitionEvent.hashCode());
        result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PartitionEventDetails)) {
            return false;
        }

        final PartitionEventDetails other = (PartitionEventDetails) obj;

        if (agentName == null) {
            if (other.agentName != null) {
                return false;
            }
        } else if (!agentName.equals(other.agentName)) {
            return false;
        }

        if (serverName == null) {
            if (other.serverName != null) {
                return false;
            }
        } else if (!serverName.equals(other.serverName)) {
            return false;
        }

        if (partitionEvent == null) {
            if (other.partitionEvent != null) {
                return false;
            }
        } else if (!partitionEvent.equals(other.partitionEvent)) {
            return false;
        }

        return true;
    }

}
