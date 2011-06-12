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

import javax.ejb.Local;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.rhq.core.domain.rest.MetricAggregate;

/**
 * Deal with metrics
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml","text/plain"})
@Path("/metric")
@Local
public interface MetricHandlerLocal {


    @GET
    @Path("data/{scheduleId}")
    MetricAggregate getMetricData(@PathParam("scheduleId") int scheduleId,
                                  @QueryParam("startTime")  long startTime,
                                  @QueryParam("endTime") long endTime,
                                  @QueryParam("dataPoints") int dataPoints);
}
