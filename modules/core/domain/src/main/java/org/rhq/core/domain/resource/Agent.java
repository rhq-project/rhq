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
package org.rhq.core.domain.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.core.domain.cloud.Server;

/**
 * A JON agent.
 */
@Entity(name = "Agent")
@NamedQueries({
    @NamedQuery(name = Agent.QUERY_FIND_BY_NAME, query = "SELECT a FROM Agent a WHERE a.name = :name"),
    @NamedQuery(name = Agent.QUERY_FIND_BY_ADDRESS_AND_PORT, query = "SELECT a FROM Agent a WHERE a.address = :address AND a.port = :port"),
    @NamedQuery(name = Agent.QUERY_FIND_BY_AGENT_TOKEN, query = "SELECT a FROM Agent a WHERE a.agentToken = :agentToken"),
    @NamedQuery(name = Agent.QUERY_FIND_BY_RESOURCE_ID, query = "SELECT r.agent FROM Resource r WHERE r.id = :resourceId"),
    @NamedQuery(name = Agent.QUERY_FIND_AGENT_ID_BY_RESOURCE_ID, query = "SELECT r.agent.id FROM Resource r WHERE r.id = :resourceId"),
    @NamedQuery(name = Agent.QUERY_FIND_AGENT_ID_BY_NAME, query = "SELECT a.id FROM Agent a WHERE a.name = :name"),
    @NamedQuery(name = Agent.QUERY_FIND_AGENT_ID_BY_SCHEDULE_ID, query = "SELECT r.agent.id FROM MeasurementSchedule sched JOIN sched.resource r WHERE sched.id = :scheduleId"),
    @NamedQuery(name = Agent.QUERY_FIND_ALL, query = "SELECT a FROM Agent a"),
    @NamedQuery(name = Agent.QUERY_FIND_BY_SERVER, query = "SELECT a FROM Agent a WHERE (a.server.id = :serverId OR :serverId IS NULL)"),
    @NamedQuery(name = Agent.QUERY_REMOVE_SERVER_REFERENCE, query = "UPDATE Agent a SET a.server.id = NULL WHERE a.server.id = :serverId "),
    @NamedQuery(name = Agent.QUERY_COUNT_ALL, query = "SELECT count(a.id) FROM Agent a"),
    @NamedQuery(name = Agent.QUERY_FIND_RESOURCE_IDS_FOR_AGENT, query = "SELECT r.id FROM Resource r WHERE r.agent.id = :agentId"),
    @NamedQuery(name = Agent.QUERY_FIND_RESOURCE_IDS_WITH_AGENTS_BY_RESOURCE_IDS, query = "" //
        + "SELECT new org.rhq.core.domain.resource.composite.ResourceIdWithAgentComposite(r.id, r.agent) " //
        + "  FROM Resource r " //
        + " WHERE r.id IN (:resourceIds)"),
    @NamedQuery(name = Agent.QUERY_FIND_ALL_SUSPECT_AGENTS, query = "SELECT new org.rhq.core.domain.resource.composite.AgentLastAvailabilityPingComposite "
        + "       ( "
        + "          a.id,a.name,a.remoteEndpoint,a.lastAvailabilityPing,a.backFilled "
        + "       ) "
        + "  FROM Agent a " + " WHERE a.lastAvailabilityPing < :dateThreshold "),
    @NamedQuery(name = Agent.QUERY_FIND_ALL_WITH_STATUS_BY_SERVER, query = "" //
        + "SELECT a.id " //
        + "  FROM Agent a " //
        + " WHERE a.server.name = :serverName " //
        + "   AND a.status <> 0 "), //
    @NamedQuery(name = Agent.QUERY_FIND_ALL_WITH_STATUS, query = "" //
        + "SELECT a.id " //
        + "  FROM Agent a " //
        + " WHERE a.status <> 0 "), //
    @NamedQuery(name = Agent.QUERY_UPDATE_CLEAR_STATUS_BY_IDS, query = "" //
        + "UPDATE Agent a " //
        + "   SET a.status = 0 " //
        + " WHERE a.id IN ( :agentIds ) "), //
    @NamedQuery(name = Agent.QUERY_FIND_BY_AFFINITY_GROUP, query = "" //
        + "SELECT a " //
        + "  FROM Agent a " //
        + " WHERE a.affinityGroup.id = :affinityGroupId "),
    @NamedQuery(name = Agent.QUERY_FIND_WITHOUT_AFFINITY_GROUP, query = "" //
        + "SELECT a " //
        + "  FROM Agent a " //
        + " WHERE a.affinityGroup IS NULL"), //
    @NamedQuery(name = Agent.QUERY_SET_AGENT_BACKFILLED, query = "" //
        + "UPDATE Agent a " //
        + "   SET a.backFilled = :backfilled " //
        + " WHERE a.id = :agentId " //
        + "   AND a.backFilled <> :backfilled "), //
    @NamedQuery(name = Agent.QUERY_IS_AGENT_BACKFILLED, query = "" //
        + "SELECT COUNT(a.id) " //
        + "  FROM Agent a " //
        + " WHERE a.id = :agentId " //
        + "   AND a.backFilled = true "), //
    @NamedQuery(name = Agent.QUERY_UPDATE_STATUS_BY_RESOURCE, query = "" //
        + " UPDATE Agent a " //
        + "    SET a.status = -1 " // negative numbers so that bitmask strategy does not conflict with this one
        + "  WHERE a.status = 0 " // we only need the first guy to set it
        + "    AND a.id = ( SELECT resA.id " // only update ourselves;
        + "                   FROM Resource res " //
        + "                   JOIN res.agent resA " //
        + "                  WHERE res.id = :resourceId ) "), //
    @NamedQuery(name = Agent.QUERY_UPDATE_STATUS_BY_ALERT_DEFINITION, query = "" //
        + " UPDATE Agent a " //
        + "    SET a.status = -1 " // negative numbers so that bitmask strategy does not conflict with this one
        + "  WHERE a.status = 0 " // we only need the first guy to set it
        + "    AND a.id = ( SELECT resA.id " // only update ourselves;
        + "                   FROM AlertDefinition ad " //
        + "                   JOIN ad.resource res " //
        + "                   JOIN res.agent resA " //
        + "                  WHERE ad.id = :alertDefinitionId ) "), //
    @NamedQuery(name = Agent.QUERY_UPDATE_STATUS_BY_MEASUREMENT_BASELINE, query = "" //
        + " UPDATE Agent a " //
        + "    SET a.status = -1 " // negative numbers so that bitmask strategy does not conflict with this one
        + "  WHERE a.status = 0 " // we only need the first guy to set it
        + "    AND a.id = ( SELECT resA.id " // only update ourselves;
        + "                   FROM MeasurementBaseline mb " //
        + "                   JOIN mb.schedule ms " //
        + "                   JOIN ms.resource res " //
        + "                   JOIN res.agent resA " //
        + "                  WHERE mb.id = :baselineId ) "), //
    @NamedQuery(name = Agent.QUERY_UPDATE_STATUS_BY_AGENT, query = "" //
        + " UPDATE Agent a " //
        + "    SET a.status = -1 " // negative numbers so that bitmask strategy does not conflict with this one
        + "  WHERE a.status = 0 " // we only need the first guy to set it
        + "    AND a.id = :agentId "), //
    @NamedQuery(name = Agent.QUERY_UPDATE_STATUS_FOR_ALL, query = "" //
        + " UPDATE Agent a " //
        + "    SET a.status = -1 " // negative numbers so that bitmask strategy does not conflict with this one
        + "  WHERE a.status = 0 "), //
    @NamedQuery(name = Agent.QUERY_UPDATE_LAST_AVAIL_REPORT, query = "" //
        + " UPDATE Agent a " //
        + "    SET lastAvailabilityReport = :reportTime, backFilled = FALSE " //
        + "  WHERE id = :agentId "), //
    @NamedQuery(name = Agent.QUERY_UPDATE_LAST_AVAIL_PING, query = "" //
        + " UPDATE Agent a " //
        + "    SET lastAvailabilityPing = :now, backFilled = FALSE " //
        + "  WHERE name = :agentName ") //

})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_AGENT_ID_SEQ", sequenceName = "RHQ_AGENT_ID_SEQ")
@Table(name = "RHQ_AGENT")
public class Agent implements Serializable {
    public static final long serialVersionUID = 1L;
    public static final String QUERY_FIND_BY_NAME = "Agent.findByName";
    public static final String QUERY_FIND_BY_ADDRESS_AND_PORT = "Agent.findByAddressAndPort";
    public static final String QUERY_FIND_BY_AGENT_TOKEN = "Agent.findByAgentToken";
    public static final String QUERY_FIND_BY_RESOURCE_ID = "Agent.findByResourceId";
    public static final String QUERY_FIND_AGENT_ID_BY_RESOURCE_ID = "Agent.findAgentIdByResourceId";
    public static final String QUERY_FIND_AGENT_ID_BY_NAME = "Agent.findAgentIdByName";
    public static final String QUERY_FIND_AGENT_ID_BY_SCHEDULE_ID = "Agent.findAgentIdByScheduleId";
    public static final String QUERY_FIND_ALL = "Agent.findAll";
    public static final String QUERY_FIND_BY_SERVER = "Agent.findByServer";
    public static final String QUERY_COUNT_ALL = "Agent.countAll";
    public static final String QUERY_FIND_RESOURCE_IDS_FOR_AGENT = "Agent.findResourceIdsForAgent";
    public static final String QUERY_FIND_RESOURCE_IDS_WITH_AGENTS_BY_RESOURCE_IDS = "Agent.findResourceIdsWithAgentsByResourceIds";
    public static final String QUERY_FIND_ALL_SUSPECT_AGENTS = "Agent.findAllSuspectAgents";
    public static final String QUERY_FIND_BY_AFFINITY_GROUP = "Agent.findByAffinityGroup";
    public static final String QUERY_FIND_WITHOUT_AFFINITY_GROUP = "Agent.findWithoutAffinityGroup";
    public static final String QUERY_SET_AGENT_BACKFILLED = "Agent.setAgentBackfilled";
    public static final String QUERY_IS_AGENT_BACKFILLED = "Agent.isAgentBackfilled";

