/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.event;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Interface for the Event Manager
 * @author Heiko W. Rupp
 * @author Joseph Marques
 *
 */
@Local
public interface EventManagerLocal extends EventManagerRemote {

    /**
     * Add the passed events to the database
     * @param events a set of events.
     */
    void addEventData(Map<EventSource, Set<Event>> events);

    Map<EventSeverity, Integer> getEventCountsBySeverity(Subject subject, int resourceId, long startDate, long endDate);

    Map<EventSeverity, Integer> getEventCountsBySeverityForGroup(Subject subject, int groupId, long startDate,
        long endDate);

    /**
     * Retrieve the count of events for the given resource in the time between begin and end, nicely separated
     * in numBuckets.
     * @param subject    Subject of the caller
     * @param resourceId Id of the resource we want to know the data
     * @param begin      Begin date
     * @param end        End date
     * @param numBuckets Number of buckets to distribute into.
     * @return
     */
    int[] getEventCounts(Subject subject, int resourceId, long begin, long end, int numBuckets);

    /**
     * Obtain detail information about the passed event
     * @param subject Subject of the caller
     * @param eventId ID of the desired event.
     * @return
     */
    EventComposite getEventDetailForEventId(Subject subject, int eventId) throws EventException;

    void deleteEventSourcesForDefinition(EventDefinition def);

    PageList<EventComposite> findEventComposites(Subject subject, EntityContext context, long begin, long end,
        EventSeverity[] severities, String source, String detail, PageControl pc);

    PageList<EventComposite> findEventCompositesByCriteria(Subject subject, EventCriteria criteria);

    EventSeverity[] getSeverityBucketsByContext(Subject subject, EntityContext context, long begin, long end,
        int bucketCount);
}
