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
package org.rhq.core.domain.install.remote;

import java.io.Serializable;

/**
 * @author Greg Hinkle
 */
public class RemoteAccessInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String host;
    private String user;
    private String password;
    private byte[] key;
    private int port = 22;
    private String agentName;
    private boolean rememberMe;
    private boolean hostAuthorized = false;

    public RemoteAccessInfo(String host, String user, byte[] key) {
        this.host = host;
        this.user = user;
        this.key = key;
    }

    public RemoteAccessInfo(String host, String user, String password) {
        this(host, 22, user, password);
    }

    public RemoteAccessInfo(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public RemoteAccessInfo() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    /**
     * If known, this is the name of the agent found on the machine pointed to by this object's connection info.
     * If null, the agent name is unknown or there is no agent on the machine.
     * @return agent name or <code>null</code> if not known.
     */
    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    /**
     * If the user wants to remember some of this access control info, this will be true.
     * This object imposes no semantics around this flag - this is just here to allow a user
     * to indicate his permission for the credentials to be remembered, if something supports that.
     * @return true if the user is OK with having the connection credentials stored somewhere for later retrieval.
     */
    public boolean getRememberMe() {
        return this.rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    /**
     * If the host to be connected to is not known, but it is authorized, this will be true.
     * Otherwise, an unknown host (that is, a host with an unknown SSH key fingerprint) will fail to be connected to.
     *
     * If the host is already known, but the known fingerprint is different than the real fingerprint, then
     * this must be true in order to connect to it. Otherwise, the connection attempt will fail.
     *
     * @return flag to indicate if the host is authorized
     */
    public boolean isHostAuthorized() {
        return hostAuthorized;
    }

    public void setHostAuthorized(boolean hostAuthorized) {
        this.hostAuthorized = hostAuthorized;
    }
}
