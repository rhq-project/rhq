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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * An object to capture a snapshot of the order in which particular agents
 * will fail over to particular servers.  The {@link FailoverListDetails}
 * will contain the ordered references back to other {@link Server}s.
 * 
 * @author jmarques
 * @author jshaughnessy
 *
 */
@Entity(name = "FailoverListDetails")
@NamedQueries( //
{
    @NamedQuery(name = FailoverListDetails.QUERY_GET_VIA_AGENT_ID, query = "SELECT fld FROM FailoverListDetails fld WHERE fld.failoverList IN ( SELECT fl FROM FailoverList fl WHERE fl.agent.id = :agentId )"),
    @NamedQuery(name = FailoverListDetails.QUERY_GET_VIA_AGENT_ID_WITH_SERVERS, query = "SELECT fld FROM FailoverListDetails fld JOIN FETCH fld.server WHERE fld.failoverList IN ( SELECT fl FROM FailoverList fl WHERE fl.agent.id = :agentId )"),
    @NamedQuery(name = FailoverListDetails.QUERY_DELETE_VIA_AGENT, query = "DELETE FROM FailoverListDetails fld WHERE fld.failoverList IN ( SELECT fl FROM FailoverList fl WHERE fl.agent = :agent )"),
    @NamedQuery(name = FailoverListDetails.QUERY_DELETE_VIA_SERVER, query = "DELETE FROM FailoverListDetails fld WHERE fld.server = :server"),
    @NamedQuery(name = FailoverListDetails.QUERY_GET_ASSIGNED_LOADS, query = "SELECT new org.rhq.core.domain.cluster.composite.FailoverListDetailsComposite(fld.ordinal, fld.serverId, COUNT(fld.serverId)) FROM FailoverListDetails fld GROUP BY fld.ordinal, fld.serverId ORDER BY fld.ordinal ASC"),
    @NamedQuery(name = FailoverListDetails.QUERY_TRUNCATE, query = "DELETE FROM FailoverListDetails") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_FAILOVER_DETAILS_ID_SEQ")
@Table(name = "RHQ_FAILOVER_DETAILS")
public class FailoverListDetails implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_GET_VIA_AGENT_ID = "FailoverListDetails.getViaAgentId";
    public static final String QUERY_GET_VIA_AGENT_ID_WITH_SERVERS = "FailoverListDetails.getViaAgentIdWithServers";
    public static final String QUERY_DELETE_VIA_AGENT = "FailoverListDetails.deleteViaAgent";
    public static final String QUERY_DELETE_VIA_SERVER = "FailoverListDetails.deleteViaServer";
    public static final String QUERY_GET_ASSIGNED_LOADS = "FailoverListDetails.getAssignedLoads";
    public static final String QUERY_TRUNCATE = "FailoverListDetails.truncate";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @JoinColumn(name = "FAILOVER_LIST_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    protected FailoverList failoverList;

    @Column(name = "ORDINAL", nullable = false)
    private int ordinal;

    @JoinColumn(name = "SERVER_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    protected Server server;

    @Column(name = "SERVER_ID", insertable = false, updatable = false)
    private int serverId;

    // required for JPA
    protected FailoverListDetails() {
    }

    public FailoverListDetails(FailoverList failoverList, int ordinal, Server server) {
        super();
        this.failoverList = failoverList;
        this.ordinal = ordinal;
        this.server = server;
        this.serverId = server.getId();
    }

    public FailoverList getFailoverList() {
        return failoverList;
    }

    public void setFailoverList(FailoverList failoverList) {
        this.failoverList = failoverList;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server agent) {
        this.server = agent;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + serverId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof FailoverListDetails)) {
            return false;
        }

        final FailoverListDetails other = (FailoverListDetails) obj;

        if (failoverList == null) {
            if (other.failoverList != null) {
                return false;
            }
        } else if (!failoverList.equals(other.failoverList)) {
            return false;
        }

        if (ordinal != other.ordinal) {
            return false;
        }

        return true;
    }

}
