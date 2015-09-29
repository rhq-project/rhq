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

import java.util.List;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.util.PageList;

/**
 * Public API for working with Events.
 */
@Remote
public interface EventManagerRemote {

    /**
     * @param subject
     * @param resourceId
     * @param begin in millis
     * @param end in millis
     * @param numBuckets
     * @return not null
     * @deprecated use {@link #findEventsByCriteria(Subject, EventCriteria)}
     */
    EventSeverity[] getSeverityBuckets(Subject subject, int resourceId, long begin, long end, int numBuckets);

    /**
     * @param subject
     * @param parentResourceId
     * @param resourceTypeId
     * @param begin in millis
     * @param end in millis
     * @param numBuckets
     * @return not null
     * @deprecated use {@link #findEventsByCriteria(Subject, EventCriteria)}
     */
    EventSeverity[] getSeverityBucketsForAutoGroup(Subject subject, int parentResourceId, int resourceTypeId,
        long begin, long end, int numBuckets);

    /**
     * @param subject
     * @param resourceGroupId
     * @param begin in millis
     * @param end in millis
     * @param numBuckets
     * @return not null
     * @deprecated use {@link #findEventsByCriteria(Subject, EventCriteria)}
     */
    EventSeverity[] getSeverityBucketsForCompGroup(Subject subject, int resourceGroupId, long begin, long end,
        int numBuckets);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<Event> findEventsByCriteria(Subject subject, EventCriteria criteria);

    /**
     *
     * @param subject Requires MANAGE_EVENT user rights or AutoGroupRights for AutoGroups
     * @param context Acceptable values: Resource, ResourceGroup and AutoGroup
     * @param eventIds
     * @return
     */
    int deleteEventsForContext(Subject subject, EntityContext context, List<Integer> eventIds);

    /**
     *
     * @param subject Requires MANAGE_EVENT user rights
     * @param context Acceptable values: Resource and ResourceGroup
     * @return
     */
    int purgeEventsForContext(Subject subject, EntityContext context);
}
