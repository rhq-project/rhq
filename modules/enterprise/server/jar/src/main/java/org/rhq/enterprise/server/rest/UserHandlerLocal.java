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

import java.util.List;

import javax.ejb.Local;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.rhq.enterprise.server.rest.domain.ResourceWithType;

/**
 * Bean that deals with user specific stuff
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml","text/plain"})
@Path("/user")
@Local
public interface UserHandlerLocal {

    @GET
    @Path("favorites/resource")
    public List<ResourceWithType> getFavorites();

    @PUT
    @Path("favorites/resource/{id}")
    public void addFavoriteResource(@PathParam("id") int id);

    @DELETE
    @Path("favorites/resource/{id}")
    public void removeResourceFromFavorites(@PathParam("id") int id);
}
