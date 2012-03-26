/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.core.domain.criteria;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.util.PageOrdering;

/**
 * Criteria for fetching Availability records, typically constrained to a Resource.
 * 
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class AvailabilityCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    // this is used to perform an interval check, see below for more, or to filter out the initial avail record
    private Long filterStartTime;
    private Integer filterResourceId; // requires overrides
    private List<AvailabilityType> filterAvailabilityTypes;

    private boolean fetchResource;

    private PageOrdering sortStartTime;

    public AvailabilityCriteria() {
        // see addFilterInterval() for more on the startTime override
        filterOverrides.put("resourceId", "resource.id = ?");
    }

    @Override
    public Class<Availability> getPersistentClass() {
        return Availability.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    /**
     * Return Availability where any portion of the Availability period falls into the specified interval. This
     * filter is mutually exclusive with filterInitialAvailability.
     *  
     * @param intervalStartTime NOT NULL, in *SECONDS* (not ms)
     * @param intervalEndTime   NOT NULL,  >= filterStartTime, in *SECONDS* (not ms)
     */
    public void addFilterInterval(Long intervalStartTime, Long intervalEndTime) {
        if (null != filterStartTime) {
            throw new IllegalStateException(
                "This filter is mutually exclusive with filterInitialAvailability, which has already been set.");
        }

        // This plays around with the way criteria generation works.  For availability we're interested in
        // interval checking but not in the standard way, we want all Avail records that *overlap* with the
        // specified interval.  Things to note about the fragment below:
        // - "startTime >= 0" is done on purpose, the fragment must start with "startTime" due to the generator
        // - we do not use BETWEEN on purpose, it caused a problem with param assignment in hibernate. so, we use the expanded form
        // - startTime and endTime are persisted in seconds,  so the paremeters should be in seconds
        String filterFragment = "" //
            + "startTime >= 0" // innoccuous tautology just needed to get generated syntax correct 
            + " AND (  ( availability.startTime >= ? AND availability.startTime <= " + intervalEndTime + " )" // interval straddles :start
            + " OR ( availability.startTime <= ? AND ( availability.endTime >= ? OR availability.endTime IS NULL ) ) )"; // availability straddles :start 

        filterOverrides.put("startTime", filterFragment);
        this.filterStartTime = intervalStartTime;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public void addFilterAvailabilityTypes(AvailabilityType... filterAvailabilityTypes) {
        if (filterAvailabilityTypes != null && filterAvailabilityTypes.length > 0) {
            this.filterAvailabilityTypes = Arrays.asList(filterAvailabilityTypes);
        }
    }

    /**
     * Include initial UNKNOWN availability with startTime=0. If unset it will be included. This
     * filter is mutually exclusive with filterInterval.
     */
    public void addFilterInitialAvailability(Boolean initialAvailability) {
        if (null != filterStartTime) {
            throw new IllegalStateException(
                "This filter is mutually exclusive with filterInterval, which has already been set.");
        }

        this.filterStartTime = (initialAvailability) ? 0L : 1L;
        filterOverrides.put("startTime", "startTime >= ?");
    }

    public void fetchResource(boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void addSortStartTime(PageOrdering sortStartTime) {
        if (!getOrderingFieldNames().contains("startTime")) {
            addSortField("startTime");
        }
        this.sortStartTime = sortStartTime;
    }
}
