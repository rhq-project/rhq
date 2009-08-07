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
package org.rhq.enterprise.server.system;

import java.io.Serializable;

/**
 * Provides version information on the server itself.
 *
 * @author John Mazzitelli
 */
public class ServerVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String version;
    private final String build;

    public ServerVersion(String version, String build) {
        this.version = version;
        this.build = build;
    }

    /**
     * The version of the server, such as "1.0.0.GA".
     * 
     * @return server version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Identifies the specific build of the server; this is typically
     * a source code control system revision number, such as "10934".
     * 
     * @return server build
     */
    public String getBuild() {
        return build;
    }

    @Override
    public String toString() {
        return this.version + "(" + this.build + ")";
    }
}
