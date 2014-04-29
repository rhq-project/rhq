/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.rhq.core.domain.criteria.PluginCriteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.rest.domain.BooleanValue;
import org.rhq.enterprise.server.rest.domain.PluginRest;
import org.rhq.enterprise.server.rest.domain.StringValue;

/**
 * @author Lukas Krejci
 * @since 4.11
 */
@Path("/plugins")
@Api(value="Plugin related", description = "This endpoint deals with RHQ's plugins")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class PluginHandlerBean extends AbstractRestBean {

    //using remote iface here seems to be causing some weird problems with
    //transactions.. I get HeuristicMixedException in integration tests when
    //declaring this as PluginManagerRemote.
    //That said, we only use methods also available in the remote iface so for now
    //we can be sure, the same things can be done by the REST API and remote API.
    @EJB
    private PluginManagerLocal pluginManager;

    @GET
    @Path("/")
    @ApiOperation("Looks for the plugins currently present in the system, optionally reloading them from the server's " +
        "filesystem")
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response findPlugins(@QueryParam("name") String name,
        @ApiParam("Look for enabled, disabled or both kinds of plugins?") @QueryParam("enabled") Boolean enabled,
        @ApiParam("The version of the plugin to look for") @QueryParam("version") String version,
        @ApiParam("Whether to look for deleted, installed or both plugins") @DefaultValue("false") @QueryParam("deleted") Boolean deleted,
        @ApiParam("Whether to reload the plugins from the filesystem before performing the search. This can be used to " +
            "take into effect the plugins that have been manually put into server's plugin \"dropbox\" directory.")
            @QueryParam("reload") boolean reload,
        @ApiParam("Page size for paging") @QueryParam("ps") @DefaultValue("20") int pageSize,
        @ApiParam("Page for paging, 0-based") @QueryParam("page") Integer page,
        @Context HttpHeaders headers, @Context UriInfo uriInfo) throws Exception {

        PluginCriteria crit = new PluginCriteria();
        if (name != null) {
            crit.addFilterName(name);
        }

        if (enabled != null) {
            crit.addFilterEnabled(enabled);
        }

        if (version != null) {
            crit.addFilterVersion(version);
        }

        if (deleted != null) {
            crit.addFilterDeleted(deleted);
        }

        if (reload) {
            pluginManager.update(caller);
        }

        if (page != null) {
            crit.setPaging(page, pageSize);
        }

        PageList<Plugin> plugins = pluginManager.findPluginsByCriteria(caller, crit);

        return paginate(headers, uriInfo, plugins, PluginRest.list(plugins), PluginRest.class).build();
    }

    @GET
    @Path("{id}")
    @ApiOperation("Gets info about a single plugin")
    public Response getPluginInfo(
        @ApiParam("The id of the plugin") @PathParam("id") int id,
        @Context HttpHeaders headers) {

        PluginCriteria crit = new PluginCriteria();
        crit.addFilterId(id);

        PageList<Plugin> plugins = pluginManager.findPluginsByCriteria(caller, crit);

        if (plugins.isEmpty()) {
            throw new StuffNotFoundException("Plugin");
        }

        return withMediaType(Response.ok(PluginRest.from(plugins.get(0))), headers).build();
    }

    @POST
    @Path("{id}")
    @ApiOperation("Updates the enablement of a plugin.")
    public Response updatePluginState(
        @ApiParam("The id of the plugin") @PathParam("id") int id,
        @ApiParam("true means plugin is enabled, false means plugin is disabled") @QueryParam("enabled") boolean enabled,
        @Context HttpHeaders headers) throws Exception {

        if (enabled) {
            pluginManager.enablePlugins(caller, Collections.singletonList(id));
        } else {
            pluginManager.disablePlugins(caller, Collections.singletonList(id));
        }

        return getPluginInfo(id, headers);
    }

    @DELETE
    @Path("{id}")
    @ApiOperation("Deletes a single plugin, optionally purging it.")
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response deletePlugin(
        @ApiParam("The id of the plugin") @PathParam("id") int id, @Context HttpHeaders headers) throws Exception {

        pluginManager.deletePlugins(caller, Collections.singletonList(id));

        return getPluginInfo(id, headers);
    }

    @PUT
    @Path("/")
    @ApiOperation("Puts the plugin provided using a content handle into a dropbox and scans the dropbox for changes. " +
        "In another words, this can result in more than just the provided plugin to become registered in the server " +
        "if there were some unregistered plugins waiting in the dropbox directory. The content identified by the handle" +
        "is NOT deleted afterwards.")
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response register(
    @ApiParam("The handle retrieved from upload") @QueryParam("handle") String handle,
    @ApiParam("Name of the plugin file") @QueryParam("name") String name,
    @Context HttpHeaders headers) throws Exception {

        List<Plugin> newOnes = pluginManager.deployUsingContentHandle(caller, name, handle);

        return withMediaType(Response.ok(PluginRest.list(newOnes)), headers).build();
    }

    @POST
    @Path("/deploy")
    @ApiOperation("Pushes out all the enabled plugins to all the agents running at that point in time. Defaults to " +
        "start that process immediately. The returned string is a handle that can be used to check whether all the " +
        "agents received the updated plugins.")
    public Response deployOnAgents(
        @ApiParam("The delay in milliseconds before triggering the update on the agents") @QueryParam("delay") @DefaultValue("0") long delay,
        @Context HttpHeaders headers) throws Exception {

        StringValue handle = new StringValue(pluginManager.schedulePluginUpdateOnAgents(caller, delay));

        return withMediaType(Response.ok(handle), headers).build();
    }

    @GET
    @Path("/deploy/{handle}")
    @ApiOperation("Checks whether the deployment to the agents identified by the provided handle has finished or not.")
    public Response isUpdateFinished(@ApiParam("The deploy handle") @PathParam("handle") String handle,
        @Context HttpHeaders headers) {

        BooleanValue ret = new BooleanValue(pluginManager.isPluginUpdateOnAgentsFinished(caller, handle));

        return withMediaType(Response.ok(ret), headers).build();
    }
}
