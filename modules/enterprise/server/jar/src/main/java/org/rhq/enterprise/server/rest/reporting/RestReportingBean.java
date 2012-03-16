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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

/**
 *  Provider of RESTful reports via CSV, Xml.
 *
 * @author Mike Thompson
 */
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RestReportingBean extends AbstractRestBean implements RestReportingLocal {

    private final Log log = LogFactory.getLog(RestReportingBean.class);

    /**
     * Subject Needed for he SetCallerInterceptor.
     */
    @EJB
    SubjectManagerLocal subjectManager;

    @EJB
    ConfigurationManagerLocal configurationManager;


    @Override
    @GET
    @Path("/configurationHistory/{resourceId}")
    @Produces("text/csv")
    public Response configurationHistory (@PathParam("resourceId") int resourceId,@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        StringBuilder sb;
        log.debug(" ** Configuration History REST invocation");
        //Integer resourceId = (Integer) request.getCriteria().getValues().get(CriteriaField.RESOURCE_ID);
        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.fetchConfiguration(true);
        criteria.addSortCreatedTime(PageOrdering.ASC);
        //List<ResourceConfigurationUpdate> history = configurationManager.findResourceConfigurationUpdatesByCriteria( subjectManager.getOverlord(), criteria);

        CriteriaQueryExecutor<ResourceConfigurationUpdate, ResourceConfigurationUpdateCriteria> queryExecutor =
            new CriteriaQueryExecutor<ResourceConfigurationUpdate, ResourceConfigurationUpdateCriteria>() {
                @Override
                public PageList<ResourceConfigurationUpdate> execute(ResourceConfigurationUpdateCriteria criteria) {
                    return configurationManager.findResourceConfigurationUpdatesByCriteria(subjectManager.getOverlord(),
                        criteria);
                }
            };

        CriteriaQuery<ResourceConfigurationUpdate, ResourceConfigurationUpdateCriteria> query =
            new CriteriaQuery<ResourceConfigurationUpdate, ResourceConfigurationUpdateCriteria>(criteria, queryExecutor);
        sb = new StringBuilder("ID,Status\n"); // set title row
        for (ResourceConfigurationUpdate configUpdate : query) {
            sb.append(configUpdate.getId());
            sb.append(",");
            sb.append(configUpdate.getStatus());
            sb.append(",");
        }

//        for (ResourceConfigurationUpdate resourceConfigurationUpdate : history) {
//            sb.append( resourceConfigurationUpdate.getGroupConfigurationUpdate().getId());
//            sb.append(",");
//            sb.append( resourceConfigurationUpdate.getGroupConfigurationUpdate().getGroup());
//            sb.append(",");
//        }
        sb.deleteCharAt(sb.length() - 1); // remove last ","
        sb.append("\n");
        //@todo: what if there is no results

        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        log.debug(" Suspect Metric media type: "+mediaType.toString());
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            ///builder = Response.ok(entityList, mediaType);

        } else if (mediaType.toString().equals("text/csv")) {
            // CSV version
            log.debug("text/csv handler for REST");
            builder = Response.ok(sb.toString(), mediaType);

        } else {
            log.debug("Unknown Media Type: "+ mediaType.toString());
            builder = Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE);

        }
        return builder.build();
    }

    @Override
    @GET
    @Path("/configurationHistory")
    @Produces("text/csv")
    public Response suspectMetricReport(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {

        log.debug(" ** Configuration History REST invocation");

        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        log.debug(" Suspect Metric media type: "+mediaType.toString());
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            ///builder = Response.ok(entityList, mediaType);

        } else if (mediaType.toString().equals("text/csv")) {
            // CSV version
            log.debug("text/csv handler for REST");
            builder = Response.ok("CSV,file", mediaType);

        } else {
            log.debug("Unknown Media Type: "+ mediaType.toString());
            builder = Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE);

        }
        return builder.build();
    }

    @Override
    @GET
    @Path("/recentOperations")
    @Produces({"text/csv", "application/xml"})
    public Response recentOperations(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        return builder.build();
    }

    @Override
    @GET
    @Path("/recentAlerts")
    @Produces({"text/csv", "application/xml"})
    public Response recentAlerts(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        return builder.build();
    }

    @Override
    @GET
    @Path("/alertDefinitions")
    @Produces({"text/csv", "application/xml"})
    public Response alertDefinitions(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        return builder.build();
    }

    @Override
    @GET
    @Path("/recentDrift")
    @Produces({"text/csv", "application/xml"})
    public Response recentDrift(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        return builder.build();
    }

    @Override
    @GET
    @Path("/inventorySummary")
    @Produces({"text/csv", "application/xml"})
    public Response inventorySummary(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        return builder.build();
    }

    @Override
    @GET
    @Path("/platformUtilization")
    @Produces({"text/csv", "application/xml"})
    public Response platformUtilization(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        return builder.build();
    }

    @Override
    @GET
    @Path("/driftCompliance")
    @Produces({"text/csv", "application/xml"})
    public Response driftCompliance(@Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        return builder.build();
    }


    /**
     * What special characters should we remove to make this valid CSV, XML.
     * @todo: ignore what characters for parsing CSV i.e., strip special chars
     * @param inString to peform replacement on
     * @return String new valid string
     */
    private String stripSpecialChars(String inString){
        return  inString.replace("\n"," ").replace(',', ' ');
    }
}
