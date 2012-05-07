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
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.links.AddLinks;
import org.jboss.resteasy.links.LinkResource;
import org.jboss.resteasy.links.LinkResources;

import org.rhq.enterprise.server.rest.domain.AlertRest;
import org.rhq.enterprise.server.rest.domain.AlertDefinitionRest;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;

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
    @AddLinks
    @LinkResource(value = AlertRest.class)
    @ApiOperation("List all alerts")
    List<AlertRest> listAlerts(
            @ApiParam(value = "Page number", defaultValue = "0") @QueryParam("page") int page,
            @ApiParam(value = "Limit to status, UNUSED AT THE MOMENT ") @QueryParam("status") String status);

    @GET
    @Path("/{id}")
    @AddLinks
    @LinkResource(value = AlertRest.class)
    @ApiOperation("Get one alert with the passed id")
    AlertRest getAlert(@ApiParam("Id of the alert to retrieve") @PathParam("id") int id);

    @PUT
    @Path("/{id}")
    @LinkResource
    @ApiOperation(value = "Mark the alert as acknowledged (by the caller)")
    AlertRest ackAlert(
            @ApiParam(value = "Id of the alert to acknowledge") @PathParam("id") int id);

    @DELETE
    @Path("/{id}")
    @LinkResource(value = AlertRest.class)
    @ApiOperation(value = "Remove the alert from the lit of alerts")
    void purgeAlert(
            @ApiParam(value = "Id of the alert to remove") @PathParam("id") int id);

    @GET
    @LinkResource(rel="definition",value = AlertDefinitionRest.class)
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
    @ApiOperation("Get one AlertDefinition by id")
    AlertDefinitionRest getAlertDefinition(@ApiParam("Id of the alert definition to retrieve") @PathParam("id") int definitionId);
}
