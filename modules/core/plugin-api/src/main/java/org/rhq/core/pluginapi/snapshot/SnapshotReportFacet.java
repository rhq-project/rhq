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
package org.rhq.core.pluginapi.snapshot;

import java.io.InputStream;

/**
 * Facet that exposes a component's ability to take a snapshot report of its logs, config and other data.
 * 
 * @author John Mazzitelli
 *
 */
public interface SnapshotReportFacet {
    /**
     * Takes a snapshot and returns the snapshot report content in the given stream. A facet implementation
     * can support different kinds of snapshots, the given name determines which kind of snapshot to take.
     * 
     * @param name identifies the type of snapshot to take
     * @param description a generic description that can be used to identify the specific snapshot being taken or it can
     *                    describe why the snapshot was being taken. This is typically text entered by a user.
     * @return a stream containing the contents of the snapshot report
     * @throws Exception if failed to generate the snapshot report
     */
    InputStream getSnapshotReport(String name, String description) throws Exception;
}
