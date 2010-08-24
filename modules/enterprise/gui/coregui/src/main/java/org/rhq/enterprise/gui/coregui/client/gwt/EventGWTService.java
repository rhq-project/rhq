/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public interface EventGWTService extends RemoteService {


    EventSeverity[] getSeverityBuckets(
            int resourceId,
            long begin,
            long end,
            int numBuckets);

    PageList<Event> findEventsByCriteria(EventCriteria criteria);
    EventSeverity[] getSeverityBucketsForAutoGroup(
            int parentResourceId,
            int resourceTypeId,
            long begin,
            long end,
            int numBuckets);

    PageList<EventComposite> findEventCompositesByCriteria(EventCriteria criteria);
    EventSeverity[] getSeverityBucketsForCompGroup(

    int deleteEventsForContext(EntityContext context, List<Integer> eventIds);
            int resourceGroupId,
            long begin,
            long end,
            int numBuckets);

    int purgeEventsForContext(EntityContext context);
}
