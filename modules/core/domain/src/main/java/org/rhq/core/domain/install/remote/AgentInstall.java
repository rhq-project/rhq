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
package org.rhq.core.domain.install.remote;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Agent installation details. Not all agents will have associated install details; only those agents
 * where the install details are known will have this.
 */
@Entity(name = "AgentInstall")
@NamedQueries({
 @NamedQuery(name = AgentInstall.QUERY_FIND_BY_NAME, query = "SELECT ai FROM AgentInstall ai WHERE ai.agentName = :agentName"),
    @NamedQuery(name = AgentInstall.QUERY_DELETE_BY_NAME, query = "DELETE FROM AgentInstall ai WHERE ai.agentName = :agentName"),
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_AGENT_INSTALL_ID_SEQ", sequenceName = "RHQ_AGENT_INSTALL_ID_SEQ")
@Table(name = "RHQ_AGENT_INSTALL")
public class AgentInstall implements Serializable {
    public static final long serialVersionUID = 1L;
    public static final String QUERY_FIND_BY_NAME = "AgentInstall.findByName";
    public static final String QUERY_DELETE_BY_NAME = "AgentInstall.deleteByName";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_AGENT_INSTALL_ID_SEQ")
    @Id
    private int id;

    @Column(name = "MTIME", nullable = false)
    private long mtime = System.currentTimeMillis();

    @Column(name = "AGENT_NAME", nullable = true)
    private String agentName;

    @Column(name = "INSTALL_LOCATION", nullable = true)
    private String installLocation;

    @Column(name = "SSH_HOST", nullable = true)
    private String sshHost;

    @Column(name = "SSH_PORT", nullable = true)
    private Integer sshPort;

    @Column(name = "SSH_USERNAME", nullable = true)
    private String sshUsername;

    @Column(name = "SSH_PASSWORD", nullable = true)
    private String sshPassword;

    /**
     * Creates a new instance of AgentInstall.
     */
    public AgentInstall() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getModifiedTime() {
        return this.mtime;
    }

    public String getAgentName() {
        return this.agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getInstallLocation() {
        return this.installLocation;
    }

    public void setInstallLocation(String installLocation) {
        this.installLocation = installLocation;
    }

    public String getSshHost() {
        return this.sshHost;
    }

    public void setSshHost(String host) {
        this.sshHost = host;
    }

    public Integer getSshPort() {
        return this.sshPort;
    }

    public void setSshPort(Integer port) {
        this.sshPort = port;
    }

    public String getSshUsername() {
        return this.sshUsername;
    }

    public void setSshUsername(String username) {
        this.sshUsername = username;
    }

    public String getSshPassword() {
        return this.sshPassword;
    }

    public void setSshPassword(String pw) {
        this.sshPassword = pw;
    }

    /**
     * Given another AgentInstall object ("overlay") this method will take
     * all non-null fields (EXCEPT for the id field) from the overlay object
     * and set them in this object.
     *
     * @param overlay contains non-null values that will be set in this object
     */
    public void overlay(AgentInstall overlay) {
        if (overlay == null) {
            return;
        }

        if (overlay.agentName != null) {
            this.agentName = overlay.agentName;
        }
        if (overlay.installLocation != null) {
            this.installLocation = overlay.installLocation;
        }
        if (overlay.sshHost != null) {
            this.sshHost = overlay.sshHost;
        }
        if (overlay.sshPort != null) {
            this.sshPort = overlay.sshPort;
        }
        if (overlay.sshUsername != null) {
            // to force usename to be nulled out, caller will pass in an empty string - that signals us to null out username
            this.sshUsername = (overlay.sshUsername.length() == 0) ? null : overlay.sshUsername;
        }
        if (overlay.sshPassword != null) {
            // to force password to be nulled out, caller will pass in an empty string - that signals us to null out password
            this.sshPassword = (overlay.sshPassword.length() == 0) ? null : overlay.sshPassword;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agentName == null) ? 0 : agentName.hashCode());
        result = prime * result + ((installLocation == null) ? 0 : installLocation.hashCode());
        result = prime * result + ((sshHost == null) ? 0 : sshHost.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AgentInstall)) {
            return false;
        }
        AgentInstall other = (AgentInstall) obj;
        if (agentName == null) {
            if (other.agentName != null) {
                return false;
            }
        } else if (!agentName.equals(other.agentName)) {
            return false;
        }
        if (installLocation == null) {
            if (other.installLocation != null) {
                return false;
            }
        } else if (!installLocation.equals(other.installLocation)) {
            return false;
        }
        if (sshHost == null) {
            if (other.sshHost != null) {
                return false;
            }
        } else if (!sshHost.equals(other.sshHost)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AgentInstall[id=" + id + ",agentName=" + this.agentName + ",installLocation=" + this.installLocation
            + "]";
    }

    @PrePersist
    void onPersist() {
        this.mtime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }
}