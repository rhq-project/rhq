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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.cluster.composite.FailoverListComposite.ServerEntry;
import org.rhq.core.domain.resource.Agent;

/**
 * An RHQ server node in the cluster
 * 
 * @author Joseph Marques
 */
@Entity(name = "Server")
@NamedQueries( //
{
    @NamedQuery(name = Server.QUERY_FIND_ALL, query = "SELECT s FROM Server s"),
    @NamedQuery(name = Server.QUERY_FIND_ALL_COMPOSITES, query = "" //
        + "SELECT NEW org.rhq.core.domain.cluster.composite.ServerWithAgentCountComposite" //
        + "     ( " //
        + "       s, " //
        + "       (SELECT COUNT(a) FROM Agent a WHERE a.server = s) " //
        + "     ) " //
        + "  FROM Server s"),
    @NamedQuery(name = Server.QUERY_FIND_BY_NAME, query = "SELECT s FROM Server s WHERE s.name = :name"),
    @NamedQuery(name = Server.QUERY_FIND_BY_OPERATION_MODE, query = "SELECT s FROM Server s WHERE s.operationMode = :mode"),
    @NamedQuery(name = Server.QUERY_FIND_BY_AFFINITY_GROUP, query = "" //
        + "SELECT s " //
        + "  FROM Server s " //
        + " WHERE s.affinityGroup.id = :affinityGroupId "),
    @NamedQuery(name = Server.QUERY_FIND_WITHOUT_AFFINITY_GROUP, query = "" //
        + "SELECT s " //
        + "  FROM Server s " //
        + " WHERE s.affinityGroup IS NULL ") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_SERVER_ID_SEQ")
@Table(name = "RHQ_SERVER")
public class Server implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "Server.findAll";
    public static final String QUERY_FIND_ALL_COMPOSITES = "Server.findAllComposites";
    public static final String QUERY_FIND_BY_NAME = "Server.findByName";
    public static final String QUERY_FIND_BY_OPERATION_MODE = "Server.findByOperationMode";
    public static final String QUERY_FIND_BY_AFFINITY_GROUP = "Server.findByAffinityGroup";
    public static final String QUERY_FIND_WITHOUT_AFFINITY_GROUP = "Server.findWithoutAffinityGroup";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "ADDRESS", nullable = false)
    private String address;

    @Column(name = "PORT", nullable = false)
    private int port;

    @Column(name = "SECURE_PORT", nullable = false)
    private int securePort;

    @Column(name = "OPERATION_MODE", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationMode operationMode;

    @Column(name = "COMPUTE_POWER", nullable = false)
    private int computePower;

    // the time this server node was installed into the infrastructure
    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @JoinColumn(name = "AFFINITY_GROUP_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private AffinityGroup affinityGroup;

    @OneToMany(mappedBy = "server", fetch = FetchType.LAZY)
    private List<Agent> agents = new ArrayList<Agent>();

    // required for JPA
    public Server() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getSecurePort() {
        return securePort;
    }

    public void setSecurePort(int securePort) {
        this.securePort = securePort;
    }

    public int getComputePower() {
        return computePower;
    }

    public void setComputePower(int computePower) {
        this.computePower = computePower;
    }

    public long getCtime() {
        return ctime;
    }

    public AffinityGroup getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(AffinityGroup affinityGroup) {
        this.affinityGroup = affinityGroup;
    }

    public OperationMode getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(OperationMode operationMode) {
        this.operationMode = operationMode;
    }

    public enum OperationMode {

        DOWN("This server is down member of the HA server cloud"), //
        MAINTENANCE("This server is a Maintenance Mode member of the HA server cloud"), //
        NORMAL("This server is a Normal Member of the HA server cloud");

        public final String message;

        private OperationMode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    public int getAgentCount() {
        return this.agents.size();
    }

    public ServerEntry getServerEntry() {
        return new FailoverListComposite.ServerEntry(address, port, securePort);
    }

    @Override
    public String toString() {
        return "Server[id=" + getId() + ",name=" + getName() + ",address=" + getAddress() + ",port=" + getPort()
            + ",securePort=" + getSecurePort() + "]";
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (ctime ^ (ctime >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof Server)) {
            return false;
        }

        final Server other = (Server) obj;

        if (ctime != other.ctime) {
            return false;
        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }

}
