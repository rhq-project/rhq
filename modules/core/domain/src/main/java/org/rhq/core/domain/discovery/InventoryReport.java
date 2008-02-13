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
package org.rhq.core.domain.discovery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.util.exception.ExceptionPackage;

/**
 * This tracks the results of an inventory discovery scan and is used to send the information back to the server when
 * appropriate.
 *
 * @author Greg Hinkle
 */
public class InventoryReport implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Should be the same as the request if server initiated. Null otherwise.
     */
    private Long requestId;

    private Agent agent;

    /**
     * Resources that are new to this report
     */
    private Set<Resource> addedRoots = new LinkedHashSet<Resource>();

    /**
     * TODO GH: We're not currently respecting this on the server - should we? private List<Resource> removedRoots;
     */

    private List<ExceptionPackage> errors = new ArrayList<ExceptionPackage>();

    private long startTime;

    private long endTime;

    private boolean runtimeReport;

    public InventoryReport(Agent agent) {
        this.agent = agent;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public Agent getAgent() {
        return agent;
    }

    public Set<Resource> getAddedRoots() {
        return addedRoots;
    }

    public void addAddedRoot(Resource resource) {
        this.addedRoots.add(resource);
    }

    public synchronized List<ExceptionPackage> getErrors() {
        return errors;
    }

    public void addError(ExceptionPackage error) {
        errors.add(error);
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isRuntimeReport() {
        return runtimeReport;
    }

    public void setRuntimeReport(boolean runtimeReport) {
        this.runtimeReport = runtimeReport;
    }

    /**
     * @return a count of the total number of resources to be added in this report
     */
    public int getResourceCount() {
        int count = 0;
        for (Resource r : addedRoots) {
            count += countResourceHierarchy(r);
        }

        return count;
    }

    private int countResourceHierarchy(Resource r) {
        int count = 1;
        for (Resource child : r.getChildResources()) {
            count += countResourceHierarchy(child);
        }

        return count;
    }
}