    // HA queries
    public static final String QUERY_FIND_ALL_WITH_STATUS_BY_SERVER = "Agent.findAllWithStatusByServer";
    public static final String QUERY_FIND_ALL_WITH_STATUS = "Agent.findAllWithStatus";
    public static final String QUERY_UPDATE_CLEAR_STATUS_BY_IDS = "Agent.updateClearStatusByIds";
    public static final String QUERY_REMOVE_SERVER_REFERENCE = "Agent.removeServerReference";

    public static final String QUERY_UPDATE_STATUS_BY_RESOURCE = "Agent.updateStatusByResource";
    public static final String QUERY_UPDATE_STATUS_BY_ALERT_DEFINITION = "Agent.updateStatusByAlertDefinition";
    public static final String QUERY_UPDATE_STATUS_BY_MEASUREMENT_BASELINE = "Agent.updateStatusByMeasurementBasleine";
    public static final String QUERY_UPDATE_STATUS_BY_AGENT = "Agent.updateStatusByAgent";
    public static final String QUERY_UPDATE_STATUS_FOR_ALL = "Agent.updateStatusForAll";

    public static final String QUERY_UPDATE_LAST_AVAIL_REPORT = "Agent.updateLastAvailReport";
    public static final String QUERY_UPDATE_LAST_AVAIL_PING = "Agent.updateLastAvailPing";

