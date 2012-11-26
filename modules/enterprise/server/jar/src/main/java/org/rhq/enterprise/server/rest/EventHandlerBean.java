/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.enterprise.server.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.rest.domain.EventRest;
import org.rhq.enterprise.server.rest.domain.EventSourceRest;

/**
 * Handle event related things
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class EventHandlerBean extends AbstractRestBean implements EventHandlerLocal {

    @EJB
    EventManagerLocal eventManager;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    EntityManager em;

    @Override
    public Response listEventSourcesForResource( int resourceId, Request request,
                                           HttpHeaders headers) {

        Resource res = fetchResource(resourceId);
        Set<EventSource> eventSources = res.getEventSources();

        List<EventSourceRest> restSources = new ArrayList<EventSourceRest>(eventSources.size());
        for (EventSource source : eventSources) {
            EventSourceRest esr = convertEventSource(source);
            restSources.add(esr);
        }


        Response.ResponseBuilder builder;
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            GenericEntity<List<EventSourceRest>> list = new GenericEntity<List<EventSourceRest>>(restSources) {};
            builder = Response.ok(list, mediaType);
        }
        else {
            builder = Response.ok(restSources, mediaType);
        }
        return builder.build();
    }

    @Override
    public EventSourceRest getEventSource(int sourceId) {

        EventSource source = findEventSourceById(sourceId);
        EventSourceRest esr = convertEventSource(source);

        return esr;
    }

    public EventSourceRest addEventSource(int resourceId, EventSourceRest esr) {

        Resource resource = fetchResource(resourceId);
        ResourceType rt = resource.getResourceType();
        Set<EventDefinition> eventDefinitions = rt.getEventDefinitions();
        EventDefinition eventDefinition=null;
        for (EventDefinition ed : eventDefinitions) {
            if (ed.getName().equals(esr.getName())) {
                eventDefinition = ed;
                break;
            }
        }
        if (eventDefinition==null) {
            throw new StuffNotFoundException("eventDefinition with name " + esr.getName());
        }
        //  check if a source with the given location already exists for the definition and resource
        Query q = em.createQuery("SELECT es FROM EventSource es WHERE es.location = :location AND es.eventDefinition = :definition AND es.resourceId = :resourceId");
        q.setParameter("location",esr.getLocation());
        q.setParameter("definition",eventDefinition);
        q.setParameter("resourceId",resourceId);
        List<EventSource> sources = q.getResultList();

        EventSource source;
        if (sources.isEmpty()) {
            source = new EventSource(esr.getLocation(),eventDefinition,resource);
            em.persist(source);
        } else if (sources.size()==1) {
            source = sources.get(0);
        } else {
            throw new IllegalStateException("We have more than one EventSource on the same Definition with the same location - must not happen");
        }

        EventSourceRest result = convertEventSource(source);
        return result;
    }

    @Override
    public Response deleteEventSource(int sourceId) {

        EventSource source = findEventSourceById(sourceId);
        em.remove(source); // We have a cascade delete on the events TODO make operation async ?

        return Response.ok().build();
    }

    @Override
    public Response getEventsForSource(int sourceId, long startTime,
                                       long endTime, String severity, Request request,
                                       HttpHeaders headers) {

        EventSource source = findEventSourceById(sourceId);

        EventCriteria criteria = new EventCriteria();
        criteria.addFilterSourceId(source.getId());
        if (startTime>0) {
            criteria.addFilterStartTime(startTime);
        }
        if (endTime>0) {
            criteria.addFilterEndTime(endTime);
        }
        if (startTime==0 && endTime==0) {
            PageControl pageControl = new PageControl();
            pageControl.setPageSize(200);
            criteria.setPageControl(pageControl);
        }

        Response.ResponseBuilder builder = getEventsAsBuilderForCriteria(headers, criteria);

        return builder.build();
    }


    @Override
    public Response getEventsForResource(int resourceId,
                                         long startTime,
                                         long endTime, String severity, Request request,
                                         HttpHeaders headers) {

        EventCriteria criteria = new EventCriteria();
        criteria.addFilterResourceId(resourceId);
        if (startTime>0) {
            criteria.addFilterStartTime(startTime);
        }
        if (endTime>0) {
            criteria.addFilterEndTime(endTime);
        }
        if (startTime==0 && endTime==0) {
            PageControl pageControl = new PageControl();
            pageControl.setPageSize(200);
            criteria.setPageControl(pageControl);
        }

        Response.ResponseBuilder builder = getEventsAsBuilderForCriteria(headers, criteria);

        return builder.build();

    }

    public Response addEventsToSource(int sourceId, List<EventRest> eventRest) {

        EventSource source = findEventSourceById(sourceId);
        Map<EventSource,Set<Event>> eventMap = new HashMap<EventSource, Set<Event>>();
        Set<Event> events = new HashSet<Event>(eventRest.size());
        for (EventRest eRest : eventRest) {
            EventSeverity eventSeverity = EventSeverity.valueOf(eRest.getSeverity());
            Event event = new Event(eRest.getTimestamp(),eventSeverity,source,eRest.getDetail());
            events.add(event);
        }
        eventMap.put(source,events);
        eventManager.addEventData(eventMap);

        return Response.noContent().build();
    }


    private Response.ResponseBuilder getEventsAsBuilderForCriteria(HttpHeaders headers, EventCriteria criteria) {
        List<Event> eventList = eventManager.findEventsByCriteria(caller,criteria);
        List<EventRest> restEvents = new ArrayList<EventRest>(eventList.size());
        for (Event event : eventList) {
            restEvents.add(convertEvent(event));
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            GenericEntity<List<EventRest>> list = new GenericEntity<List<EventRest>>(restEvents) {};
            builder = Response.ok(list, mediaType);
        }
        else {
            builder = Response.ok(restEvents, mediaType);
        }
        return builder;
    }

    private EventSourceRest convertEventSource(EventSource source) {
        EventSourceRest esr = new EventSourceRest();
        esr.setId(source.getId());
        esr.setDescription(source.getEventDefinition().getDescription());
        esr.setDisplayName(source.getEventDefinition().getDisplayName());
        esr.setName(source.getEventDefinition().getName());
        esr.setLocation(source.getLocation());
        esr.setResourceId(source.getResourceId());

        return esr;
    }

    private EventRest convertEvent(Event event) {
        EventRest er = new EventRest();
        er.setDetail(event.getDetail());
        er.setId(event.getId());
        er.setSeverity(event.getSeverity().toString());
        er.setTimestamp(event.getTimestamp());
        er.setSourceId(event.getSource().getId());

        return er;
    }

    private EventSource findEventSourceById(int sourceId) {

        EventSource source = em.find(EventSource.class,sourceId);
        if (source==null)
            throw new StuffNotFoundException("Event source with id " + sourceId);
        return source;

    }

}
