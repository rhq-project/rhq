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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

/**
 * Inventory Summary Report Bean.
 *
 * @author Mike Thompson
 */
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class InventorySummaryReportBean extends AbstractReportingRestBean implements InventorySummaryReportLocal {


    private final Log log = LogFactory.getLog(InventorySummaryReportBean.class);

    @EJB
    SubjectManagerLocal subjectManager;

    @EJB
    ConfigurationManagerLocal configurationManager;



    @Override
    @GET
    @Path("/csv")
    @Produces({MEDIA_TYPE_TEXT_CSV})
    public Response inventorySummaryReportCSV(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        log.debug(" ** InventorySummaryReport REST invocation");

        String myCsvDataTitles = "Version, Date Completed, Date Submitted, Status, User, Update Type, Resource, Ancestry\n";
        String myCsvData1 = "10045, 02/17/2012 08:11:17 AM, 02/18/2012 09:12:18 AM, Success, , Individual, AlertConditionQueue, JBossMQ < localhost:2099 < RHQ Server < 192.168.1.2\n";
        String myCsvData2 = "10046, 02/17/2012 08:15:17 AM, 02/18/2012 10:09:13 AM, Success, , Individual, DLQ, JBossMQ < localhost:2099 < RHQ Server < 192.168.1.2\n";

        Response.ResponseBuilder  builder = Response.status(500); // default error response
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            ///builder = Response.ok(entityList, mediaType);

        } else if (mediaType.equals(MEDIA_TYPE_TEXT_CSV)) {
            // CSV version
            log.debug("Inside Text/plain handler for REST");
            builder = Response.ok(myCsvDataTitles + myCsvData1 + myCsvData2, mediaType);

        } else {
            //unknown

        }
        return builder.build();
    }
}
