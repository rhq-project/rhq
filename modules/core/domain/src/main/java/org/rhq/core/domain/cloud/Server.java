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

import org.rhq.core.domain.cloud.composite.FailoverListComposite;
import org.rhq.core.domain.cloud.composite.FailoverListComposite.ServerEntry;
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
        + "SELECT NEW org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite" //
        + "     ( " //
        + "       s, " //
        + "       (SELECT COUNT(a) FROM Agent a WHERE a.server = s) " //
        + "     ) " //
        + "  FROM Server s"),
    @NamedQuery(name = Server.QUERY_FIND_BY_NAME, query = "" //
        + "         SELECT s " //
        + "           FROM Server s " //
        + "LEFT JOIN FETCH s.affinityGroup ag " //
        + "          WHERE s.name = :name"),
    @NamedQuery(name = Server.QUERY_FIND_ALL_CLOUD_MEMBERS, query = "SELECT s FROM Server s WHERE NOT s.operationMode = 'INSTALLED'"),
    @NamedQuery(name = Server.QUERY_FIND_ALL_NORMAL_CLOUD_MEMBERS, query = "SELECT s FROM Server s WHERE s.operationMode = 'NORMAL'"),
    @NamedQuery(name = Server.QUERY_FIND_BY_AFFINITY_GROUP, query = "" //
        + "SELECT s " //
        + "  FROM Server s " //
        + " WHERE s.affinityGroup.id = :affinityGroupId "),
    @NamedQuery(name = Server.QUERY_FIND_WITHOUT_AFFINITY_GROUP, query = "" //
        + "SELECT s " //
        + "  FROM Server s " //
        + " WHERE s.affinityGroup IS NULL "), //
    @NamedQuery(name = Server.QUERY_DELETE_BY_ID, query = "" //
        + "DELETE FROM Server s WHERE s.id = :serverId "), //
    @NamedQuery(name = Server.QUERY_UPDATE_SET_STALE_DOWN, query = "" //
        + "UPDATE Server s " //
        + "   SET s.operationMode = :downMode " //
        + " WHERE s.operationMode = :normalMode " //
        + "   AND s.mtime < :staleTime " //
        + "   AND ( s.name <> :thisServerName OR :thisServerName IS NULL ) "), //
    @NamedQuery(name = Server.QUERY_UPDATE_STATUS_BY_NAME, query = "" //
        + " UPDATE Server s " //
        + "    SET s.status = 3 " //Change this to the only value possible before adding MANUAL_MAINTENANCE_MODE.
                                  //This status should never be set to negative since negative values are not
                                  //allowed by the bitmask.
        + "  WHERE s.status = 0 ") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_SERVER_ID_SEQ", sequenceName = "RHQ_SERVER_ID_SEQ")
