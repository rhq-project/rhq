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
package org.rhq.core.domain.discovery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Contains a set of one or more {@link Availability} values used to indicate the statuses of a set of resources. Note
 * that only the {@link Availability#getStartTime() start times} are used when looking at the resource times - the end
 * times found in the {@link Availability} objects are ignored. This is because a report only defines a snapshot in time
 * - at a particular time (the availabilities' start times) a resource was either up or down. Reports do not tell you
 * the span of time a resource was up or down, it only tells you what state they were in at a particular millisecond in
 * time.
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class AvailabilityReport implements Serializable {
    private static final long serialVersionUID = 1L;

    public class Datum implements Serializable {

        private static final long serialVersionUID = 1L;

        private int resourceId;
        private AvailabilityType availabilityType;
        private long startTime;

        public Datum(Availability availability) {
            this.resourceId = availability.getResource().getId();
            this.startTime = availability.getStartTime().getTime();
            this.availabilityType = availability.getAvailabilityType();
        }

        public int getResourceId() {
            return resourceId;
        }

        public long getStartTime() {
            return startTime;
        }

        public AvailabilityType getAvailabilityType() {
            return availabilityType;
        }

        public String toString() {
            return "AvailabilityReport.Datum[resourceId=" + this.resourceId + ",type=" + this.availabilityType
                + ",start-time=" + new Date(startTime) + "]";
        }
    }

    private String agentName;
    private List<Datum> availabilities = new ArrayList<Datum>();
    private boolean changesOnly = false;

    /**
     * Constructor for {@link AvailabilityReport} that assumes this report will represent a full inventory (same as if
     * constructing with {@link #AvailabilityReport(boolean, String)} with the first argument being <code>false</code>).
     *
     * @param agentName identifies the agent that produced this report
     */
    public AvailabilityReport(String agentName) {
        this(false, agentName);
    }

    /**
     * Constructor for {@link AvailabilityReport}.
     *
     * @param changesOnly if <code>false</code>, this report will represent the full inventory; in other words, it will
     *                    contain availability statuses for all resources. If <code>true</code>, this report will only
     *                    contain availability statuses for only those resources that have changed status
     * @param agentName   identifies the agent that produced this report
     */
    public AvailabilityReport(boolean changesOnly, String agentName) {
        this.changesOnly = changesOnly;
        this.agentName = agentName;
    }

    /**
     * Returns the agent name of the agent that produced this report.
     *
     * @return the agent name
     */
    public String getAgentName() {
        return agentName;
    }

    public void addAvailability(Availability availability) {
        this.availabilities.add(new Datum(availability));
    }

    public List<AvailabilityReport.Datum> getResourceAvailability() {
        return availabilities;
    }

    /**
     * Returns <code>false</code> if all resources in inventory are represented in this report. <code>true</code> is
     * returned if only those resources that have changed status are in this report.
     *
     * @return indicates if all resources or just resources that changed are found in this report
     */
    public boolean isChangesOnlyReport() {
        return changesOnly;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Returns a string representation of this report.
     *
     * @param  includeAll if <code>true</code>, the returned string includes all the individual availabilities,
     *                    otherwise, the returned string only tells you how many of them there are
     *
     * @return string representation of the report
     */
    public String toString(boolean includeAll) {
        StringBuilder str = new StringBuilder("AV:");
        str.append('[').append(agentName).append(']');
        str.append('[').append(availabilities.size()).append(']');
        str.append('[').append(changesOnly ? "changesOnly" : "full").append(']');

        if (includeAll && (availabilities.size() > 0)) {
            for (Datum next : availabilities) {
                str.append('\n');
                str.append(next);
            }
        }

        return str.toString();
    }

}