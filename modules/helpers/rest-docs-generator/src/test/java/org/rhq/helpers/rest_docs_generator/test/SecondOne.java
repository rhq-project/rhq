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

package org.rhq.helpers.rest_docs_generator.test;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiParam;

import org.rhq.helpers.rest_docs_generator.test.FooBean;

/**
 * Just an example class
 * @author Heiko W. Rupp
 */
@Path("bla")
public interface SecondOne {

    @Path("bla")
    @PUT
    @ApiError(code=404,reason = "Resource with the passed key not found")
    public Response putSomeData(
            @ApiParam("The primary key") @PathParam("id")int id,
            @ApiParam("The data to put") FooBean bean);

}
