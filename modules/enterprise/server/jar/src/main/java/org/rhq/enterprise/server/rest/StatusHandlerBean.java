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
package org.rhq.enterprise.server.rest;

import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.rest.domain.Status;
import org.rhq.enterprise.server.rest.domain.StringValue;
import org.rhq.enterprise.server.system.SystemInfoManagerLocal;

/**
 * Return system status
 * @author Heiko W. Rupp
 */
@Api(value = "Provide system status information")
@Path("/status")
@Produces({"application/json","application/xml","text/html"})
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class StatusHandlerBean extends AbstractRestBean {


    @EJB
    private SystemInfoManagerLocal infoMgr;
    @EJB
    private ServerManagerLocal serverManager;
    @EJB
    private SubjectManagerLocal subjectManager;

    @GZIP
    @ApiOperation(value="Retrieve the current configured state of the server along with some runtime information." +
            "Caller must have MANAGE_SETTINGS to access this endpoint.",
    responseClass = "Map 'values' with map of key-value pairs describing the status")
    @GET
    @Path("/")
    public Response getStatus(@Context HttpHeaders httpHeaders) throws Exception{

        Map<String,String> statusMap = infoMgr.getSystemInformation(caller);
        Status status = new Status();
        status.setValues(statusMap);

        MediaType mediaType = httpHeaders.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            String htmlString = renderTemplate("status", status);
            builder = Response.ok(htmlString, mediaType);
        } else if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();
            builder = Response.status(Response.Status.OK).entity(WRITER.writeValueAsString(status));

        } else  {
            builder = Response.ok(status, httpHeaders.getAcceptableMediaTypes().get(0));
        }

        return builder.build();
    }

    @GET
    @Path("/server")
    @ApiOperation(value = "Get the operation mode of this server")
    public StringValue serverState() {
        Server server = serverManager.getServer();
        return new StringValue(server.getOperationMode().name());
    }
}
