/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.rest.domain.*;

/**
 * Deal with alert related stuff
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class AlertHandlerBean extends AbstractRestBean implements AlertHandlerLocal {

//    private final Log log = LogFactory.getLog(AlertHandlerBean.class);

    @EJB
    AlertManagerLocal alertManager;

    @EJB
    AlertDefinitionManagerLocal alertDefinitionManager;


    @Override
    public Response listAlerts(int page, String status, boolean slim, Long since, Request request, UriInfo uriInfo, HttpHeaders headers) {


        AlertCriteria criteria = new AlertCriteria();
        criteria.setPaging(page,20); // TODO implement linking to next page
        if (since!=null) {
            criteria.addFilterStartTime(since);
        }
        PageList<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        List<AlertRest> ret = new ArrayList<AlertRest>(alerts.size());
        for (Alert al : alerts) {
            AlertRest ar = alertToDomain(al, uriInfo, slim);
            ret.add(ar);
        }

        MediaType type = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (type.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listAlerts.ftl",ret),type);
        } else {
            GenericEntity<List<AlertRest>> entity = new GenericEntity<List<AlertRest>>(ret) {};
            builder = Response.ok(entity);
        }

        return builder.build();
    }

    @Override
    public int countAlerts(Long since) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        criteria.fetchAlertDefinition(false);
        criteria.fetchConditionLogs(false);
        criteria.fetchRecoveryAlertDefinition(false);
        criteria.fetchNotificationLogs(false);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        if (since!=null) {
            criteria.addFilterStartTime(since);
        }
        PageList<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        int count = alerts.getTotalSize();

        return count;
    }

    @Override
    public Response getAlert(int id, boolean slim, UriInfo uriInfo, Request request, HttpHeaders headers) {

        Alert al = findAlertWithId(id);
        MediaType type = headers.getAcceptableMediaTypes().get(0);

        EntityTag eTag = new EntityTag(Integer.toHexString(al.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {
            AlertRest ar = alertToDomain(al, uriInfo, slim);
            if (type.equals(MediaType.TEXT_HTML_TYPE)) {
                builder = Response.ok(renderTemplate("alert.ftl",ar),type);
            } else {
                builder = Response.ok(ar);
            }
        }
        builder.tag(eTag);

        return builder.build();
    }

    @Override
    public Response getConditionLogs(int id, Request request, UriInfo uriInfo, HttpHeaders headers) {

        Alert al = findAlertWithId(id);
        Set<AlertConditionLog> conditions =  al.getConditionLogs();
        MediaType type = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        if (type.equals(MediaType.APPLICATION_XML_TYPE)) {
            List<StringValue> result = new ArrayList<StringValue>(conditions.size());
            for (AlertConditionLog log : conditions) {
                AlertCondition condition = log.getCondition();
                String entry = String.format("category='%s', name='%s', comparator='%s', threshold='%s', option='%s' : %s",
                        condition.getCategory(), condition.getName(), condition.getComparator(), condition.getThreshold(), condition.getOption(), log.getValue() );
                StringValue sv = new StringValue(entry);
                result.add(sv);
            }
            GenericEntity<List<StringValue>> entity = new GenericEntity<List<StringValue>>(result){};
            builder = Response.ok(entity);
        }
        else {
            List<String> result = new ArrayList<String>(conditions.size());

            for (AlertConditionLog log : conditions) {
                AlertCondition condition = log.getCondition();
                String entry = String.format("category='%s', name='%s', comparator='%s', threshold='%s', option='%s' : %s",
                        condition.getCategory(), condition.getName(), condition.getComparator(), condition.getThreshold(), condition.getOption(), log.getValue() );
                result.add(entry);
            }
            if (type.equals(MediaType.TEXT_HTML_TYPE)) {
                builder = Response.ok(renderTemplate("genericStringList.ftl",result),type);
            } else {
                builder = Response.ok(result);
            }
        }

        return builder.build();

    }

    @Override
    public Response getNotificationLogs(int id, Request request, UriInfo uriInfo, HttpHeaders headers) {
        Alert al = findAlertWithId(id);
        MediaType type = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;

        List<AlertNotificationLog> notifications =  al.getAlertNotificationLogs();
        if (type.equals(MediaType.APPLICATION_XML_TYPE)) {
            List<StringValue> result = new ArrayList<StringValue>(notifications.size());
            for (AlertNotificationLog log : notifications) {
                String entry = log.getSender() + ": " + log.getResultState() + ": " + log.getMessage();
                StringValue sv = new StringValue(entry);
                result.add(sv);
            }

            GenericEntity<List<StringValue>> entity = new GenericEntity<List<StringValue>>(result){};
            builder = Response.ok(entity);
        } else {
            List<String> result = new ArrayList<String>(notifications.size());
            for (AlertNotificationLog log : notifications) {
                String entry = log.getSender() + ": " + log.getResultState() + ": " + log.getMessage();
                result.add(entry);
            }
            if (type.equals(MediaType.TEXT_HTML_TYPE)) {
                builder = Response.ok(renderTemplate("genericStringList.ftl",result),type);
            } else {
                builder = Response.ok(result);
            }
        }

        return builder.build();
    }

    @Override
    public AlertRest ackAlert(int id, UriInfo uriInfo) {
        findAlertWithId(id); // Ensure the alert exists
        int count = alertManager.acknowledgeAlerts(caller,new int[]{id});

        // TODO this is not reliable due to Tx constraints ( the above may only run after this ackAlert() method has finished )

        Alert al = findAlertWithId(id);
        AlertRest ar = alertToDomain(al, uriInfo, true);
        return ar;
    }

    @Override
    public void purgeAlert(int id) {
        alertManager.deleteAlerts(caller, new int[]{id});

    }

    @Override
    public AlertDefinitionRest getDefinitionForAlert(int alertId) {
        Alert al = findAlertWithId(alertId);
        AlertDefinition def = al.getAlertDefinition();
        AlertDefinitionRest ret = definitionToDomain(def);
        return ret;
    }

    @Override
    public List<AlertDefinitionRest> listAlertDefinitions(int page, String status) {

        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.setPaging(page,20); // TODO add link to next page
        List<AlertDefinition> defs = alertDefinitionManager.findAlertDefinitionsByCriteria(caller, criteria);
        List<AlertDefinitionRest> ret = new ArrayList<AlertDefinitionRest>(defs.size());
        for (AlertDefinition def : defs) {
            AlertDefinitionRest adr = definitionToDomain(def);
            ret.add(adr);
        }
        return ret;
    }

    @Override
    public Response getAlertDefinition(int definitionId, Request request) {

        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (def==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId );

        EntityTag eTag = new EntityTag(Integer.toHexString(def.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {
            AlertDefinitionRest adr = definitionToDomain(def);
            builder = Response.ok(adr);
        }
        builder.tag(eTag);

        return builder.build();
    }

    @Override
    public Response updateDefinition(int definitionId, AlertDefinitionRest definitionRest, Request request) {
        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (def==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        def.setEnabled(definitionRest.isEnabled());
        def.setPriority(AlertPriority.valueOf(definitionRest.getPriority()));

        def = alertDefinitionManager.updateAlertDefinition(caller,def.getId(),def,false);

        EntityTag eTag = new EntityTag(Integer.toHexString(def.hashCode()));
        AlertDefinitionRest adr = definitionToDomain(def);

        Response.ResponseBuilder builder = Response.ok(adr);
        builder.tag(eTag);

        return builder.build();

    }

    private AlertDefinitionRest definitionToDomain(AlertDefinition def) {
        AlertDefinitionRest adr = new AlertDefinitionRest(def.getId());
        adr.setName(def.getName());
        adr.setEnabled(def.getEnabled());
        adr.setPriority(def.getPriority().getName());

        return adr;
    }

    /**
     * Retrieve the alert with id id.
     * @param id Primary key of the alert
     * @return Alert domain object
     * @throws StuffNotFoundException if no such alert exists in the system.
     */
    private Alert findAlertWithId(int id) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterId(id);
        List<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        if (alerts.isEmpty())
            throw new StuffNotFoundException("Alert with id " + id);

        return alerts.get(0);
    }

    public AlertRest alertToDomain(Alert al, UriInfo uriInfo, boolean slim) {
        AlertRest ret = new AlertRest();
        ret.setId(al.getId());
        AlertDefinition alertDefinition = al.getAlertDefinition();
        ret.setName(alertDefinition.getName());
        AlertDefinitionRest alertDefinitionRest;
        if (slim) {
            alertDefinitionRest = new AlertDefinitionRest(alertDefinition.getId());
        } else {
            alertDefinitionRest = definitionToDomain(alertDefinition);
        }
        ret.setAlertDefinition(alertDefinitionRest);
        ret.setDefinitionEnabled(alertDefinition.getEnabled());
        if (al.getAcknowledgingSubject()!=null) {
            ret.setAckBy(al.getAcknowledgingSubject());
            ret.setAckTime(al.getAcknowledgeTime());
        }
        ret.setAlertTime(al.getCtime());
        ret.setDescription(alertManager.prettyPrintAlertConditions(al,false));

        Resource r = fetchResource(alertDefinition.getResource().getId());
        ResourceWithType rwt;
        if (slim) {
            rwt = new ResourceWithType(r.getName(),r.getId());
        } else {
            rwt = fillRWT(r,uriInfo);
        }
        ret.setResource(rwt);

        // add some links
        UriBuilder builder = uriInfo.getBaseUriBuilder();
        builder.path("/alert/{id}/conditions");
        URI uri = builder.build(al.getId());
        Link link = new Link("conditions",uri.toString());
        ret.addLink(link);
        builder = uriInfo.getBaseUriBuilder();
        builder.path("/alert/{id}/notifications");
        uri = builder.build(al.getId());
        link = new Link("notification",uri.toString());
        ret.addLink(link);
        builder = uriInfo.getBaseUriBuilder();
        builder.path("/alert/{id}/definition");
        uri = builder.build(al.getId());
        link = new Link("definition",uri.toString());
        ret.addLink(link);


        return ret;
    }
}
