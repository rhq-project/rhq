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

import java.util.List;

import javax.ejb.Local;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.enterprise.server.rest.domain.AlertRest;
import org.rhq.enterprise.server.rest.domain.AlertDefinitionRest;

/**
 * Deal with Alerts
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml","text/plain"})
@Local
@Path("/alert")
@Api(value = "Deal with Alerts",description = "This api deals with alerts that have fired. It does not offer to create/update AlertDefinitions (yet)")
public interface AlertHandlerLocal {

    @GET
    @Path("/")
    @ApiOperation(value = "List all alerts", multiValueResponse = true, responseClass = "List<AlertRest")
    Response listAlerts(
            @ApiParam(value = "Page number", defaultValue = "0") @QueryParam("page") int page,
            @ApiParam(value = "Limit to status, UNUSED AT THE MOMENT ") @QueryParam("status") String status,
            @ApiParam(value = "Should full resources and definitions be sent") @QueryParam("slim") @DefaultValue(
                    "false") boolean slim,
            @ApiParam(value = "If non-null only send alerts that have fired after this time, time is millisecond since epoch")
                @QueryParam("since") Long since,
            @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers);

    @GET
    @Path("count")
    @ApiOperation("Return a count of alerts in the system depending on criteria")
    int countAlerts(@ApiParam(value = "If non-null only send alerts that have fired after this time, time is millisecond since epoch")
                    @QueryParam("since") Long since);

    @GET
    @Cache(maxAge = 60)
    @Path("/{id}")
    @ApiOperation(value = "Get one alert with the passed id", responseClass = "AlertRest")
    Response getAlert(
        @ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
        @ApiParam(value = "Should full resources and definitions be sent") @QueryParam("slim") @DefaultValue("false") boolean slim,
        @Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);

    @GET
    @Path("/{id}/conditions")
    @Cache(maxAge = 300)
    @ApiOperation(value = "Return the notification logs for the given alert")
    Response getConditionLogs(@ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
                              @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers);

    @GET
    @Path("/{id}/notifications")
    @Cache(maxAge = 60)
    @ApiOperation(value = "Return the notification logs for the given alert")
    Response getNotificationLogs(@ApiParam("Id of the alert to retrieve") @PathParam("id") int id,
                                 @Context Request request, @Context UriInfo uriInfo, @Context HttpHeaders headers);

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "Mark the alert as acknowledged (by the caller)", notes = "Returns a slim version of the alert")
    AlertRest ackAlert(
            @ApiParam(value = "Id of the alert to acknowledge") @PathParam("id") int id, @Context UriInfo uriInfo);

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Remove the alert from the lit of alerts")
    void purgeAlert(
            @ApiParam(value = "Id of the alert to remove") @PathParam("id") int id);

    @GET
    @Cache(maxAge = 300)
    @Path("/{id}/definition")
    @ApiOperation("Get the alert definition (basics) for the alert")
    AlertDefinitionRest getDefinitionForAlert(@ApiParam("Id of the alert to show the definition") @PathParam("id") int alertId);


    @GET
    @Path("/definition")
    @ApiOperation("List all Alert Definition")
    List<AlertDefinitionRest> listAlertDefinitions(
            @ApiParam(value = "Page number", defaultValue = "0") @QueryParam("page") int page,
            @ApiParam(value = "Limit to status, UNUSED AT THE MOMENT ") @QueryParam("status") String status);

    @GET
    @Path("/definition/{id}")
    @ApiOperation(value = "Get one AlertDefinition by id", responseClass = "AlertDefinitionRest")
    Response getAlertDefinition(@ApiParam("Id of the alert definition to retrieve") @PathParam("id") int definitionId,
        @Context Request request);

    @PUT
    @Path("/definition/{id}")
    @ApiOperation(value = "Update the alert definition (priority, enablement)", notes = "Priority must be HIGH,LOW,MEDIUM")
    Response updateDefinition(@ApiParam("Id of the alert definition to update") @PathParam("id") int definitionId,
                                         AlertDefinitionRest definition, @Context Request request);
}
