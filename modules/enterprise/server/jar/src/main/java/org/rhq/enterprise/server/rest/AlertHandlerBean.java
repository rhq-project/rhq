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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.rest.domain.*;

/**
 * Deal with alert related stuff
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml","text/plain"})
@Path("/alert")
@Api(value = "Deal with Alerts",description = "This api deals with alerts that have fired. It does not offer to create/update AlertDefinitions (yet). Everything " +
    "related to creating / updating Alert Definitions is purely experimental at the moment and can change without notice at any time.")
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class AlertHandlerBean extends AbstractRestBean {

//    private final Log log = LogFactory.getLog(AlertHandlerBean.class);

    @EJB
    AlertManagerLocal alertManager;

    @EJB
    AlertDefinitionManagerLocal alertDefinitionManager;

    @EJB
    AlertNotificationManagerLocal notificationMgr;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;



    @GZIP
    @GET
    @Path("/")
    @ApiOperation(value = "List all alerts", multiValueResponse = true, responseClass = "List<AlertRest>")
    public Response listAlerts(
            @ApiParam(value = "Page number", defaultValue = "1") @QueryParam("page") int page,
            @ApiParam(value = "Limit to priority", allowableValues = "High, Medium, Low, All") @DefaultValue("All") @QueryParam("prio") String prio,
            @ApiParam(value = "Should full resources and definitions be sent") @QueryParam("slim") @DefaultValue(
                    "false") boolean slim,
            @ApiParam(
                    value = "If non-null only send alerts that have fired after this time, time is millisecond since epoch")
            @QueryParam("since") Long since,
            @ApiParam(value = "Id of a resource to limit search for") @QueryParam("resourceId") Integer resourceId,
            @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers) {


        AlertCriteria criteria = new AlertCriteria();
        criteria.setPaging(page,20); // TODO implement linking to next page
        if (since!=null) {
            criteria.addFilterStartTime(since);
        }

        if (resourceId!=null) {
            criteria.addFilterResourceIds(resourceId);
        }

        if (!prio.equals("All")) {
            AlertPriority alertPriority = AlertPriority.valueOf(prio.toUpperCase());
            criteria.addFilterPriorities(alertPriority);
        }
        criteria.addSortCtime(PageOrdering.DESC);

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

    @GET
    @Path("count")
    @ApiOperation("Return a count of alerts in the system depending on criteria")
    public int countAlerts(@ApiParam(value = "If non-null only send alerts that have fired after this time, time is millisecond since epoch")
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

        return count;
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
    @ApiOperation(value = "Return the notification logs for the given alert")
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
    @ApiOperation(value = "Remove the alert from the lit of alerts")
    public void purgeAlert(@ApiParam(value = "Id of the alert to remove") @PathParam("id") int id) {
        alertManager.deleteAlerts(caller, new int[]{id});

    }

    @GET
    @Cache(maxAge = 300)
    @Path("/{id}/definition")
    @ApiOperation("Get the alert definition (basics) for the alert")
    public AlertDefinitionRest getDefinitionForAlert(@ApiParam("Id of the alert to show the definition") @PathParam("id") int alertId) {
        Alert al = findAlertWithId(alertId);
        AlertDefinition def = al.getAlertDefinition();
        AlertDefinitionRest ret = definitionToDomain(def, false); // TODO allow 'full' parameter?
        return ret;
    }

    @GET
    @Path("/definition")
    public Response redirectDefinitionToDefinitions(@Context UriInfo uriInfo) {
        UriBuilder uriBuilder = uriInfo.getRequestUriBuilder();
        uriBuilder.replacePath("/rest/alert/definitions"); // TODO there needs to be a better way
        Response.ResponseBuilder builder = Response.seeOther(uriBuilder.build());
        return builder.build();
    }

    @GZIP
    @GET
    @Path("/definitions")
    @ApiOperation("List all Alert Definition")
    public List<AlertDefinitionRest> listAlertDefinitions(
            @ApiParam(value = "Page number", defaultValue = "0") @QueryParam("page") int page,
            @ApiParam(value = "Limit to status, UNUSED AT THE MOMENT ") @QueryParam("status") String status) {

        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.setPaging(page,20); // TODO add link to next page
        List<AlertDefinition> defs = alertDefinitionManager.findAlertDefinitionsByCriteria(caller, criteria);
        List<AlertDefinitionRest> ret = new ArrayList<AlertDefinitionRest>(defs.size());
        for (AlertDefinition def : defs) {
            AlertDefinitionRest adr = definitionToDomain(def, false);
            ret.add(adr);
        }
        return ret;
    }

    @GET
    @Path("/definition/{id}")
    @ApiOperation(value = "Get one AlertDefinition by id", responseClass = "AlertDefinitionRest")
    public Response getAlertDefinition(@ApiParam("Id of the alert definition to retrieve") @PathParam("id") int definitionId,
                                       @ApiParam("Should conditions be returned too?") @QueryParam("full") @DefaultValue("false") boolean full,
            @Context Request request) {

        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller, definitionId);
        if (def==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId );

        EntityTag eTag = new EntityTag(Integer.toHexString(def.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {
            AlertDefinitionRest adr = definitionToDomain(def, full);
            builder = Response.ok(adr);
        }
        builder.tag(eTag);

        return builder.build();
    }

    @POST
    @Path("/definitions")
    @ApiOperation("Create an AlertDefinition for the resource passed as query param")
    public Response createAlertDefinitionForResource(@QueryParam("resourceId") int resourceId, AlertDefinitionRest adr,
        @Context UriInfo uriInfo) {

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName(adr.getName());
        alertDefinition.setEnabled(adr.isEnabled());
        alertDefinition.setPriority(AlertPriority.valueOf(adr.getPriority().toUpperCase()));
        alertDefinition.setConditionExpression(BooleanExpression.valueOf(adr.getConditionMode().toUpperCase()));
        alertDefinition.setRecoveryId(adr.getRecoveryId());

        Set<AlertCondition> conditions = new HashSet<AlertCondition>(adr.getConditions().size());
        for (AlertConditionRest acr : adr.getConditions()) {
            AlertCondition condition = conditionRestToCondition(acr);
            conditions.add(condition);
        }
        alertDefinition.setConditions(conditions);

        List<AlertNotification> notifications = new ArrayList<AlertNotification>(adr.getNotifications().size());
        for (AlertNotificationRest anr : adr.getNotifications()) {
            AlertNotification notification = new AlertNotification(anr.getSenderName());
            // TODO validate sender
            notification.setAlertDefinition(alertDefinition);
            Configuration configuration = new Configuration();
            for (Map.Entry<String,Object> entry: anr.getConfig().entrySet()) {
                configuration.put(new PropertySimple(entry.getKey(),entry.getValue()));
            }
            notification.setConfiguration(configuration);
            // TODO extra configuration (?)

            notifications.add(notification);
        }

        alertDefinition.setAlertNotifications(notifications);


        // TODO for all things below
        alertDefinition.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));




        int definitionId = alertDefinitionManager.createAlertDefinition(caller,alertDefinition,resourceId,false);

        AlertDefinition updatedDefinition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        AlertDefinitionRest uadr = definitionToDomain(updatedDefinition,true) ; // TODO param 'full'

        uadr.setId(definitionId);

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/alert/definition/{id}");
        URI uri = uriBuilder.build(definitionId);


        Response.ResponseBuilder builder = Response.created(uri);
        builder.entity(uadr);
        return builder.build();
    }

    @PUT
    @Path("/definition/{id}")
    @ApiOperation(value = "Update the alert definition (priority, enablement)", notes = "Priority must be HIGH,LOW,MEDIUM")
    public Response updateDefinition(
            @ApiParam("Id of the alert definition to update") @PathParam("id") int definitionId,
            AlertDefinitionRest definitionRest) {
        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (def==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        def.setEnabled(definitionRest.isEnabled());
        def.setPriority(AlertPriority.valueOf(definitionRest.getPriority()));

        def = alertDefinitionManager.updateAlertDefinition(caller,def.getId(),def,false); // TODO set to true once we allow to change any/all

        EntityTag eTag = new EntityTag(Integer.toHexString(def.hashCode()));
        AlertDefinitionRest adr = definitionToDomain(def, false);

        Response.ResponseBuilder builder = Response.ok(adr);
        builder.tag(eTag);

        return builder.build();

    }

    @DELETE
    @Path("definition/{id}")
    @ApiOperation("Delete an alert definition")
    public Response deleteDefinition(@PathParam("id") int definitionId) {

        int count = alertDefinitionManager.removeAlertDefinitions(caller, new int[]{definitionId});

        return Response.noContent().build();
    }


    @POST
    @Path("definition/{id}/conditions")
    public Response addConditionToDefinition(@PathParam("id") int definitionId, AlertConditionRest conditionRest, @Context UriInfo uriInfo) {

        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (definition==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        AlertCondition condition = conditionRestToCondition(conditionRest);

        definition.addCondition(condition);

        alertDefinitionManager.updateAlertDefinition(caller,definitionId,definition,false);

        AlertDefinition updatedDefinition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        Set<AlertCondition> conditions = updatedDefinition.getConditions();
        int conditionId=-1;
        for (AlertCondition cond :conditions) {
            if (cond.getName().equals(condition.getName()))
                conditionId = cond.getId();
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/alert/definition/{id}/condition/{cid}");
        URI uri = uriBuilder.build(definitionId,conditionId);

        Response.ResponseBuilder builder = Response.created(uri);

        return builder.build();

    }

    private AlertCondition conditionRestToCondition(AlertConditionRest conditionRest) {
        AlertCondition condition = new AlertCondition();
        condition.setName(conditionRest.getName().name());
        condition.setCategory(AlertConditionCategory.valueOf(conditionRest.getCategory().getName()));
        return condition;
    }

    @POST
    @Path("definition/{id}/notifications")
    public Response addNotificationToDefinition(@PathParam("id") int definitionId, AlertNotificationRest notificationRest, @Context UriInfo uriInfo) {

        AlertNotification notification = new AlertNotification(notificationRest.getSenderName());

        // first check if the sender by name exists
        AlertSenderPluginManager pluginManager = alertManager.getAlertPluginManager();
        if (pluginManager.getAlertSenderForNotification(notification)==null) {
            throw new StuffNotFoundException("AlertSender with name [" + notificationRest.getSenderName() +"]");
        }

        // Now check if the definition exists as well
        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (definition==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        // definition and sender are valid, continue
        int existingNotificationCount = definition.getAlertNotifications().size();

//        notification.setAlertDefinition(definition); setting this will result in duplicated notifications
        definition.addAlertNotification(notification);

        Configuration configuration = new Configuration();
        for (Map.Entry<String,Object> entry: notificationRest.getConfig().entrySet()) {
            configuration.put(new PropertySimple(entry.getKey(),entry.getValue()));
        }
        notification.setConfiguration(configuration);
        // TODO extra configuration (?)


        alertDefinitionManager.updateAlertDefinition(caller, definitionId, definition, false);


        AlertDefinition updatedDefinition = alertDefinitionManager.getAlertDefinition(caller,definitionId);

        List<AlertNotification> notifs = updatedDefinition.getAlertNotifications();

        assert notifs.size() == existingNotificationCount +1;

        AlertNotification updatedNotification = notifs.get(existingNotificationCount);
        AlertNotificationRest updatedNotificationRest = notificationToNotificationRest(updatedNotification);

        int notificationId = updatedNotification.getId();

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/alert/definition/{id}/notification/{nid}");
        URI uri = uriBuilder.build(definitionId, notificationId );

        Response.ResponseBuilder builder = Response.created(uri);
        builder.entity(updatedNotificationRest);

        return builder.build();

    }

    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Return a list of alert notification senders")
    @GET @GZIP
    @Path("senders")
    public Response getAlertSenders(@Context UriInfo uriInfo) {

        List<String> senderNames = notificationMgr.listAllAlertSenders();
        List<AlertSender> senderList = new ArrayList<AlertSender>(senderNames.size());
        for (String senderName : senderNames) {
            AlertSenderInfo info = notificationMgr.getAlertInfoForSender(senderName);
            AlertSender sender = new AlertSender(senderName);
            sender.setDescription(info.getDescription());
            senderList.add(sender);
        }

        Response.ResponseBuilder builder = Response.ok(senderList); // TODO XML

        return builder.build();

    }

    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Return an alert notification senders by name")
    @GET @GZIP
    @Path("sender/{name}")
    public Response getAlertSenderByName(@PathParam("name")String senderName, @Context UriInfo uriInfo) {

        AlertSenderInfo info = notificationMgr.getAlertInfoForSender(senderName);
        if (info==null) {
            throw new StuffNotFoundException("Alert sender with name [" + senderName + "]");
        }
        AlertSender sender = new AlertSender(senderName);
        sender.setDescription(info.getDescription());

        Response.ResponseBuilder builder = Response.ok(sender); // TODO XML

        return builder.build();

    }


    private AlertDefinitionRest definitionToDomain(AlertDefinition def, boolean full) {
        AlertDefinitionRest adr = new AlertDefinitionRest(def.getId());
        adr.setName(def.getName());
        adr.setEnabled(def.getEnabled());
        adr.setPriority(def.getPriority().getName());
        adr.setConditionMode(def.getConditionExpression().toString());

        Set<AlertCondition> conditions = def.getConditions();
        if (full && conditions.size()>0) {
            List<AlertConditionRest> conditionRestList = new ArrayList<AlertConditionRest>(conditions.size());
            for (AlertCondition condition : conditions) {
                AlertConditionRest acr = conditionToConditionRest(condition);
                conditionRestList.add(acr);
            }
            adr.setConditions(conditionRestList);
        }
        List<AlertNotification> notifications = def.getAlertNotifications();
        if (full && notifications.size()>0) {
            List<AlertNotificationRest> notificationRestList = new ArrayList<AlertNotificationRest>(notifications.size());
            for (AlertNotification notification : notifications) {
                AlertNotificationRest anr = notificationToNotificationRest(notification);
                notificationRestList.add(anr);
            }
            adr.setNotifications(notificationRestList);
        }

        return adr;
    }

    private AlertNotificationRest notificationToNotificationRest(AlertNotification notification) {
        AlertNotificationRest anr = new AlertNotificationRest();
        anr.setId(notification.getId());
        anr.setSenderName(notification.getSenderName());

        for (Map.Entry<String, PropertySimple> entry : notification.getConfiguration().getSimpleProperties().entrySet()) {
            anr.getConfig().put(entry.getKey(),entry.getValue().getStringValue()); // TODO correct type conversion of 2nd argument
        }
        // TODO Extra Configuration

        return anr;
    }

    private AlertConditionRest conditionToConditionRest(AlertCondition condition) {
        AlertConditionRest acr = new AlertConditionRest();
        acr.setId(condition.getId());
        acr.setName(AlertConditionOperator.valueOf(condition.getName()));
        acr.setCategory(condition.getCategory());
        acr.setOption(condition.getOption());
        // TODO measurement definition
        acr.setThreshold(condition.getThreshold());
        acr.setTriggerId(condition.getTriggerId()); // TODO what's that?

        return acr;
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
            alertDefinitionRest = definitionToDomain(alertDefinition, false);
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
