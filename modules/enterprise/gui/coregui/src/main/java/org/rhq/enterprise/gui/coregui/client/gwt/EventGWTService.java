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

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.common.EntityContext;
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

    EventSeverity[] getSeverityBuckets(int resourceId, long begin, long end, int numBuckets) throws RuntimeException;

    EventSeverity[] getSeverityBucketsForAutoGroup(int parentResourceId, int resourceTypeId, long begin, long end,
        int numBuckets) throws RuntimeException;

    EventSeverity[] getSeverityBucketsForCompGroup(int resourceGroupId, long begin, long end, int numBuckets)
        throws RuntimeException;

    PageList<Event> findEventsByCriteria(EventCriteria criteria) throws RuntimeException;

    PageList<EventComposite> findEventCompositesByCriteria(EventCriteria criteria) throws RuntimeException;

    int deleteEventsForContext(EntityContext context, List<Integer> eventIds) throws RuntimeException;

    int purgeEventsForContext(EntityContext context) throws RuntimeException;

    Map<EventSeverity, Integer> getEventCountsBySeverity(int resourceId, long startDate, long endDate);
}
