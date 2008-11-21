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
package org.rhq.core.clientapi.server.core;

import java.io.Serializable;

/**
 * Identifies the version of an agent.
 *
 * @author John Mazzitelli
 */
public class AgentVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String version;
    private final String build;

    // in the future, we might want to also include the comm protocol version

    public AgentVersion(String version, String build) {
        this.version = version;
        this.build = build;
    }

    /**
     * The version of the agent, such as "1.0.0.GA".
     * 
     * @return agent version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Identifies the specific build of the agent; this is typically
     * a source code control system revision number, such as "10934".
     * 
     * @return agent build
     */
    public String getBuild() {
        return build;
    }

    @Override
    public String toString() {
        return this.version + "(" + this.build + ")";
    }
}