    // this value is set, when authorized user wants to reset the token
    public static final String SECURITY_TOKEN_RESET = "@#$reset$#@";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_AGENT_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "ADDRESS", nullable = false)
    private String address;

    @Column(name = "PORT", nullable = false)
    private int port;

    @Column(name = "AGENTTOKEN", nullable = false)
    private String agentToken;

    @Column(name = "REMOTE_ENDPOINT")
    private String remoteEndpoint;

    @Column(name = "CTIME", nullable = false)
    private long ctime = System.currentTimeMillis();

    @Column(name = "MTIME", nullable = false)
    private long mtime = System.currentTimeMillis();

    @Column(name = "LAST_AVAILABILITY_REPORT")
    private Long lastAvailabilityReport;

    @Column(name = "LAST_AVAILABILITY_PING")
    private Long lastAvailabilityPing;

    @JoinColumn(name = "AFFINITY_GROUP_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private AffinityGroup affinityGroup;

    @JoinColumn(name = "SERVER_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private Server server;

    @Column(name = "STATUS", nullable = false)
    private int status;

    @Column(name = "BACKFILLED", nullable = false)
    private boolean backFilled;

    /**
     * Creates a new instance of Agent
     */
    public Agent() {
    }

    /**
     * Constructor for {@link Agent}.
     *
     * @param name
     * @param address
     * @param port
     * @param remoteEndpoint
     * @param agentToken
     */
    public Agent(@NotNull
    String name, String address, int port, String remoteEndpoint, String agentToken) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.remoteEndpoint = remoteEndpoint;
        this.agentToken = agentToken;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * The agent's name will usually, but is not required to, be the fully qualified domain name of the machine where
     * the agent is running. In other words, the {@link #getAddress() address}. However, there is no technical reason
     * why you cannot have an agent with a name such as "foo". In fact, there are use-cases where the agent name is not
     * the same as the address (e.g. perhaps you expect the machine to periodically change hostnames due to DNS name
     * changes and so you want to name it something more permanent).
     *
     * <p>Agent names must be unique - no two agents can have the same name.</p>
     *
     * @return the agent's unique name
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull
    String name) {
        this.name = name;
    }

    /**
     * Returns the machine address that is used to connect to the agent. This can be either an IP address or a fully
     * qualified host name.
     *
     * @return the name of the host where the agent is located
     */
    @NotNull
    public String getAddress() {
        return this.address;
    }

    public void setAddress(@NotNull
    String address) {
        this.address = address;
    }

    /**
     * Returns the port number that the agent is listening to when accepting messages from the server.
     *
     * @return server socket port being listened to by the agent
     */
    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the token string that allows the agent to talk back to the server. If the agent provides an agent token
     * different that this, the server will ignore the agent's messages. The agent gets a new token whenever it
     * registers with the server.
     *
     * @return agent token string
     */
    @NotNull
    public String getAgentToken() {
        return agentToken;
    }

    public void setAgentToken(@NotNull
    String agentToken) {
        this.agentToken = agentToken;
    }

    /**
     * The remote endpoint is the full connection string that is to be used to connect to the agent. If <code>
     * null</code>, a default protocol mechanism will be used. The remote endpoint will usually have both the
     * {@link #getAddress() address} and {@link #getPort() port} encoded in it.
     *
     * @return the endpoint describing where and how to communicate with the agent
     */
    public String getRemoteEndpoint() {
        return remoteEndpoint;
    }

    public void setRemoteEndpoint(String locatorUri) {
        this.remoteEndpoint = locatorUri;
    }

    /**
     * Returns when this agent object was initially created.
     *
     * @return creation time
     */
    public long getCreatedTime() {
        return this.ctime;
    }

    /**
     * Returns when this agent object was modified - usually when it receives a new security token. Other times an agent
     * will be modified is when it registers a new remote endpoint (in the case when the agent changes the port it is
     * listening to, for example).
     *
     * @return modified time
     */
    public long getModifiedTime() {
        return this.mtime;
    }

    /**
     * Returns the timestamp when this agent last returned an availability report. If <code>null</code>, then this agent
     * has yet to send its very first availability report.
     *
     * @return timestamp when the last availability report was received from this agent
     */
    public Long getLastAvailabilityReport() {
        return lastAvailabilityReport;
    }

    /**
     * Sets the timestamp when this agent last returned an availability report.
     *
     * @param lastAvailabilityReport when the last availability report was received from this agent
     */
    public void setLastAvailabilityReport(Long lastAvailabilityReport) {
        this.lastAvailabilityReport = lastAvailabilityReport;
    }

    /**
     * Sets the timestamp when this agent last performed an availability ping.
     *
     * @param lastAvailabilityPing when the last availability ping was received by this agent
     */
    public void setLastAvailabilityPing(Long lastAvailabilityPing) {
        this.lastAvailabilityPing = lastAvailabilityPing;
    }

    /**
     * Returns the timestamp when this agent last performed an availability ping. If <code>null</code>, then this agent
     * has yet to perform its very first availability ping.
     *
     * @return timestamp when the last availability ping was received from this agent
     */
    public Long getLastAvailabilityPing() {
        return lastAvailabilityPing;
    }

    /**
     * Returns the {@link AffinityGroup} this agent currently belongs to.
     *
     * @return the {@link AffinityGroup} this agent currently belongs to
     */
    public AffinityGroup getAffinityGroup() {
        return affinityGroup;
    }

    /**
     * Sets the {@link AffinityGroup} this agent should belong to.
     *
     * @param affinityGroup the {@link AffinityGroup} this agent should belong to
     */
    public void setAffinityGroup(AffinityGroup affinityGroup) {
        this.affinityGroup = affinityGroup;
    }

    /**
     * Returns the {@link Server} this agent is currently communicating to.
     *
     * @return the {@link Server} this agent is currently communicating to
     */
    public Server getServer() {
        return server;
    }

    /**
     * Sets the {@link Server} this agent should communicate with.
     *
     * @param server the {@link Server} this agent should communicate with
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Returns 0 if this agent is current.  Otherwise, returns a mask of {@link Agent.Status}
     * elements corresponding to the updates that have occurred that are related to this agent.
     *
     * @return 0 if this agent is current.  Otherwise, returns a mask of {@link Agent.Status}
     * elements corresponding to the updates that have occurred that are related to this agent.
     */
    public int getStatus() {
        return status;
    }

    /**
     * If this status was non-zero, some scheduled job would have had to come along to perform
     * some work on behalf of this agent.  After that work is complete, the status can be reset
     * (set to 0) signifying that no further work needs to be done on this agent (as long as the
     * status remains 0).
     */
    public void clearStatus() {
        status = 0;
    }

    /**
     * If some subsystem makes a change to some data that this agent cares about (as summarized
     * by the various {@link Status} elements), then that change should be added via this method.
     * Periodically, a background job will come along, check the status, and possibly perform
     * work on behalf of this agent based on the type of change.
     */
    public void addStatus(Status newStatus) {
        this.status |= newStatus.mask;
    }

    public List<String> getStatusMessages() {
        return Status.getMessages(status);
    }

    public enum Status {

        RESOURCE_HIERARCHY_UPDATED(1, "This agent's managed resource hierarchy has been updated"), //
        BASELINES_CALCULATED(2, "This agent's baselines have been recalculated"), //
        ALERT_DEFINITION(4, "Some alert definition with an agent-specific condition category was updated");

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

    public boolean isBackFilled() {
        return backFilled;
    }

    public void setBackFilled(boolean backFilled) {
        this.backFilled = backFilled;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        if (this.server == null) {
            printWithTrace("Agent getting it's server reference set to null");
        }
        this.mtime = System.currentTimeMillis();
    }

    private void printWithTrace(String message) {
        try {
            new IllegalArgumentException(message);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Agent)) {
            return false;
        }

        Agent agent = (Agent) obj;
        return name.equals(agent.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Agent[id=" + id + ",name=" + this.name + ",address=" + this.address + ",port=" + this.port
            + ",remote-endpoint=" + this.remoteEndpoint + ",last-availability-ping=" + this.lastAvailabilityPing
            + ",last-availability-report=" + this.lastAvailabilityReport + "]";
    }
}