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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
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
@NamedQueries ({
    @NamedQuery(name = PartitionEventDetails.QUERY_FIND_BY_EVENT_ID, query = "" //
        + "SELECT ped FROM PartitionEventDetails ped " //
        + "JOIN FETCH ped.agent JOIN FETCH ped.server " //
        + "WHERE ped.partitionEvent.id = :eventId "),
    @NamedQuery(name = PartitionEventDetails.QUERY_COUNT_BY_EVENT_ID, query = "" //
        + "SELECT ped FROM PartitionEventDetails ped " //
        + "WHERE ped.partitionEvent.id = :eventId ")
    })
@SequenceGenerator(name = "id", sequenceName = "RHQ_PARTITION_DETAILS_ID_SEQ")
@Table(name = "RHQ_PARTITION_DETAILS")
public class PartitionEventDetails implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_EVENT_ID = "PartitionEventDetails.findByEventId";
    public static final String QUERY_COUNT_BY_EVENT_ID = "PartitionEventDetails.countByEventId";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @JoinColumn(name = "PARTITION_EVENT_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    protected PartitionEvent partitionEvent;

    @JoinColumn(name = "AGENT_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    protected Agent agent;

    @Column(name = "AGENT_ID", insertable = false, updatable = false)
    private int agentId;

    @JoinColumn(name = "SERVER_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    protected Server server;

    @Column(name = "SERVER_ID", insertable = false, updatable = false)
    private int serverId;

    // required for JPA
    protected PartitionEventDetails() {
    }

    public PartitionEventDetails(PartitionEvent partitionEvent, Agent agent, Server server) {
        this.partitionEvent = partitionEvent;
        this.agent = agent;
        this.server = server;
        this.agentId = agent.getId();
        this.serverId = server.getId();
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

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + agentId;
        result = prime * result + ((partitionEvent == null) ? 0 : partitionEvent.hashCode());
        result = prime * result + serverId;
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

        if (agentId != other.agentId || serverId != other.serverId) {
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
