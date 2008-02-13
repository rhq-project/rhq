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
package org.rhq.core.pluginapi.inventory;

import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.system.ProcessInfo;

/**
 * An individual result from a process scan which includes information on the process that matched and the
 * {@link ProcessScan} itself.
 *
 * @author John Mazzitelli
 */
public class ProcessScanResult {
    private final ProcessScan processScan;
    private final ProcessInfo processInfo;

    public ProcessScanResult(ProcessScan scan, ProcessInfo info) {
        this.processScan = scan;
        this.processInfo = info;
    }

    /**
     * Represents the operating system process that matched the {@link #getProcessScan() process scan}'s query. This
     * provides information such as the process name and its command line arguments.
     *
     * @return matched process' information
     */
    public ProcessInfo getProcessInfo() {
        return processInfo;
    }

    /**
     * Returns information on the process scan, such as the query that was used to match the
     * {@link #getProcessInfo() process} and the process scan's name, if one was provided.
     *
     * @return process scan information
     */
    public ProcessScan getProcessScan() {
        return processScan;
    }

    @Override
    public String toString() {
        return "ProcessScanResult: scan=[" + processScan + "], info=[" + processInfo + "]";
    }
}