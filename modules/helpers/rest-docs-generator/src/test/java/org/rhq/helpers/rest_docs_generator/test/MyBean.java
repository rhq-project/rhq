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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
@Api(value="My Foobar api", description = "This api does foo and bar and baz and is uber cool")
@Path("/foo")
public interface MyBean {

    @GET
    @Path("/one")
    @ApiOperation("This is the first method")
    public String methodOne();

    @PUT
    @Path("/two/{pp}")
    @ApiOperation("This is the second method")
    public void methodTwo(
            @ApiParam(value="The customer id") @PathParam("pp") @DefaultValue("42") int pp,
            @ApiParam(value="Hulla",required = false) @QueryParam("qp") String qp);
}
