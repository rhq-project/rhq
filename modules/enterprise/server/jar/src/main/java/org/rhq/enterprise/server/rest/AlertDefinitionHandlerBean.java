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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
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
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.StringUtil;
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
@Path("/alert")
@Api(value = "Deal with Alert Definitions",description = "This api deals with alert definitions.")
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class AlertDefinitionHandlerBean extends AbstractRestBean {

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;

    @EJB
    private AlertNotificationManagerLocal notificationMgr;

    @EJB
    private AlertConditionManagerLocal conditionMgr;

    @EJB
    private AlertManagerLocal alertManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupMgr;

    @EJB
    private ResourceTypeManagerLocal resourceTypeMgr;


    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;



    // Redirect from /definition to /definitions for GET requests
    @GET
    @Path("/definition")
    @ApiOperation(value = "Redirects to /alert/definitions")
    public Response redirectDefinitionToDefinitions(@Context UriInfo uriInfo) {
        UriBuilder uriBuilder = uriInfo.getRequestUriBuilder();
        String path = uriInfo.getPath();
        path = path.replace("/definition","/definitions");
        uriBuilder.replacePath("/rest" + path);
        Response.ResponseBuilder builder = Response.seeOther(uriBuilder.build());
        return builder.build();
    }

    @GZIP
    @GET
    @Path("/definitions")
    @ApiOperation(value = "List all Alert Definition", responseClass = "AlertDefinitionRest", multiValueResponse = true)
    public Response listAlertDefinitions(
            @ApiParam("Should conditions and notifications be returned too?") @QueryParam("full") @DefaultValue("false") boolean full,
            @ApiParam(value = "Page number") @QueryParam("page")  Integer page,
            @ApiParam(value = "Page size") @DefaultValue("20") @QueryParam("ps") int pageSize,
            @ApiParam(value = "Resource id") @QueryParam("resourceId") Integer resourceId,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {

        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addSortId(PageOrdering.ASC);
        if (page!=null) {
            criteria.setPaging(page,pageSize);
        }
        if (resourceId!=null) {
            criteria.addFilterResourceIds(resourceId);
        }

        PageList<AlertDefinition> defs = alertDefinitionManager.findAlertDefinitionsByCriteria(caller, criteria);
        List<AlertDefinitionRest> ret = new ArrayList<AlertDefinitionRest>(defs.size());
        for (AlertDefinition def : defs) {
            AlertDefinitionRest adr = definitionToDomain(def, full, uriInfo);
            ret.add(adr);
        }

        Response.ResponseBuilder builder = Response.ok();

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        builder.type(mediaType);

        if (mediaType.equals(wrappedCollectionJsonType)) {
            wrapForPaging(builder,uriInfo,defs,ret);
        } else {
            createPagingHeader(builder,uriInfo,defs);
            if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
                GenericEntity<List<AlertDefinitionRest>> list = new GenericEntity<List<AlertDefinitionRest>>(ret) {
                            };
                builder.entity(list);
            } else {
                builder.entity(ret);
            }
        }

        return builder.build();
    }

    @GET
    @Path("/definition/{id}")
    @ApiOperation(value = "Get one AlertDefinition by id", responseClass = "AlertDefinitionRest")
    @ApiError(code = 404, reason = "No definition found with the passed id.")
    public Response getAlertDefinition(@ApiParam("Id of the alert definition to retrieve") @PathParam("id") int definitionId,
                                       @ApiParam("Should conditions and notifications be returned too?") @QueryParam("full") @DefaultValue("true") boolean full,
            @Context Request request, @Context UriInfo uriInfo) {

        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller, definitionId);
        if (def==null) {
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId );
        }

        EntityTag eTag = new EntityTag(Integer.toHexString(def.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {
            AlertDefinitionRest adr = definitionToDomain(def, full, uriInfo);
            builder = Response.ok(adr);
        }
        builder.tag(eTag);

        return builder.build();
    }

    @POST
    @Path("/definitions")
    @ApiOperation(value="Create an AlertDefinition for the resource/group/resource type passed as query param. " +
        "One and only one of the three params must be given at any time. Please also check the POST method " +
        "for conditions and notifications to see their options")
    @ApiErrors({
        @ApiError(code = 406, reason = "There was not exactly one of 'resourceId','groupId' or 'resourceTypeId' given"),
        @ApiError(code = 406, reason = "The passed condition failed validation"),
        @ApiError(code = 406, reason = "The passed group was a mixed group, that can not have alert definitions"),
        @ApiError(code = 404, reason = "A non existing alert notification sender was requested."),
        @ApiError(code = 404, reason = "A referenced alert to recover does not exist")
    })
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

        Resource resource = null;
        if (resourceId!= null) {
            resource = fetchResource(resourceId);
        }

        ResourceType resourceType=null;
        if (groupId!=null) {
            ResourceGroup group = resourceGroupMgr.getResourceGroup(caller,groupId);
            alertDefinition.setGroup(group);
            if (group.getGroupCategory()== GroupCategory.MIXED) {
                throw new BadArgumentException("Group with id " + +groupId + " is a mixed group");
            }
            resourceType = group.getResourceType(); // TODO this may be null -> check 1st resource
        }

        if (resourceTypeId!=null) {
            resourceType = resourceTypeMgr.getResourceTypeById(caller,resourceTypeId);
            alertDefinition.setResourceType(resourceType);
        }

        Set<AlertCondition> conditions = new HashSet<AlertCondition>(adr.getConditions().size());
        for (AlertConditionRest acr : adr.getConditions()) {
            AlertCondition condition = conditionRestToCondition(acr, resource, resourceType);
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
            else
                throw new StuffNotFoundException("Recovery alert with id " + adr.getRecoveryId());
        }

        AlertDefinition updatedDefinition = alertDefinitionManager.createAlertDefinitionInNewTransaction(caller,
            alertDefinition, resourceId, false);
        int definitionId = updatedDefinition.getId();
        AlertDefinitionRest uadr = definitionToDomain(updatedDefinition,true, uriInfo) ; // TODO param 'full' ?

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
    @ApiError(code = 404, reason = "No AlertDefinition with the passed id exists")
    public Response updateDefinition(
            @ApiParam("Id of the alert definition to update") @PathParam("id") int definitionId,
            @ApiParam("Data for the update") AlertDefinitionRest definitionRest,
            @Context UriInfo uriInfo) {

        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (definition==null) {
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);
        }

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
            if (recoveryDef!=null) {
                definition.setRecoveryId(definitionRest.getRecoveryId());
            }
            else {
                throw new StuffNotFoundException("Alert to recover with id " + definitionRest.getRecoveryId());
            }
        }


        definition = alertDefinitionManager.updateAlertDefinitionInternal(caller, definitionId, definition, true,
            true, true);
        entityManager.flush();

        EntityTag eTag = new EntityTag(Integer.toHexString(definition.hashCode()));
        AlertDefinitionRest adr = definitionToDomain(definition, false, uriInfo);

        Response.ResponseBuilder builder = Response.ok(adr);
        builder.tag(eTag);

        return builder.build();

    }

    @DELETE
    @Path("definition/{id}")
    @ApiOperation(value = "Delete an alert definition", notes = "This operation is by default idempotent, returning 204." +
            "If you want to check if the definition existed at all, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Definition was deleted or did not exist with validation not set"),
        @ApiError(code = 404, reason = "Definition did not exist and validate was set")
    })

    public Response deleteDefinition(@ApiParam("Id of the definition to delete") @PathParam("id") int definitionId,
                                     @ApiParam("Validate if the definition exists") @QueryParam("validate") @DefaultValue("false") boolean validate) {

        int count = alertDefinitionManager.removeAlertDefinitions(caller, new int[]{definitionId});

        if (count == 0 && validate) {
            throw new StuffNotFoundException("Definition with id " + definitionId);
        }
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
            dampeningCategory = AlertDampening.Category.valueOf(adr.getDampeningCategory().toUpperCase());
        }
        catch (Exception e) {
            AlertDampening.Category[] vals = AlertDampening.Category.values();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < vals.length ; i++) {
                builder.append(vals[i].name());
                if (i < vals.length-1) {
                    builder.append(", ");
                }
            }
            throw new BadArgumentException("dampening category","Allowed values are: " + builder.toString());
        }
        if (dampeningCategory == AlertDampening.Category.ONCE) {
            // WillRecover = true means to disable after firing
            // See org.rhq.enterprise.server.alert.AlertManagerBean.willDefinitionBeDisabled()
            alertDefinition.setWillRecover(true);
            dampeningCategory = AlertDampening.Category.NONE;
        }
        if (dampeningCategory == AlertDampening.Category.NO_DUPLICATES) {
            dampeningCategory = AlertDampening.Category.NONE;
        }

        AlertDampening dampening = new AlertDampening(dampeningCategory);
        if (adr.getDampeningCount()>-1) {
            dampening.setValue(adr.getDampeningCount());
        }
        if (adr.getDampeningPeriod()>0) {
            dampening.setPeriod(adr.getDampeningPeriod());
            try {
                if (adr.getDampeningUnit()!=null) {
                    dampening.setPeriodUnits(AlertDampening.TimeUnits.valueOf(adr.getDampeningUnit().toUpperCase()));
                }
            } catch (Exception e) {
                throw new BadArgumentException("dampening unit", "Allowed values are MINUTES,HOURS,DAYS, WEEKS");
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



    @DELETE
    @Path("condition/{cid}")
    @ApiOperation(value = "Remove an alert condition", notes = "This operation is by default idempotent, returning 204." +
            "If you want to check if the condition existed at all, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Condition was deleted or did not exist with validation not set"),
        @ApiError(code = 404, reason = "Condition did not exist and validate was set")
    })
    public Response deleteCondition(
        @ApiParam("The id of the condition to remove")@PathParam("cid") int conditionId,
        @ApiParam("Validate if the condition exists") @QueryParam("validate") @DefaultValue("false") boolean validate) {

        Integer definitionId;
        try {
            definitionId = findDefinitionIdForConditionId(conditionId);
        }
        catch (NoResultException nre) {
            if (validate) {
                throw new StuffNotFoundException("Condition with id " + conditionId);
            }
            else {
                return Response.noContent().build();
            }
        }

        AlertDefinition definition2;
        definition2 = entityManager.find(AlertDefinition.class,definitionId);
        AlertCondition condition=null;
        for (AlertCondition c: definition2.getConditions()) {
            if (c.getId() == conditionId) {
                condition=c;
            }
        }

        definition2.getConditions().remove(condition);

        alertDefinitionManager.updateAlertDefinition(caller,definitionId,definition2,true);

        return Response.noContent().build();
    }

    @POST
    @Path("definition/{id}/conditions")
    @ApiOperation(value = "Add a new alert condition to an existing alert definition",
        notes = "<xml>" +
            "<para>Each condition falls into a category. Allowed categories are " +
            "AVAILABILITY, AVAIL_DURATION, BASELINE(m), CHANGE(m), CONTROL, DRIFT, EVENT, RANGE(m), RESOURCE_CONFIG, THRESHOLD(m), TRAIT(m)." +
            "Categories with an appended (m) are for metrics and need a metricDefinition, but no name, as the name is obtained from the " +
            "metric definition. Parameters vary depending on the category: " +
            "<itemizedlist>"+
            "<listitem><simpara>AVAILABILITY: name is one of AVAIL_GOES_DOWN, " +
            "AVAIL_GOES_DISABLED, AVAIL_GOES_UNKNOWN, AVAIL_GOES_NOT_UP and AVAIL_GOES_UP.</simpara></listitem>" +
            "<listitem><simpara>AVAIL_DURATION: name is one of AVAIL_DURATION_DOWN andAVAIL_DURATION_NOT_UP; option gives the duration in seconds.</simpara></listitem>"+
            "<listitem><simpara>BASELINE: option is one of 'min','mean','max', threshold gives the percentage (0.01=1%), " +
            "comparator is one of '&lt;','=' and '>'.</simpara></listitem>" +
            "<listitem><simpara>CONTROL: option gives the Operation status (FAILURE,SUCCESS,INPROGRESS,CANCELED), name is the name " +
            "of the operation (not the display-name).</simpara></listitem>" +
            "<listitem><simpara>EVENT: name is the severity (DEBUG,INFO,WARN,ERROR,FATAL), option is an optional RegEx to match against.</simpara></listitem>" +
            "<listitem><simpara>DRIFT: name is optional and matches drift-definitions; option is optional and matches directories.</simpara></listitem>" +
            "<listitem><simpara>RANGE: threshold has the lower bound, " +
            "option the higher bound, comparator is one of '&lt;','&lt;=','=','>=' or '>'.</simpara></listitem>" +
            "<listitem><simpara>RESOURCE_CONFIG: no additional params needed.</simpara></listitem>" +
            "<listitem><simpara>THRESHOLD: comparator " +
            "is one of '&lt;','=','>'; threshold is the value to compare against.</simpara></listitem>" +
            "<listitem><simpara>TRAIT: option is an optional RegEx to match against.</simpara></listitem>" +
            "</itemizedlist>" +
            "</para></xml>" )
    @ApiErrors({
        @ApiError(code = 404, reason = "No AlertDefinition with the passed id exists"),
        @ApiError(code = 406, reason = "The passed condition failed validation. A more detailed message is provided"),
    })
    public Response addConditionToDefinition(
        @ApiParam("The id of the alert definition") @PathParam("id") int definitionId,
        @ApiParam("The condition to add") AlertConditionRest conditionRest, @Context UriInfo uriInfo) {

        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (definition==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        Resource resource = definition.getResource();
        ResourceType resourceType = definition.getResourceType();
        AlertCondition condition = conditionRestToCondition(conditionRest, resource, resourceType);

        definition.addCondition(condition);

        alertDefinitionManager.updateAlertDefinition(caller,definitionId,definition,false);

        Response.ResponseBuilder builder = getResponseBuilderForCondition(definitionId, uriInfo, condition, true);

        return builder.build();
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
    @ApiErrors({
        @ApiError(code = 404, reason = "Condition with passed id does not exist"),
        @ApiError(code = 406, reason = "The passed category or condition operator was invalid")
    })
    public Response updateCondition(
        @ApiParam("The id of the condition to update") @PathParam("cid") int conditionId,
        @ApiParam("The updated condition") AlertConditionRest conditionRest, @Context UriInfo uriInfo) {

        Integer definitionId;
        try {
            definitionId = findDefinitionIdForConditionId(conditionId);
        }
        catch (NoResultException nre) {
            throw new StuffNotFoundException("Condition with id " + conditionId);
        }

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

        Resource resource = definition.getResource();
        ResourceType resourceType = definition.getResourceType();
        AlertCondition restCondition = conditionRestToCondition(conditionRest, resource, resourceType);

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
    @ApiError(code = 404, reason = "No condition with the passed id exists")
    public Response getCondition(
        @ApiParam("The id of the condition to retrieve") @PathParam("cid") int conditionId) {

        AlertCondition condition = conditionMgr.getAlertConditionById(conditionId);
        if (condition==null) {
            throw new StuffNotFoundException("No condition with id " + conditionId);
        }
        AlertConditionRest acr = conditionToConditionRest(condition);

        return Response.ok(acr).build();

    }

    /**
     * Convert a passed condition from the REST side into the internal domain
     * representation. The largest part of this method is validation of the input
     *
     *
     * @param conditionRest Object to convert
     * @param resource Optional {@link org.rhq.core.domain.resource.Resource} to check against if not null
     * @param resourceType Optional {@link ResourceType} to validate against if not null
     * @return Converted domain object
     * @throws BadArgumentException If validation fails
     */
    private AlertCondition conditionRestToCondition(AlertConditionRest conditionRest, Resource resource,
                                                    ResourceType resourceType) {
        AlertCondition condition = new AlertCondition();

        try {
            condition.setCategory(AlertConditionCategory.valueOf(conditionRest.getCategory().toUpperCase()));
        } catch (Exception e) {
            String allowedValues = stringify(AlertConditionCategory.class);
            throw new BadArgumentException("Field 'category' [" + conditionRest.getCategory() + "] is invalid. Allowed values "+
                "are : " + allowedValues);
        }

        int measurementDefinition = conditionRest.getMeasurementDefinition();
        MeasurementDefinition md;
        if (measurementDefinition!=0) {
            md = entityManager.find(MeasurementDefinition.class, measurementDefinition);
            if (md==null) {
                throw new StuffNotFoundException("measurementDefinition with id " + measurementDefinition);
            }

            // Validate that the definition belongs to the resource, if passed
            if (resource!=null) {
                ResourceType type = resource.getResourceType();
                Set<MeasurementDefinition> definitions = type.getMetricDefinitions();
                if (!definitions.contains(md)) {
                    throw new BadArgumentException("MeasurementDefinition does not apply to resource");
                }
            }

            // Validate that the definition belongs to the passed resource type
            if (resourceType!=null) {
                Set<MeasurementDefinition> definitions = resourceType.getMetricDefinitions();
                if (!definitions.contains(md)) {
                    throw new BadArgumentException("MeasurementDefinition does not apply to resource type");
                }
            }
        }


        String optionValue = conditionRest.getOption();

        String conditionName = conditionRest.getName();
        // Set the name for all cases and allow it to be overridden later.
        condition.setName(conditionName);

        AlertConditionCategory category = condition.getCategory();
        switch (category) {
        case ALERT:
            // Looks internal -- noting to do.
            break;
        case AVAIL_DURATION:
            if (optionValue ==null) {
                throw new BadArgumentException("Option needs to be provided as duration in seconds");
            }
            try {
                Integer.parseInt(optionValue);
            } catch (NumberFormatException nfe) {
                throw new BadArgumentException("Option provided [" + optionValue + "] was bad. Must be duration in seconds");
            }
            checkForAllowedValues("name", conditionName, "AVAIL_DURATION_DOWN", "AVAIL_DURATION_NOT_UP");
            break;
        case AVAILABILITY:
            checkForAllowedValues("name", conditionName, "AVAIL_GOES_DOWN", "AVAIL_GOES_DISABLED",
                "AVAIL_GOES_UNKNOWN", "AVAIL_GOES_NOT_UP", "AVAIL_GOES_UP");
            break;
        case BASELINE:
            if (measurementDefinition ==0) {
                throw new BadArgumentException("You need to provide a measurementDefinition for category BASELINE");
            }

            md = entityManager.find(MeasurementDefinition.class,
                measurementDefinition);
            if (md==null) {
                throw new StuffNotFoundException("measurementDefinition with id " + measurementDefinition);
            }
            condition.setMeasurementDefinition(md);
            condition.setName(md.getDisplayName());
            checkForAllowedValues("option", optionValue, "min", "max", "mean");
            checkForAllowedValues("comparator", conditionRest.getComparator(), "<", "=", ">");
            break;
        case CHANGE:
            md = getMeasurementDefinition(measurementDefinition, category);
            condition.setMeasurementDefinition(md);
            condition.setName(md.getDisplayName());
            if (md.getDataType()== DataType.CALLTIME) {
                checkForAllowedValues("option", optionValue, "MIN", "MAX", "AVG");
            }
            break;
        case CONTROL:
            checkForAllowedValues("option",optionValue,"INPROGRESS", "SUCCESS", "FAILURE", "CANCELED");

            if (conditionName ==null) {
                throw new BadArgumentException("name must be the name (not display name) of a valid operation.");
            }
            // TODO check for valid operation -- only on the resource or type itself (still hard enough)
            break;
        case DRIFT:
            // option and name are optional, so nothing to do
            break;
        case EVENT:
            checkForAllowedValues("name", conditionName,"DEBUG", "INFO", "WARN", "ERROR", "FATAL");
            // option is an optional regular expression
            break;
        case RANGE:
            checkForAllowedValues("comparator", conditionRest.getComparator(), "<", "=", ">","<=",">=");
            if (optionValue==null) {
                throw new BadArgumentException("You need to supply an upper threshold in 'option' as numeric value");
            }
            try {
                Double.parseDouble(optionValue);
            }
            catch (NumberFormatException nfe) {
                throw new BadArgumentException("You need to supply an upper threshold in 'option' as numeric value");
            }
            md = getMeasurementDefinition(measurementDefinition, category);
            condition.setMeasurementDefinition(md);
            condition.setName(md.getDisplayName());

            break;
        case RESOURCE_CONFIG:
            // Nothing to do
            break;
        case THRESHOLD:
            checkForAllowedValues("comparator", conditionRest.getComparator(), "<", "=", ">");
            md = getMeasurementDefinition(measurementDefinition, category);
            condition.setMeasurementDefinition(md);
            condition.setName(md.getDisplayName());

            if (md.getDataType()== DataType.CALLTIME) {
                checkForAllowedValues("option", optionValue, "MIN", "MAX", "AVG");
            }

            break;
        case TRAIT:
            md = getMeasurementDefinition(measurementDefinition, category);
            condition.setMeasurementDefinition(md);
            condition.setName(md.getDisplayName());

            // No need to check options - they are optional
            break;
        }

        condition.setOption(optionValue);
        condition.setComparator(conditionRest.getComparator());
        condition.setThreshold(conditionRest.getThreshold());
        condition.setTriggerId(conditionRest.getTriggerId());

        return condition;
    }

    private MeasurementDefinition getMeasurementDefinition(int measurementDefinition, AlertConditionCategory category) {
        MeasurementDefinition md;
        if (measurementDefinition ==0) {
            throw new BadArgumentException("You need to provide a measurementDefinition for category " + category.name());
        }
        md = entityManager.find(MeasurementDefinition.class,
            measurementDefinition);
        if (md==null) {
            throw new StuffNotFoundException("measurementDefinition with id " + measurementDefinition);
        }

        return md;
    }

    /**
     * Test if #toCheck matches one of the allowedValues and throw a BadArgumentException
     * if not. In this case the attribute name is passed in the exception
     *
     *
     * @param attributeName Name of the Attribute in error
     * @param toCheck Value to check for
     * @param allowedValues Allowed values
     * @throws BadArgumentException if the values to check does not match any of the allowed values
     */
    private void checkForAllowedValues(String attributeName, String toCheck, String... allowedValues) {
        if (toCheck==null) {
            throw new BadArgumentException("Field " + attributeName + " must be set. Allowed values are: " + StringUtil.arrayToString(allowedValues));
        }

        if (allowedValues==null) {
            throw new IllegalArgumentException("No allowed values are provided - please contact support");
        }

        boolean match = false;

        for (String value : allowedValues) {
            if (toCheck.equals(value))
                match=true;
        }
        if (!match) {
            throw new BadArgumentException("Field " + attributeName + " has an invalid value [" + toCheck + "]. Allowed values are: "
                + StringUtil.arrayToString(allowedValues));
        }
    }

    /**
     * List the names of the passed Enum as a comma separated String
     * @param clazz
     * @return
     */
    private String stringify(Class<? extends Enum> clazz) {
        EnumSet enumSet = EnumSet.allOf(clazz);
        StringBuilder b = new StringBuilder();
        Iterator iter = enumSet.iterator();
        while (iter.hasNext()) {
            Enum anEnum= (Enum) iter.next();
            b.append(anEnum.name());
            if (iter.hasNext()) {
                b.append(", ");
            }
        }
        return b.toString();
    }

    private Response.ResponseBuilder getResponseBuilderForCondition(int definitionId, UriInfo uriInfo,
                                                                    AlertCondition originalCondition, boolean isCreate) {
        AlertDefinition updatedDefinition = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        Set<AlertCondition> conditions = updatedDefinition.getConditions();
        int conditionId=-1;
        AlertCondition createdCondition = null;
        for (AlertCondition cond :conditions) {
            if (originalCondition.getId() == cond.getId() || (
                cond.getName() != null && cond.getName().equals(originalCondition.getName())))
            {
                conditionId = cond.getId();
                createdCondition = cond;
            }
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/alert/condition/{cid}");
        URI uri = uriBuilder.build(conditionId);

        AlertConditionRest result = conditionToConditionRest(createdCondition);

        Response.ResponseBuilder builder;
        if (isCreate) {
            builder = Response.created(uri);
        }
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
    @ApiError(code = 404, reason = "No notification with the passed id found")
    public Response getNotification(
        @ApiParam("The id of the notification definition to retrieve") @PathParam("nid") int notificationId) {

        AlertNotification notification = notificationMgr.getAlertNotification(caller,notificationId);
        if (notification==null) {
            throw new StuffNotFoundException("No notification with id " + notificationId);
        }
        AlertNotificationRest anr = notificationToNotificationRest(notification);

        return Response.ok(anr).build();
    }

    @DELETE
    @Path("notification/{nid}")
    @ApiOperation(value = "Remove a notification definition", notes = "This operation is by default idempotent, returning 204." +
            "If you want to check if the notification existed at all, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Notification was deleted or did not exist with validation not set"),
        @ApiError(code = 404, reason = "Notification did not exist and validate was set")
    })
    public Response deleteNotification(
        @ApiParam("The id of the notification definition to remove") @PathParam("nid") int notificationId,
        @ApiParam("Validate if the notification exists") @QueryParam("validate") @DefaultValue("false") boolean validate) {

        AlertNotification notification = notificationMgr.getAlertNotification(caller,notificationId);
        if (notification!=null) {
            AlertDefinition definition = alertDefinitionManager.getAlertDefinition(caller,notification.getAlertDefinition().getId());

            definition.getAlertNotifications().remove(notification);

            alertDefinitionManager.updateAlertDefinitionInternal(caller,definition.getId(),definition,true,true,true);

            entityManager.flush();
        } else {
            if (validate) {
                throw new StuffNotFoundException("Notification with id "+ notificationId);
            }
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("notification/{nid}")
    @ApiOperation("Update a notification definition")
    @ApiError(code = 404, reason = "There is no notification with the passed id")
    public Response updateNotification(
        @ApiParam("The id of the notification definition to update") @PathParam("nid") int notificationId,
        @ApiParam("The updated notification definition to use") AlertNotificationRest notificationRest) {

        AlertNotification notification = notificationMgr.getAlertNotification(caller,notificationId);
        if (notification==null) {
            throw new StuffNotFoundException("No notification with id " + notificationId);
        }

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
            if (n.getSenderName().equals(notification.getSenderName())) {
                newNotifId = n.getId();
            }
        }

        AlertNotification result = notificationMgr.getAlertNotification(caller,newNotifId);
        AlertNotificationRest resultRest = notificationToNotificationRest(result);

        return Response.ok(resultRest).build(); // TODO
    }



    @POST
    @Path("definition/{id}/notifications")
    @ApiOperation("Add a new notification definition to an alert definition")
    @ApiErrors({
        @ApiError(code = 404, reason = "Requested alert notification sender does not exist"),
        @ApiError(code = 404, reason = "There is no alert definition with the passed id")
    })
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
        if (definition==null) {
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);
        }

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
    @ApiError(code = 404, reason = "There is no sender with the passed name")
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


    AlertDefinitionRest definitionToDomain(AlertDefinition def, boolean full, UriInfo uriInfo) {
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
        if (dampening.getCategory()== AlertDampening.Category.NONE && def.getWillRecover()) {
            adr.setDampeningCategory(AlertDampening.Category.ONCE.name());
        }

        AlertDampening.TimeUnits units = dampening.getValueUnits();
        String s = units != null ? " " + units.name() : "";
        adr.setDampeningCount(dampening.getValue());
        units = dampening.getPeriodUnits();
        s = units != null ? " " + units.name() : "";
        adr.setDampeningPeriod(dampening.getPeriod());
        if (dampening.getPeriodUnits()!=null) {
            adr.setDampeningUnit(dampening.getPeriodUnits().name());
        }

        List<Link> links = adr.getLinks();
        if (def.getResource()!=null) {
            links.add(createUILink(uriInfo, UILinkTemplate.RESOURCE_ALERT_DEF, def.getResource().getId(), adr.getId()));
            links.add(getLinkToResource(def.getResource(), uriInfo, "resource"));
        } else if (def.getGroup() != null) {
            links.add(
                createUILink(uriInfo, UILinkTemplate.GROUP_ALERT_DEF, def.getGroup().getId(), adr.getId()));
            links.add(getLinkToGroup(def.getGroup(), uriInfo, "group"));
        } else {
            links.add(
                createUILink(uriInfo, UILinkTemplate.TEMPLATE_ALERT_DEF, def.getResourceType().getId(), adr.getId()));
            links.add(getLinkToResourceType(def.getResourceType(),uriInfo,"resourceType"));
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
        acr.setName(condition.getName());
        acr.setCategory(condition.getCategory().getName());
        acr.setOption(condition.getOption());
        acr.setComparator(condition.getComparator());
        acr.setMeasurementDefinition(condition.getMeasurementDefinition()==null?0:condition.getMeasurementDefinition().getId());
        acr.setThreshold(condition.getThreshold());
        acr.setTriggerId(condition.getTriggerId()); // TODO what's that?

        return acr;
    }


}
