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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Agent;

/**
 * An object to logically group {@link Server}s and {@link Agent}s
 * together so that the high-availability distribution algorithm
 * repartitions the {@link Agent}s with tendencies to connect and
 * fail over to {@link Server}s in the same {@link AffinityGroup}.
 * 
 * @author Joseph Marques
 *
 */
@Entity(name = "AffinityGroup")
@NamedQueries //
( { @NamedQuery(name = AffinityGroup.QUERY_FIND_ALL, query = "SELECT ag FROM AffinityGroup ag"),
    @NamedQuery(name = AffinityGroup.QUERY_FIND_ALL_COMPOSITES, query = "" //
        + "SELECT NEW org.rhq.core.domain.cluster.composite.AffinityGroupCountComposite " //
        + "     ( " //
        + "       ag, " //
        + "       (SELECT COUNT(a) FROM Agent a WHERE a.affinityGroup = ag), " //
        + "       (SELECT COUNT(s) FROM Server s WHERE s.affinityGroup = ag) " //
        + "     ) " //
        + "  FROM AffinityGroup ag "), //
    @NamedQuery(name = AffinityGroup.QUERY_UPDATE_REMOVE_AGENTS, query = "" //
        + "UPDATE Agent a " //
        + "   SET a.affinityGroup = NULL " //
        + " WHERE a.affinityGroup.id IN ( :affinityGroupIds ) "), //
    @NamedQuery(name = AffinityGroup.QUERY_UPDATE_REMOVE_SERVERS, query = "" //
        + "UPDATE Server s " //
        + "   SET s.affinityGroup = NULL " //
        + " WHERE s.affinityGroup.id IN ( :affinityGroupIds ) "), //
    @NamedQuery(name = AffinityGroup.QUERY_DELETE_BY_IDS, query = "" //
        + "DELETE FROM AffinityGroup ag " //
        + " WHERE ag.id IN ( :affinityGroupIds ) ") // 
})
@SequenceGenerator(name = "id", sequenceName = "RHQ_AFFINITY_GROUP_ID_SEQ")
@Table(name = "RHQ_AFFINITY_GROUP")
public class AffinityGroup implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "AffinityGroup.findAll";
    public static final String QUERY_FIND_ALL_COMPOSITES = "AffinityGroup.findAllComposites";
    public static final String QUERY_UPDATE_REMOVE_AGENTS = "AffinityGroup.updateRemoveAgents";
    public static final String QUERY_UPDATE_REMOVE_SERVERS = "AffinityGroup.updateRemoveServers";
    public static final String QUERY_DELETE_BY_IDS = "AffinityGroup.deleteByIds";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @OneToMany(mappedBy = "affinityGroup", fetch = FetchType.LAZY)
    private List<Server> servers = new ArrayList<Server>();

    @OneToMany(mappedBy = "affinityGroup", fetch = FetchType.LAZY)
    private List<Agent> agents = new ArrayList<Agent>();

    // required for JPA
    protected AffinityGroup() {
    }

    public AffinityGroup(String name) {
        this.name = name;
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

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof AffinityGroup)) {
            return false;
        }

        final AffinityGroup other = (AffinityGroup) obj;

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
