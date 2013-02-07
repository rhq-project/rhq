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
package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.auth.Subject;

@Path("/inventorySummary")
@Local
@Api(basePath="http://localhost:7080/coregui/reports", value = "The inventory summary report")

public interface InventorySummaryLocal {

    /**
     * Generates an inventory summary report. The report can be in either a master or
     * details format.
     *
     * @param request
     * @param resourceTypeId Corresponds to the resourceTypeId query string parameter and
     * must be specified to generate the details version of the report.
     * @param version Corresponds to the version query string parameter and should be
     * specified to generate the details version of the report.
     * @return An output stream that contains the CSV report.
     */
    @GZIP
    @GET
    @Produces({"text/csv"})
    @ApiOperation(value = "Export the Inventory Summary data as CSV")
    StreamingOutput generateReport(
        @Context HttpServletRequest request,
        @QueryParam("resourceTypeId") String resourceTypeId,
        @QueryParam("version") String version);

    StreamingOutput generateReportInternal(
        HttpServletRequest request,
        String resourceTypeId,
        String version,
        Subject user);

}
