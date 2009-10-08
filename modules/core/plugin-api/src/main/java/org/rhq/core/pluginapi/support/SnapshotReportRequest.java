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
package org.rhq.core.pluginapi.support;

import java.io.Serializable;

/**
 * Encapsulates information about a snapshot report request.
 * 
 * @author John Mazzitelli
 */
public class SnapshotReportRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String description;

    /**
     * @param name identifies the type of snapshot to take
     */
    public SnapshotReportRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Returns the name that identifies the type of snapshot to take. This can be used by the facet implementation
     * to determine what it should include in the snapshot report and how to generate it.
     * 
     * @return name of snapshot to take
     */
    public String getName() {
        return this.name;
    }

    /**
     * A generic description that can be used to identify the specific snapshot being taken or it can
     * describe why the snapshot was being taken. This is typically text entered by a user.
     *
     * @return snapshot report description
     */
    public String getDescription() {
        return this.description;
    }
}
