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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.rest.domain.AlertDefinitionRest;
import org.rhq.enterprise.server.rest.domain.AlertRest;
import org.rhq.enterprise.server.rest.domain.IntegerValue;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;
import org.rhq.enterprise.server.rest.domain.StringValue;

/**
 * Deal with alert related stuff
 * @author Heiko W. Rupp
 */
@Path("/alert")
@Api(value = "Deal with Alerts",description = "This api deals with alerts that have fired.")
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class AlertHandlerBean extends AbstractRestBean {

    @EJB
    private AlertManagerLocal alertManager;


    @GZIP
    @GET
    @Path("/")
    @ApiOperation(value = "List all alerts, possibly limiting by resource or alert definition, priority and start time", multiValueResponse = true, responseClass = "List<AlertRest>")
    @ApiErrors({
        @ApiError(code = 406, reason = "There are 'resourceId' and 'definitionId' passed as query parameters"),
        @ApiError(code = 406, reason = "Page size was 0"),
        @ApiError(code = 406, reason = "Page number was < 0")
    })
    public Response listAlerts(
        @ApiParam(value = "Page number") @QueryParam("page") @DefaultValue("0") int page,
        @ApiParam(value = "Page size; use -1 for 'unlimited'") @QueryParam("size") @DefaultValue("100")int size,
        @ApiParam(value = "Limit to priority", allowableValues = "High, Medium, Low, All") @DefaultValue("All") @QueryParam("prio") String prio,
        @ApiParam(value = "Should full resources and definitions be sent") @QueryParam("slim") @DefaultValue("false") boolean slim,
        @ApiParam(value = "If non-null only send alerts that have fired after this time, time is millisecond since epoch") @QueryParam("since") Long since,
        @ApiParam(value = "Id of a resource to limit search for") @QueryParam("resourceId") Integer resourceId,
        @ApiParam(value = "If of an alert definition to search for") @QueryParam("definitionId") Integer definitionId,
        @ApiParam(value = "Should only unacknowledged alerts be sent") @QueryParam("unacknowledgedOnly") @DefaultValue("false") boolean unacknowledgedOnly,
        @ApiParam(value = "Should not display any recovered alerts") @QueryParam("filter_recovered") @DefaultValue("false") boolean noRecovered,
        @ApiParam(value = "Should not display any recovery alerts") @QueryParam("filter_recoverytypes") @DefaultValue("false") boolean noRecoveryType,
        @ApiParam(value = "Display only alerts matching this name filter") @QueryParam("filter_name") @DefaultValue("") String name,
        @Context UriInfo uriInfo, @Context HttpHeaders headers) {

        if (resourceId!=null && definitionId!=null) {
            throw new BadArgumentException("At most one of 'resourceId' and 'definitionId' may be given");
        }
        if (size==0) {
            throw new BadArgumentException("size","Must not be 0");
        }
        if (page<0) {
            throw new BadArgumentException("page","Must be >=1");
        }

        AlertCriteria criteria = new AlertCriteria();

        if (size==-1) {
            PageControl pageControl = PageControl.getUnlimitedInstance();
            pageControl.setPageNumber(page);
            criteria.setPageControl(pageControl);
        }
        else {
            criteria.setPaging(page, size);
        }

        if (since!=null) {
            criteria.addFilterStartTime(since);
        }

        if (resourceId!=null) {
            criteria.addFilterResourceIds(resourceId);
        }
        if (definitionId!=null) {
            criteria.addFilterAlertDefinitionIds(definitionId);
        }

        if (!prio.equals("All")) {
            AlertPriority alertPriority = AlertPriority.valueOf(prio.toUpperCase());
            criteria.addFilterPriorities(alertPriority);
        }

        if(name != null && name.length() > 0) {
            criteria.addFilterName(name);
        }

        if (unacknowledgedOnly) {
            criteria.addFilterUnacknowledgedOnly(Boolean.TRUE);
        }

        criteria.addFilterRecovered(noRecovered);

        if(noRecoveryType) {
            criteria.addFilterRecoveryIds(Integer.valueOf(0));
        }

        criteria.addSortCtime(PageOrdering.DESC);

        PageList<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        List<AlertRest> ret = new ArrayList<AlertRest>(alerts.size());
        for (Alert al : alerts) {
            AlertRest ar = alertToDomain(al, uriInfo, slim);
            ret.add(ar);
        }

        MediaType type = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder = Response.ok();
        builder.type(type);

        if (type.equals(MediaType.TEXT_HTML_TYPE)) {
            builder.entity(renderTemplate("listAlerts.ftl",ret));
        } else {
            if (type.equals(wrappedCollectionJsonType)) {
                wrapForPaging(builder,uriInfo,alerts,ret);
            }
            else {
                GenericEntity<List<AlertRest>> entity = new GenericEntity<List<AlertRest>>(ret) {};
                builder.entity(entity);
                createPagingHeader(builder,uriInfo,alerts);
            }
        }

        return builder.build();
    }

    @GET
    @Path("count")
    @ApiOperation("Return a count of alerts in the system depending on criteria")
    public IntegerValue countAlerts(@ApiParam(value = "If non-null only send alerts that have fired after this time, time is millisecond since epoch")
                        @QueryParam("since") Long since) {

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

        return new IntegerValue(count);
    }

    @GET
    @Cache(maxAge = 60)
    @Path("/{id}")
    @ApiOperation(value = "Get one alert with the passed id", responseClass = "AlertRest")
    public Response getAlert(
            @ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
            @ApiParam(value = "Should full resources and definitions be sent") @QueryParam("slim") @DefaultValue("false") boolean slim,
            @Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {

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

    @GET
    @Path("/{id}/conditions")
    @Cache(maxAge = 300)
    @ApiOperation(value = "Return the condition logs for the given alert")
    public Response getConditionLogs(@ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
                                  @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers) {

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

    @GET
    @Path("/{id}/notifications")
    @Cache(maxAge = 60)
    @ApiOperation(value = "Return the notification logs for the given alert")
    public Response getNotificationLogs(@ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
                                     @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
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

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "Mark the alert as acknowledged (by the caller)", notes = "Returns a slim version of the alert")
    public AlertRest ackAlert(@ApiParam(value = "Id of the alert to acknowledge") @PathParam("id") int id, @Context UriInfo uriInfo) {
        findAlertWithId(id); // Ensure the alert exists
        int count = alertManager.acknowledgeAlerts(caller, new int[]{id});

        // TODO this is not reliable due to Tx constraints ( the above may only run after this ackAlert() method has finished )

        Alert al = findAlertWithId(id);
        AlertRest ar = alertToDomain(al, uriInfo, true);
        return ar;
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Remove the alert from the list of alerts", notes = "This operation is by default idempotent, returning 204." +
                "If you want to check if the alert existed at all, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Alert was deleted or did not exist with validation not set"),
        @ApiError(code = 404, reason = "Alert did not exist and validate was set")
    })
    public Response purgeAlert(@ApiParam(value = "Id of the alert to remove") @PathParam("id") int id,
                           @ApiParam("Validate if the alert exists") @QueryParam("validate") @DefaultValue("false") boolean validate) {
        int count = alertManager.deleteAlerts(caller, new int[]{id});

        if (count == 0 && validate) {
            throw new StuffNotFoundException("Alert with id " + id);
        }
        return Response.noContent().build();
    }

    @GET
    @Cache(maxAge = 300)
    @Path("/{id}/definition")
    @ApiOperation("Get the alert definition (basics) for the alert")
    public AlertDefinitionRest getDefinitionForAlert(@ApiParam("Id of the alert to show the definition") @PathParam("id") int alertId,
                                                     @Context UriInfo uriInfo) {
        Alert al = findAlertWithId(alertId);
        AlertDefinition def = al.getAlertDefinition();
        AlertDefinitionHandlerBean adhb = new AlertDefinitionHandlerBean();
        AlertDefinitionRest ret = adhb.definitionToDomain(def, false, uriInfo); // TODO allow 'full' parameter?
        return ret;
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
        if (alerts.isEmpty()) {
            throw new StuffNotFoundException("Alert with id " + id);
        }

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
            AlertDefinitionHandlerBean adhb = new AlertDefinitionHandlerBean();
            alertDefinitionRest = adhb.definitionToDomain(alertDefinition, false, uriInfo);
        }
        ret.setAlertDefinition(alertDefinitionRest);
        ret.setDefinitionEnabled(alertDefinition.getEnabled());
        if (al.getAcknowledgingSubject()!=null) {
            ret.setAckBy(al.getAcknowledgingSubject());
            ret.setAckTime(al.getAcknowledgeTime());
        }
        ret.setAlertTime(al.getCtime());
        ret.setDescription(alertManager.prettyPrintAlertConditions(al,false));
        ret.setRecoveryTime(al.getRecoveryTime());

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

        int resourceId = alertDefinition.getResource().getId();
        ret.addLink(createUILink(uriInfo,UILinkTemplate.RESOURCE_ALERT,resourceId,al.getId()));

        return ret;
    }
}
