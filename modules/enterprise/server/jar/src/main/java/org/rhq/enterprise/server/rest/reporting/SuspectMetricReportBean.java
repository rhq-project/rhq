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
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Suspect metrics report Bean. Create the csv and xml versions of REST data.
 *
 * @author Mike Thompson
 */
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class SuspectMetricReportBean extends AbstractRestBean implements SuspectMetricReportLocal {

    private final Log log = LogFactory.getLog(SuspectMetricReportBean.class);

    @EJB
    SubjectManagerLocal subjectManager;

    @EJB
    ConfigurationManagerLocal configurationManager;


    @Override
    @GET
    @Path("/test")
    @Produces("text/csv")
    public Response suspectMetricReport(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        log.debug(" ** Suspect Metric REST invocation");
        //Integer resourceId = (Integer) request.getCriteria().getValues().get(CriteriaField.RESOURCE_ID);
//        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
//        criteria.addFilterResourceIds(resource.getId());
//        criteria.fetchConfiguration(true);
//        criteria.addSortCreatedTime(PageOrdering.ASC);
//        List<ResourceConfigurationUpdate> history = configurationManager.findResourceConfigurationUpdatesByCriteria( overlord, criteria);
        StringBuilder sb = new StringBuilder("ID,Group\n"); // set title row
        List<ResourceConfigurationUpdate> history = new ArrayList<ResourceConfigurationUpdate>(); // mocked up obviously
        for (ResourceConfigurationUpdate resourceConfigurationUpdate : history) {
            sb.append( resourceConfigurationUpdate.getGroupConfigurationUpdate().getId());
            sb.append(",");
            sb.append( resourceConfigurationUpdate.getGroupConfigurationUpdate().getGroup());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1); // remove last ","
        sb.append("\n");

        Response.ResponseBuilder  builder = Response.status(500); // default error response
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        log.debug(" Suspect Metric media type: "+mediaType.toString());
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            ///builder = Response.ok(entityList, mediaType);

        } else if (mediaType.toString().equals("text/csv")) {
            // CSV version
            log.debug("text/csv handler for REST");
            builder = Response.ok(sb.toString(), mediaType);

        } else {
            //unknown

        }
        return builder.build();
    }
}
