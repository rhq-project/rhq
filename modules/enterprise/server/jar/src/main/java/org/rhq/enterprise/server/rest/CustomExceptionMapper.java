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

import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.rest.domain.RHQErrorWrapper;

/**
 * Map a NotFoundException to a HTTP response with respective error message
 * @author Heiko W. Rupp
 */
@Provider
public class CustomExceptionMapper implements ExceptionMapper<Exception> {

    @Context
    HttpHeaders httpHeaders;

    Log log = LogFactory.getLog(getClass().getName());

    @Override
    public Response toResponse(Exception e) {

        Response.ResponseBuilder builder;
        Response.Status status;

        if (e instanceof StuffNotFoundException)
            status =Response.Status.NOT_FOUND;
        else if (e instanceof ResourceNotFoundException)
            status = Response.Status.NOT_FOUND;
        else if (e instanceof ResourceGroupNotFoundException)
            status = Response.Status.NOT_FOUND;
        else if (e instanceof ResourceTypeNotFoundException)
            status = Response.Status.NOT_FOUND;
        else if (e instanceof ParameterMissingException)
            status = Response.Status.NOT_ACCEPTABLE;
        else if (e instanceof BadArgumentException)
            status = Response.Status.NOT_ACCEPTABLE;
        else if (e instanceof PermissionException)
            status = Response.Status.FORBIDDEN;
        else if (e instanceof EJBException && e.getCause()!=null && e.getCause() instanceof IllegalArgumentException)
            status = Response.Status.NOT_ACCEPTABLE;
        else
            status = Response.Status.SERVICE_UNAVAILABLE;

        builder = Response.status(status);
        String message = e.getMessage();
        wrapMessage(builder, message);
        return builder.build();
    }

    /**
     * Wrap the passed message according to the mediaType from the HttpHeader. If the
     * type can not be determined, we use plain text
     * @param builder ResponseBuilder to add the message to
     * @param message The message to wrap
     */
    private void wrapMessage(Response.ResponseBuilder builder, String message) {

        MediaType mediaType;
        try {
            mediaType = httpHeaders.getAcceptableMediaTypes().get(0);
        } catch (Exception e) {
            log.debug(e.getMessage());
            mediaType = MediaType.TEXT_PLAIN_TYPE;
        }


        if (mediaType.equals(MediaType.TEXT_PLAIN_TYPE)) {
            builder.entity(message);
        } else if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder.entity("<html><body><h1>Error</h1><h2>" + message + "</h2></body></html>");
        } else {
            RHQErrorWrapper error = new RHQErrorWrapper(message);
            builder.entity(error);
        }
        builder.type(mediaType);
    }
}
