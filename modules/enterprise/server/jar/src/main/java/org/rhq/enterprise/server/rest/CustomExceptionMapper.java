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

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;

/**
 * Map a NotFoundException to a HTTP response with respective error message
 * @author Heiko W. Rupp
 */
@Provider
public class CustomExceptionMapper implements ExceptionMapper<Exception> {


    @Override
    public Response toResponse(Exception e) {

        Throwable cause = e.getCause();
        Response.ResponseBuilder builder;
        if (cause !=null) {
            Response.Status status;
            if (cause instanceof StuffNotFoundException)
                status =Response.Status.NOT_FOUND;
            else if (cause instanceof ResourceNotFoundException)
                status = Response.Status.NOT_FOUND;
            else if (cause instanceof ParameterMissingException)
                status = Response.Status.NOT_ACCEPTABLE;
            else if (cause instanceof PermissionException)
                status = Response.Status.FORBIDDEN;
            else
                status = Response.Status.SERVICE_UNAVAILABLE;

            builder = Response.status(status);
            builder.entity(cause.getMessage());
        }
        else {
            if (e instanceof PermissionException) {
                builder = Response.status(Response.Status.FORBIDDEN);
            } else {
                builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (e.getMessage()!=null)
                builder.entity(e.getMessage());
        }
        return builder.build();
    }
}
