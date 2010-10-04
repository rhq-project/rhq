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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.EventGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class EventGWTServiceImpl extends AbstractGWTServiceImpl implements EventGWTService {

    private static final long serialVersionUID = 1L;

    private EventManagerLocal eventManager = LookupUtil.getEventManager();

    public EventSeverity[] getSeverityBuckets(int resourceId, long begin, long end, int numBuckets) {
        try {
            return SerialUtility.prepare(eventManager.getSeverityBuckets(getSessionSubject(), resourceId, begin, end,
                numBuckets), "EventService.getSeverityBuckets");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public EventSeverity[] getSeverityBucketsForAutoGroup(int parentResourceId, int resourceTypeId, long begin,
        long end, int numBuckets) {
        try {
            return SerialUtility.prepare(eventManager.getSeverityBucketsForAutoGroup(getSessionSubject(),
                parentResourceId, resourceTypeId, begin, end, numBuckets),
                "EventService.getSeverityBucketsForAutoGroup");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public EventSeverity[] getSeverityBucketsForCompGroup(int resourceGroupId, long begin, long end, int numBuckets) {
        try {
            return SerialUtility.prepare(eventManager.getSeverityBucketsForCompGroup(getSessionSubject(),
                resourceGroupId, begin, end, numBuckets), "EventService.getSeverityBucketsForCompGroup");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<Event> findEventsByCriteria(EventCriteria criteria) {
        try {
            return SerialUtility.prepare(eventManager.findEventsByCriteria(getSessionSubject(), criteria),
                "EventService.findEventsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<EventComposite> findEventCompositesByCriteria(EventCriteria criteria) {
        try {
            return SerialUtility.prepare(eventManager.findEventCompositesByCriteria(getSessionSubject(), criteria),
                "EventService.findEventsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int deleteEventsForContext(EntityContext context, List<Integer> eventIds) {
        try {
            return eventManager.deleteEventsForContext(getSessionSubject(), context, eventIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public int purgeEventsForContext(EntityContext context) {
        try {
            return eventManager.purgeEventsForContext(getSessionSubject(), context);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

}
