/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.rest.domain.AlertConditionRest;
import org.rhq.enterprise.server.rest.domain.AlertDefinitionRest;
import org.rhq.enterprise.server.rest.domain.AlertNotificationRest;
import org.rhq.enterprise.server.rest.domain.AlertSender;
import org.rhq.enterprise.server.rest.domain.Link;

/**
 * Deal with Alert Definitions. Note that this class shares the /alert/ sub-context with the
 * AlertHandlerBean
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml","text/plain"})
@Path("/alert")
@Api(value = "Deal with Alert Definitions",description = "This api deals with alert definitions. Everything " +
    " is purely experimental at the moment and can change without notice at any time.")
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class AlertDefinitionHandlerBean extends AbstractRestBean {

    @EJB
    AlertDefinitionManagerLocal alertDefinitionManager;

    @EJB
    AlertNotificationManagerLocal notificationMgr;

    @EJB
    AlertConditionManagerLocal conditionMgr;

    @EJB
    AlertManagerLocal alertManager;

    @EJB
    ResourceGroupManagerLocal resourceGroupMgr;

    @EJB
    ResourceTypeManagerLocal resourceTypeMgr;


    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;



    // Redirect from /definition to /definitions for GET requests
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
    @ApiOperation("Create an AlertDefinition for the resource/group/resource type passed as query param. One and only one of the three params must be given at any time.")
    public Response createAlertDefinition(@ApiParam("The id of the resource to attach the definition to") @QueryParam("resourceId") Integer resourceId,
                                          @ApiParam("The id of the group to attach the definition to") @QueryParam("groupId") Integer groupId,
                                          @ApiParam("The id of the resource type to attach the definition to") @QueryParam("resourceTypeId") Integer resourceTypeId,
                                          @ApiParam("The data for the new definition") AlertDefinitionRest adr,
        @Context UriInfo uriInfo) {

        int i = 0;
        if (resourceId!=null) i++;
        if (groupId!=null) i++;
        if (resourceTypeId!=null) i++;

        if (i!=1) {
            throw new BadArgumentException("query param","You must give exactly one query param out of 'resourceId', 'groupId' or 'resourceTypeId'");
        }

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName(adr.getName());
        alertDefinition.setEnabled(adr.isEnabled());
        if (adr.getPriority()==null) {
            adr.setPriority("LOW");
        }
        alertDefinition.setPriority(AlertPriority.valueOf(adr.getPriority().toUpperCase()));
        alertDefinition.setConditionExpression(BooleanExpression.valueOf(adr.getConditionMode().toUpperCase()));
        alertDefinition.setRecoveryId(adr.getRecoveryId());

        if (groupId!=null) {
            ResourceGroup group = resourceGroupMgr.getResourceGroup(caller,groupId);
            alertDefinition.setResourceGroup(group);
        }
        if (resourceTypeId!=null) {
            ResourceType type = resourceTypeMgr.getResourceTypeById(caller,resourceTypeId);
            alertDefinition.setResourceType(type);
        }

        Set<AlertCondition> conditions = new HashSet<AlertCondition>(adr.getConditions().size());
        for (AlertConditionRest acr : adr.getConditions()) {
            AlertCondition condition = conditionRestToCondition(acr);
            conditions.add(condition);
        }
        alertDefinition.setConditions(conditions);

        List<AlertNotification> notifications = new ArrayList<AlertNotification>(adr.getNotifications().size());
        // check if the sender by name exists
        AlertSenderPluginManager pluginManager = alertManager.getAlertPluginManager();

        for (AlertNotificationRest anr : adr.getNotifications()) {

            AlertNotification notification = notificationRestToNotification(alertDefinition, anr);
            if (pluginManager.getAlertSenderForNotification(notification)==null) {
                throw new StuffNotFoundException("AlertSender with name [" + anr.getSenderName() +"]");
            }

            notifications.add(notification);
        }

        alertDefinition.setAlertNotifications(notifications);
        setDampeningFromRest(alertDefinition, adr);

        // Set the recovery id if such a definition exists at all
        if (adr.getRecoveryId()>0) {
            AlertDefinition recoveryDef = alertDefinitionManager.getAlertDefinition(caller,adr.getRecoveryId());
            if (recoveryDef!=null)
                alertDefinition.setRecoveryId(adr.getRecoveryId());
        }

        int definitionId = alertDefinitionManager.createAlertDefinition(caller, alertDefinition, resourceId, false);

        AlertDefinition updatedDefinition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        AlertDefinitionRest uadr = definitionToDomain(updatedDefinition,true) ; // TODO param 'full' ?

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
    @ApiOperation(value = "Update the alert definition (priority, enablement, dampening, recovery)", notes = "Priority must be HIGH,LOW,MEDIUM. If not provided, LOW is assumed.")
    public Response updateDefinition(
            @ApiParam("Id of the alert definition to update") @PathParam("id") int definitionId,
            @ApiParam("Data for the update") AlertDefinitionRest definitionRest) {

        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (definition==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        definition = new AlertDefinition(definition); // detach

        definition.setEnabled(definitionRest.isEnabled());
        if (definitionRest.getPriority()!=null) {
            definition.setPriority(AlertPriority.valueOf(definitionRest.getPriority()));
        }
        else {
            definition.setPriority(AlertPriority.LOW);
        }
        setDampeningFromRest(definition, definitionRest);

        // Set the recovery id if such a definition exists at all
        if (definitionRest.getRecoveryId()>0) {
            AlertDefinition recoveryDef = alertDefinitionManager.getAlertDefinition(caller,definitionRest.getRecoveryId());
            if (recoveryDef!=null)
                definition.setRecoveryId(definitionRest.getRecoveryId());
        }


        definition = alertDefinitionManager.updateAlertDefinitionInternal(caller, definitionId, definition, true,
            true, true);
        entityManager.flush();

        EntityTag eTag = new EntityTag(Integer.toHexString(definition.hashCode()));
        AlertDefinitionRest adr = definitionToDomain(definition, false);

        Response.ResponseBuilder builder = Response.ok(adr);
        builder.tag(eTag);

        return builder.build();

    }

    @DELETE
    @Path("definition/{id}")
    @ApiOperation("Delete an alert definition")
    public Response deleteDefinition(@ApiParam("Id of the definition to delete") @PathParam("id") int definitionId) {

        alertDefinitionManager.removeAlertDefinitions(caller, new int[]{definitionId});

        return Response.noContent().build();
    }

    /**
     * Create a dampening object for the passed definition from the alert definition rest that is passed in.
     * @param alertDefinition The alert definition to modify
     * @param adr The incoming AlertDefinitonRest object
     */
    private void setDampeningFromRest(AlertDefinition alertDefinition, AlertDefinitionRest adr) {
        AlertDampening.Category dampeningCategory;
        try {
            dampeningCategory = AlertDampening.Category.valueOf(adr.getDampeningCategory());
        }
        catch (Exception e) {
            AlertDampening.Category[] vals = AlertDampening.Category.values();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < vals.length ; i++) {
                builder.append(vals[i].name());
                if (i < vals.length-1)
                    builder.append(", ");
            }
            throw new BadArgumentException("dampening category","Allowed values are: " + builder.toString());
        }
        AlertDampening dampening = new AlertDampening(dampeningCategory);
        if (adr.getDampeningCount()!=null) {
            if (adr.getDampeningCount().contains(" ")) {
                String tmp = adr.getDampeningCount().trim();
                int num = Integer.parseInt(tmp.substring(0,tmp.indexOf(' ')));
                tmp = tmp.substring(tmp.lastIndexOf(' ')).trim();
                dampening.setValue(num);
                dampening.setValueUnits(AlertDampening.TimeUnits.valueOf(tmp.toUpperCase()));
            }
            else {
                dampening.setValue(Integer.parseInt(adr.getDampeningCount()));
            }
        }
        if (adr.getDampeningPeriod()!=null) {
            if (adr.getDampeningPeriod().contains(" ")) {
                String tmp = adr.getDampeningPeriod().trim();
                int num = Integer.parseInt(tmp.substring(0,tmp.indexOf(' ')));
                tmp = tmp.substring(tmp.lastIndexOf(' ')).trim();
                dampening.setPeriod(num);
                dampening.setPeriodUnits(AlertDampening.TimeUnits.valueOf(tmp.toUpperCase()));
            }
            else {
                dampening.setPeriod(Integer.parseInt(adr.getDampeningPeriod()));
            }
        }

        alertDefinition.setAlertDampening(dampening);
    }

    private AlertNotification notificationRestToNotification(AlertDefinition alertDefinition,
                                                             AlertNotificationRest anr) {
        AlertNotification notification = new AlertNotification(anr.getSenderName());
        // TODO validate sender
        notification.setAlertDefinition(alertDefinition);
        Configuration configuration = new Configuration();
        for (Map.Entry<String,Object> entry: anr.getConfig().entrySet()) {
            configuration.put(new PropertySimple(entry.getKey(),entry.getValue()));
        }
        notification.setConfiguration(configuration);
        // TODO extra configuration (?)
        return notification;
    }



    @POST
    @Path("definition/{id}/conditions")
    @ApiOperation("Add a new alert condition to an existing alert definition")
    public Response addConditionToDefinition(
        @ApiParam("The id of the alert definition") @PathParam("id") int definitionId,
        @ApiParam("The condition to add") AlertConditionRest conditionRest, @Context UriInfo uriInfo) {

        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (definition==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        AlertCondition condition = conditionRestToCondition(conditionRest);

        definition.addCondition(condition);

        alertDefinitionManager.updateAlertDefinition(caller,definitionId,definition,false);

        Response.ResponseBuilder builder = getResponseBuilderForCondition(definitionId, uriInfo, condition, true);

        return builder.build();
    }

    @DELETE
    @Path("condition/{cid}")
    @ApiOperation("Remove an alert condition")
    public Response deleteCondition(
        @ApiParam("The id of the condition to remove")@PathParam("cid") int conditionId) {
        Integer definitionId = findDefinitionIdForConditionId(conditionId);

        AlertDefinition definition2 = entityManager.find(AlertDefinition.class,definitionId);
        AlertCondition condition=null;
        for (AlertCondition c: definition2.getConditions()) {
            if (c.getId() == conditionId)
                condition=c;
        }

        definition2.getConditions().remove(condition);

        alertDefinitionManager.updateAlertDefinition(caller,definitionId,definition2,true);

        return Response.noContent().build();
    }

    private Integer findDefinitionIdForConditionId(int conditionId) {
    /*
            // this returns a proxy object, which is not fully initialized
            // and all the further work will fail
            AlertCondition condition = conditionMgr.getAlertConditionById(conditionId);
            AlertDefinition def = condition.getAlertDefinition()

            // So we need to "manually" pull that information in
    */

        Query q = entityManager.createQuery("SELECT condition.alertDefinition.id FROM AlertCondition condition WHERE condition.id = :id ");
        q.setParameter("id",conditionId);
        Object o = q.getSingleResult();
        return (Integer)o;
    }

    @PUT
    @Path("condition/{cid}")
    @ApiOperation("Update an existing condition of an alert definition.Note that the update will change the id of the condition")
    public Response updateCondition(
        @ApiParam("The id of the condition to update") @PathParam("cid") int conditionId,
        @ApiParam("The updated condition") AlertConditionRest conditionRest, @Context UriInfo uriInfo) {

        Integer definitionId = findDefinitionIdForConditionId(conditionId);

        AlertDefinition definition = entityManager.find(AlertDefinition.class,definitionId);
        AlertCondition condition=null;

        for (Iterator<AlertCondition> iterator = definition.getConditions().iterator(); iterator.hasNext(); ) {
            AlertCondition oldCondition = iterator.next();
            if (oldCondition.getId() == conditionId) {
                condition = new AlertCondition(oldCondition);
                oldCondition.setAlertDefinition(null);
                iterator.remove();
                entityManager.remove(oldCondition);
            }
        }


        AlertCondition restCondition = conditionRestToCondition(conditionRest);

        condition.setOption(conditionRest.getOption());
        condition.setComparator(conditionRest.getComparator());
        condition.setMeasurementDefinition(restCondition.getMeasurementDefinition());
        condition.setThreshold(conditionRest.getThreshold());
        condition.setTriggerId(conditionRest.getTriggerId());
        definition.getConditions().add(condition);

        alertDefinitionManager.updateAlertDefinitionInternal(caller, definitionId, definition, true, true, true);

        entityManager.flush();

        Response.ResponseBuilder builder = getResponseBuilderForCondition(definitionId,uriInfo,condition,false);
        return builder.build();

    }

    @GET
    @Path("condition/{cid}")
    @ApiOperation("Retrieve a condition of an alert definition by its condition id")
    public Response getCondition(
        @ApiParam("The id of the condition to retrieve") @PathParam("cid") int conditionId) {

        AlertCondition condition = conditionMgr.getAlertConditionById(conditionId);
        AlertConditionRest acr = conditionToConditionRest(condition);

        return Response.ok(acr).build();

    }

    private AlertCondition conditionRestToCondition(AlertConditionRest conditionRest) {
        AlertCondition condition = new AlertCondition();
        condition.setName(conditionRest.getName().name());
        condition.setCategory(AlertConditionCategory.valueOf(conditionRest.getCategory().getName()));
        condition.setOption(conditionRest.getOption());
        condition.setComparator(conditionRest.getComparator());
        MeasurementDefinition md = entityManager.find(MeasurementDefinition.class,
            conditionRest.getMeasurementDefinition());
        condition.setMeasurementDefinition(md);
        condition.setThreshold(conditionRest.getThreshold());
        condition.setTriggerId(conditionRest.getTriggerId());

        return condition;
    }

    private Response.ResponseBuilder getResponseBuilderForCondition(int definitionId, UriInfo uriInfo,
                                                                    AlertCondition originalCondition, boolean isCreate) {
        AlertDefinition updatedDefinition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        Set<AlertCondition> conditions = updatedDefinition.getConditions();
        int conditionId=-1;
        AlertCondition createdCondition = null;
        for (AlertCondition cond :conditions) {
            if (cond.getName().equals(originalCondition.getName())) {
                conditionId = cond.getId();
                createdCondition = cond;
            }
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/alert/condition/{cid}");
        URI uri = uriBuilder.build(conditionId);

        AlertConditionRest result = conditionToConditionRest(createdCondition);

        Response.ResponseBuilder builder;
        if (isCreate)
            builder = Response.created(uri);
        else  {
            builder = Response.ok();
            builder.location(uri);
        }
        builder.entity(result);
        return builder;
    }

    @GET
    @Path("notification/{nid}")
    @ApiOperation("Return a notification definition by its id")
    public Response getNotification(
        @ApiParam("The id of the notification definition to retrieve") @PathParam("nid") int notificationId) {

        AlertNotification notification = notificationMgr.getAlertNotification(caller,notificationId);
        AlertNotificationRest anr = notificationToNotificationRest(notification);

        return Response.ok(anr).build();
    }

    @DELETE
    @Path("notification/{nid}")
    @ApiOperation("Remove a notification definition")
    public Response deleteNotification(
        @ApiParam("The id of the notification definition to remove") @PathParam("nid") int notificationId) {

        AlertNotification notification = notificationMgr.getAlertNotification(caller,notificationId);
        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,notification.getAlertDefinition().getId());

        definition.getAlertNotifications().remove(notification);

        alertDefinitionManager.updateAlertDefinitionInternal(caller,definition.getId(),definition,true,true,true);
//        alertDefinitionManager.updateAlertDefinition(caller, definition.getId(), copiedDef, true);

        entityManager.flush();

        return Response.noContent().build();
    }

    @PUT
    @Path("notification/{nid}")
    @ApiOperation("Update a notification definition")
    public Response updateNotification(
        @ApiParam("The id of the notification definition to update") @PathParam("nid") int notificationId,
        @ApiParam("The updated notification definition to use") AlertNotificationRest notificationRest) {

        AlertNotification notification = notificationMgr.getAlertNotification(caller,notificationId);
        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,notification.getAlertDefinition().getId());

        AlertNotification newNotif = notificationRestToNotification(definition,notificationRest);
        notification.setConfiguration(newNotif.getConfiguration());
        notification.setExtraConfiguration(newNotif.getExtraConfiguration());
        // id and sender need to stay the same

        alertDefinitionManager.updateAlertDefinitionInternal(caller,definition.getId(),definition,true,true,true);
        entityManager.flush();

        List<AlertNotification> notifications = definition.getAlertNotifications();
        int newNotifId = 0;
        for (AlertNotification n : notifications) {
            if (n.getSenderName().equals(notification.getSenderName()))
                newNotifId = n.getId();
        }

        AlertNotification result = notificationMgr.getAlertNotification(caller,newNotifId);
        AlertNotificationRest resultRest = notificationToNotificationRest(result);

        return Response.ok(resultRest).build(); // TODO
    }



    @POST
    @Path("definition/{id}/notifications")
    @ApiOperation("Add a new notification definition to an alert definition")
    public Response addNotificationToDefinition(
        @ApiParam("Id of the alert definition that should get the notification definition") @PathParam("id") int definitionId,
        @ApiParam("The notification definition to add") AlertNotificationRest notificationRest, @Context UriInfo uriInfo) {

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


        alertDefinitionManager.updateAlertDefinitionInternal(caller, definitionId, definition, false, true, true);


        alertDefinitionManager.getAlertDefinition(caller,definitionId);

        entityManager.flush();

        AlertDefinition updatedDefinition = alertDefinitionManager.getAlertDefinitionById(caller,definitionId);

        List<AlertNotification> notifs = updatedDefinition.getAlertNotifications();

        assert notifs.size() == existingNotificationCount +1;

        AlertNotification updatedNotification = notifs.get(existingNotificationCount);
        AlertNotificationRest updatedNotificationRest = notificationToNotificationRest(updatedNotification);

        int notificationId = updatedNotification.getId();

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/alert/notification/{nid}");
        URI uri = uriBuilder.build(notificationId );

        Response.ResponseBuilder builder = Response.created(uri);
        builder.entity(updatedNotificationRest);

        return builder.build();

    }

    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Return a list of alert notification senders with a short description. The list does not include the configuration definition.")
    @GET @GZIP
    @Path("senders")
    public Response getAlertSenders(@Context UriInfo uriInfo) {

        List<String> senderNames = notificationMgr.listAllAlertSenders();
        List<AlertSender> senderList = new ArrayList<AlertSender>(senderNames.size());
        for (String senderName : senderNames) {
            AlertSenderInfo info = notificationMgr.getAlertInfoForSender(senderName);
            AlertSender sender = new AlertSender(senderName);
            sender.setDescription(info.getDescription());

            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/alert/sender/{name}");
            URI uri = uriBuilder.build(sender.getSenderName());
            Link self = new Link("self",uri.toString());
            sender.setLink(self);

            senderList.add(sender);
        }

        GenericEntity<List<AlertSender>> entity = new GenericEntity<List<AlertSender>>(senderList) {};

        Response.ResponseBuilder builder = Response.ok(entity);

        return builder.build();

    }

    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Return an alert notification sender by name. This includes information about the configuration it expects")
    @GET @GZIP
    @Path("sender/{name}")
    public Response getAlertSenderByName(
        @ApiParam("Name of the sender to retrieve") @PathParam("name")String senderName, @Context UriInfo uriInfo) {

        AlertSenderInfo info = notificationMgr.getAlertInfoForSender(senderName);
        if (info==null) {
            throw new StuffNotFoundException("Alert sender with name [" + senderName + "]");
        }
        AlertSender sender = new AlertSender(senderName);
        sender.setDescription(info.getDescription());

        ConfigurationDefinition definition = notificationMgr.getConfigurationDefinitionForSender(senderName);
        for (PropertyDefinition pd : definition.getPropertyDefinitions().values()) {
            if (pd instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;
                sender.getConfigDefinition().put(pds.getName(),pds.getType().name());
            }
            else {
                log.warn("Property " + pd.getName() + " for sender " + senderName + " is not of a supported type");
            }
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/alert/sender/{name}");
        URI uri = uriBuilder.build(sender.getSenderName());
        Link self = new Link("self",uri.toString());
        sender.setLink(self);


        Response.ResponseBuilder builder = Response.ok(sender);

        return builder.build();

    }


    AlertDefinitionRest definitionToDomain(AlertDefinition def, boolean full) {
        AlertDefinitionRest adr = new AlertDefinitionRest(def.getId());
        adr.setName(def.getName());
        adr.setEnabled(def.getEnabled());
        adr.setPriority(def.getPriority().getName());
        adr.setConditionMode(def.getConditionExpression().toString());
        adr.setRecoveryId(def.getRecoveryId());

        if (full) {
            Set<AlertCondition> conditions = def.getConditions();
            if (conditions.size() > 0) {
                List<AlertConditionRest> conditionRestList = new ArrayList<AlertConditionRest>(conditions.size());
                for (AlertCondition condition : conditions) {
                    AlertConditionRest acr = conditionToConditionRest(condition);
                    conditionRestList.add(acr);
                }
                adr.setConditions(conditionRestList);
            }
            List<AlertNotification> notifications = def.getAlertNotifications();
            if (notifications.size() > 0) {
                List<AlertNotificationRest> notificationRestList = new ArrayList<AlertNotificationRest>(notifications.size());
                for (AlertNotification notification : notifications) {
                    AlertNotificationRest anr = notificationToNotificationRest(notification);
                    notificationRestList.add(anr);
                }
                adr.setNotifications(notificationRestList);
            }
        }

        AlertDampening dampening = def.getAlertDampening();
        adr.setDampeningCategory(dampening.getCategory().name());
        AlertDampening.TimeUnits units = dampening.getValueUnits();
        String s = units != null ? " " + units.name() : "";
        adr.setDampeningCount(dampening.getValue()  + s);
        units = dampening.getPeriodUnits();
        s = units != null ? " " + units.name() : "";
        adr.setDampeningPeriod(dampening.getPeriod() + s);

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
        acr.setComparator(condition.getComparator());
        acr.setMeasurementDefinition(condition.getMeasurementDefinition()==null?0:condition.getMeasurementDefinition().getId());
        acr.setThreshold(condition.getThreshold());
        acr.setTriggerId(condition.getTriggerId()); // TODO what's that?

        return acr;
    }


}
