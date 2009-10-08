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

import java.io.InputStream;
import java.io.Serializable;

/**
 * Contains the results of a snapshot report, including a stream to the contents of the snapshot.
 * 
 * @author John Mazzitelli
 */
public class SnapshotReportResults implements Serializable {
    private static final long serialVersionUID = 1L;

    private final InputStream inputStream;

    /**
     * Creates the results object.
     * 
     * @param inputStream the stream that contains the snapshot report content.
     */
    public SnapshotReportResults(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }
}
