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

package org.rhq.enterprise.server.event;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Interface for the Event Manager
 * @author Heiko W. Rupp
 * @author Joseph Marques
 *
 */
@Local
public interface EventManagerLocal {

    /**
     * Add the passed events to the database
     * @param events a set of events.
     */
    public void addEventData(Map<EventSource, Set<Event>> events);

    /**
     * Deletes event data older than the specified time.
     *
     * @param deleteUpToTime event data older than this time will be deleted
     * @return number of deleted Events
     */
    public int purgeEventData(Date deleteUpToTime) throws SQLException;

    /**
     * Retrieve the events for the given resources that happened in the given time frame.
     * @param subject
     * @param resources Resources we are interested in
     * @param startDate Start time of interest
     * @param endDate End time of interest
     * @return List of Events for that time frame.
     */
    public List<Event> getEventsForResources(Subject subject, List<Resource> resources, long startDate, long endDate);

    /**
     * Retrieve the events for the given resource that happened in the given time frame.
     * @param subject
     * @param resourceId
     * @param startDate
     * @param endDate
     * @param severity Severity of events we are interested in. Pass 'null' for all events.
     * @param pc TODO
     * @return
     */
    public PageList<EventComposite> getEventsForResource(Subject subject, int resourceId, long startDate, long endDate,
        EventSeverity severity, PageControl pc);

    public PageList<EventComposite> getEventsForCompGroup(Subject subject, int groupId, long begin, long endDate,
        EventSeverity severity, PageControl pc);

    public PageList<EventComposite> getEventsForCompGroup(Subject subject, int groupId, long begin, long endDate,
        EventSeverity severity, int eventId, String source, String searchString, PageControl pc);

    public PageList<EventComposite> getEventsForAutoGroup(Subject subject, int parent, int type, long begin,
        long endDate, EventSeverity object, PageControl pc);

    public PageList<EventComposite> getEventsForAutoGroup(Subject subject, int parent, int type, long begin,
        long endDate, EventSeverity object, int eventId, String source, String searchString, PageControl pc);

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
    public int[] getEventCounts(Subject subject, int resourceId, long begin, long end, int numBuckets);

    /**
     * Provide the buckets for a timeline with the (most severe) severity for each bucket.
     * @param subject    Subject of the caller
     * @param resourceId Id of the resource for which we want to know the data
     * @param begin      Begin date
     * @param end        End date
     * @param numBuckets Number of buckets to distribute into.
     * @return
     */
    public EventSeverity[] getSeverityBuckets(Subject subject, int resourceId, long begin, long end, int numBuckets);

    /**
     * Provide the buckets for a timeline with the (most severe) severity for each bucket.
     * @param subject    Subject of the caller
     * @param groupId    Id of the compatible group for which we want to know the data
     * @param begin      Begin date
     * @param end        End date
     * @param numBuckets Number of buckets to distribute into.
     * @return
     */
    public EventSeverity[] getSeverityBucketsForCompGroup(Subject subject, int groupId, long begin, long end,
        int numBuckets);

    /**
     * Provide the buckets for a timeline with the (most severe) severity for each bucket.
     * @param subject    Subject of the caller
     * @param parentId   Id of the parent of the autogroup for which we want to know the data
     * @param type       Id of the children type of the autogroup
     * @param begin      Begin date
     * @param end        End date
     * @param numBuckets Number of buckets to distribute into.
     * @return
     */
    public EventSeverity[] getSeverityBucketsForAutoGroup(Subject subject, int parentId, int type, long begin,
        long end, int numBuckets);

    /**
     * Return a {@link PageList} of {@link Event} objects, that match the input 
     * @param subject
     * @param resourceIds We want events for those resources
     * @param begin      Begin time for the events display
     * @param end        End time for the events display
     * @param severity   Severity we are interested in. Null for all
     * @param eventId    If > -1 , show events around this given one (including the given)
     * @param source TODO
     * @param searchString TODO
     * @param pc         {@link PageControl} to specify the list size
     * @return           List of Events
     */
    public PageList<EventComposite> getEvents(Subject subject, int[] resourceIds, long begin, long end,
        EventSeverity severity, int eventId, String source, String searchString, PageControl pc);

    /**
     * Obtain detail information about the passed event
     * @param subject Subject of the caller
     * @param eventId ID of the desired event.
     * @return
     */
    public EventComposite getEventDetailForEventId(Subject subject, int eventId) throws EventException;

    public void deleteEventSourcesForDefinition(EventDefinition def);

    public int getEventDefinitionCountForResourceType(int resourceTypeId);

}
