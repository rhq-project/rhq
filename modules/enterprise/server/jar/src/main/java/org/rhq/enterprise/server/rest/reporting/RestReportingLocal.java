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
package org.rhq.enterprise.server.rest.reporting;


import javax.ejb.Local;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

/**
 * Provide RESTful endpoints for application reports in CSV and xml.
 *
 * @author Mike Thompson
 */
@Path("/reports")
@Local
public interface RestReportingLocal {

    //Subsystems Section

    @GET
    @Path("/suspectMetrics")
    @Produces({"text/csv", "application/xml"})
    Response suspectMetricReport(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);

    @GET
    @Path("/configurationHistory")
    @Produces({"text/csv", "application/xml"})
    Response configurationHistory(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);


    @GET
    @Path("/recentOperations")
    @Produces({"text/csv", "application/xml"})
    Response recentOperations(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);


    @GET
    @Path("/recentAlerts")
    @Produces({"text/csv", "application/xml"})
    Response recentAlerts(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);


    @GET
    @Path("/alertDefinitions")
    @Produces({"text/csv", "application/xml"})
    Response alertDefinitions(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);



    @GET
    @Path("/recentDrift")
    @Produces({"text/csv", "application/xml"})
    Response recentDrift(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);


    // Inventory Section

    @GET
    @Path("/inventorySummary")
    @Produces({"text/csv", "application/xml"})
    StreamingOutput inventorySummary(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);


    @GET
    @Path("/platformUtilization")
    @Produces({"text/csv", "application/xml"})
    Response platformUtilization(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);


    @GET
    @Path("/driftCompliance")
    @Produces({"text/csv", "application/xml"})
    Response driftCompliance(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers);



}