@Table(name = "RHQ_SERVER")
public class Server implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "Server.findAll";
    public static final String QUERY_FIND_ALL_COMPOSITES = "Server.findAllComposites";
    public static final String QUERY_FIND_BY_NAME = "Server.findByName";
    public static final String QUERY_FIND_ALL_CLOUD_MEMBERS = "Server.findAllCloudMembers";
    public static final String QUERY_FIND_BY_AFFINITY_GROUP = "Server.findByAffinityGroup";
    public static final String QUERY_FIND_WITHOUT_AFFINITY_GROUP = "Server.findWithoutAffinityGroup";
    public static final String QUERY_DELETE_BY_ID = "Server.deleteById";
    public static final String QUERY_UPDATE_SET_STALE_DOWN = "Server.updateSetStaleDown";
    public static final String QUERY_FIND_ALL_NORMAL_CLOUD_MEMBERS = "Server.findAllNormalCloudMembers";
    public static final String QUERY_UPDATE_STATUS_BY_NAME = "Server.updateStatusByName";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_SERVER_ID_SEQ")
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

    // the time this server node was last updated
    @Column(name = "MTIME", nullable = false)
    private long mtime;

    @JoinColumn(name = "AFFINITY_GROUP_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private AffinityGroup affinityGroup;

    @OneToMany(mappedBy = "server", fetch = FetchType.LAZY)
    private List<Agent> agents = new ArrayList<Agent>();

    @Column(name = "STATUS", nullable = false)
    private int status;

    @Column(name = "VERSION", nullable = false)
    private String version;

    // required for JPA
    public Server() {
    }

    public Server(int serverId) {
        this.id = serverId;
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

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

        DOWN("This server is down member of the HA server cloud", true), //
        INSTALLED("This server is newly installed but not yet fully operating", true), //
        MAINTENANCE("This server is a Maintenance Mode member of the HA server cloud", false), //
        NORMAL("This server is a Normal Member of the HA server cloud", true);

        public final String message;
        private final boolean readOnly;

        private OperationMode(String message, boolean configurable) {
            this.message = message;
            this.readOnly = configurable;
        }

        public String getMessage() {
            return message;
        }

        public boolean isReadOnly() {
            return readOnly;
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
        return new FailoverListComposite.ServerEntry(id, address, port, securePort);
    }

    /**
     * Returns 0 if this server is current.  Otherwise, returns a mask of {@link Server.Status}
     * elements corresponding to the updates that have occurred that are related to this server.
     *
     * @return 0 if this server is current.  Otherwise, returns a mask of {@link Server.Status}
     * elements corresponding to the updates that have occurred that are related to this server.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Verifies if bitmask status is set.
     *
     * @param queryStatus status
     * @return true if status set, false otherwise
     */
    public boolean hasStatus(Status queryStatus) {
        return (this.status & queryStatus.mask) == queryStatus.mask;
    }

    /**
     * Clears the specified bitmask status.
     *
     * @param removeStatus status to be removed
     */
    public void clearStatus(Status removeStatus) {
        this.status &= ~removeStatus.mask;
    }

    /**
     * @deprecated do not use this method as it may corrupt the tracking of the server status. Use the new
     * {@link #clearStatus(org.rhq.core.domain.cloud.Server.Status)} instead.
     */
    @Deprecated
    public void clearStatus() {
        this.status = 0;
    }

    /**
     * If some subsystem makes a change to some data that this server cares about (as summarized
     * by the various {@link Status} elements), then that change should be added via this method.
     * Periodically, a background job will come along, check the status, and possibly perform
     * work on behalf of this server based on the type of change.
     * @param newStatus
     */
    public void addStatus(Status newStatus) {
        this.status |= newStatus.mask;
    }

    /**
     * @return list all messages for the status mask
     */
    public List<String> getStatusMessages() {
        return Status.getMessages(status);
    }

    //Please read BZ 535484 for initial design: https://bugzilla.redhat.com/show_bug.cgi?id=535484
    //Prior to MANUAL_MAINTENANCE_MODE only used for debug purposes, design now changed to
    //persist statuses between server restarts in production code
    public enum Status {

        //Debug only flags (first five bits are reserved for debug flags)
        RESOURCE_HIERARCHY_UPDATED(1, "The resource hierarchy has been updated"), //
        ALERT_DEFINITION(2, "Some alert definition with a global condition category was updated"),

        //Production flags
        MANUAL_MAINTENANCE_MODE(32, "Manual Maintenance mode setup by the user either via UI or properties file.");

        public final int mask;
        public final String message;

        private Status(int mask, String message) {
            this.mask = mask;
            this.message = message;
        }

        public static List<String> getMessages(int mask) {
            List<String> results = new ArrayList<String>();
            for (Status next : Status.values()) {
                if (next.mask == (next.mask & mask)) {
                    results.add(next.message);
                }
            }
            return results;
        }

    }

    @Override
    public String toString() {
        return "Server[id=" + getId() + ",name=" + getName() + ",address=" + getAddress() + ",port=" + getPort()
            + ",securePort=" + getSecurePort() + "]";
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
        this.mtime = this.ctime;
        // I'd like to do this bu smartgwt can't handle the getPackage() call
        //this.version = this.getClass().getPackage().getImplementationVersion();
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
