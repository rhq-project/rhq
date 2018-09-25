/*
 * RHQ Management Platform
 *  Copyright (C) 2005-2013 Red Hat, Inc.
 *  All rights reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation version 2 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.infinispan.manager.CacheContainer;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import org.jboss.security.SimplePrincipal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.rest.domain.GroupRest;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;
import org.rhq.enterprise.server.rest.domain.UserRest;

/**
 * Class that deals with user specific stuff
 * @author Heiko W. Rupp
 */
@Produces({ "application/json", "application/xml", "text/plain", "text/html" })
@Path("/user")
@Api(value = "Api that deals with user related stuff")
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class UserHandlerBean extends AbstractRestBean {

    /**
     * List of favorite {@link org.rhq.core.domain.resource.Resource} id's, delimited by '|' characters. Default is "".
     */
    public static final String RESOURCE_HEALTH_RESOURCES = ".dashContent.resourcehealth.resources";

    /**
     * List of favorite {@link org.rhq.core.domain.resource.group.ResourceGroup} id's, delimited by '|' characters.
     * Default is "".
     */
    public static final String GROUP_HEALTH_GROUPS = ".dashContent.grouphealth.groups";

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @javax.annotation.Resource( name = "ISPNsecurity", mappedName = "java:jboss/infinispan/security")
    private CacheContainer cacheContainer;
    protected org.infinispan.Cache<CacheKey, Object> authCache;

    @PostConstruct
    public void start() {
        super.start();
        this.authCache = this.cacheContainer.getCache("RHQRESTSecurityDomain");
    }

    @GZIP
    @GET
    @Path("favorites/resource")
    @ApiOperation(value = "Return a list of favorite resources of the caller", multiValueResponse = true, responseClass = "ResourceWithType")
    public Response getFavorites(@Context
    UriInfo uriInfo, @Context
    HttpHeaders httpHeaders) {

        Set<Integer> favIds = getResourceIdsForFavorites();
        List<ResourceWithType> ret = new ArrayList<ResourceWithType>();

        MediaType mediaType = httpHeaders.getAcceptableMediaTypes().get(0);
        for (Integer id : favIds) {
            try {
                Resource res = resourceManager.getResource(caller, id);

                ResourceWithType rwt = fillRWT(res, uriInfo);
                ret.add(rwt);
            } catch (Exception e) {
                if (e instanceof ResourceNotFoundException) {
                    log.debug("Favorite resource with id " + id + " not found - not returning to the user");
                }
                else {
                    log.warn("Retrieving resource with id " + id + " failed: " + e.getLocalizedMessage());
                }
            }
        }
        Response.ResponseBuilder builder;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listResourceWithType", ret), mediaType);
        } else {
            GenericEntity<List<ResourceWithType>> list = new GenericEntity<List<ResourceWithType>>(ret) {
            };
            builder = Response.ok(list);
        }

        return builder.build();

    }

    @GZIP
    @GET
    @Path("favorites/group")
    @ApiOperation(value = "Return a list of favorite groups of the caller", multiValueResponse = true, responseClass = "GroupRest")
    public Response getGroupFavorites(@Context
    UriInfo uriInfo, @Context
    HttpHeaders httpHeaders) {

        Set<Integer> favIds = getGroupIdsForFavorites();
        List<GroupRest> ret = new ArrayList<GroupRest>();

        MediaType mediaType = httpHeaders.getAcceptableMediaTypes().get(0);
        for (Integer id : favIds) {
            try {
                /*
                * In theory we should not have any bad group ids in favorites, but ...
                */
                ResourceGroup res = resourceGroupManager.getResourceGroup(caller, id);
                GroupRest rwt = fillGroup(res, uriInfo);
                ret.add(rwt);
            }
            catch (ResourceGroupNotFoundException e) {
                log.debug("Favorite group with id " + id + " not found - not returning to the user");
            }
            catch (Exception e) {
                log.warn("Retrieving group with id " + id + " failed: " + e.getLocalizedMessage());
            }
        }
        Response.ResponseBuilder builder;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listGroup", ret), mediaType);
        } else {
            GenericEntity<List<GroupRest>> list = new GenericEntity<List<GroupRest>>(ret) {
            };
            builder = Response.ok(list);
        }

        return builder.build();

    }

    @PUT
    @Path("favorites/resource/{id}")
    @ApiOperation(value = "Add a resource as favorite for the caller")
    public void addFavoriteResource(@ApiParam(name = "id", value = "Id of the resource")
    @PathParam("id")
    int resourceId) {

        // Check if the resource exists and throw an error if not
        fetchResource(resourceId);

        Set<Integer> favIds = getResourceIdsForFavorites();
        if (!favIds.contains(resourceId)) {
            favIds.add(resourceId);
            updateResourceFavorites(favIds);
        }
    }

    @PUT
    @Path("favorites/group/{id}")
    @ApiOperation(value = "Add a group as favorite for the caller")
    public void addFavoriteResourceGroup(@ApiParam(name = "id", value = "Id of the group")
    @PathParam("id")
    int groupId) {

        // Check if the resource exists and throw an error if not
        fetchGroup(groupId, false);

        Set<Integer> favIds = getGroupIdsForFavorites();
        if (!favIds.contains(groupId)) {
            favIds.add(groupId);
            updateGroupFavorites(favIds);
        }
    }

    @DELETE
    @Path("favorites/resource/{id}")
    @ApiOperation(value = "Remove a resource from favorites", notes = "This operation is by default idempotent, returning 204." +
                "If you want to check if the resource was a favorite, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Resource was removed or was no favorite with validation not set"),
        @ApiError(code = 404, reason = "Resource was no favorite and validate was set")
    })
    public void removeResourceFromFavorites(
        @ApiParam(name = "id", value = "Id of the resource") @PathParam("id") int id,
        @ApiParam("Validate if the resource is a favorite") @QueryParam("validate") @DefaultValue("false") boolean validate) {

        Set<Integer> favIds = getResourceIdsForFavorites();
        if (favIds.contains(id)) {
            favIds.remove(id);
            updateResourceFavorites(favIds);
        }
        else {
            if (validate) {
                throw new StuffNotFoundException("Resource with id " + id + " in favorites ");
            }
        }
    }

    @DELETE
    @Path("favorites/group/{id}")
    @ApiOperation(value = "Remove a group from favorites", notes = "This operation is by default idempotent, returning 204." +
                    "If you want to check if the group was a favorite, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Group was removed or was no favorite with validation not set"),
        @ApiError(code = 404, reason = "Group was no favorite and validate was set")
    })
    public void removeResourceGroupFromFavorites(
        @ApiParam(name = "id", value = "Id of the group") @PathParam("id") int id,
        @ApiParam("Validate if the group is a favorite") @QueryParam("validate") @DefaultValue("false") boolean validate) {

        Set<Integer> favIds = getGroupIdsForFavorites();
        if (favIds.contains(id)) {
            favIds.remove(id);
            updateGroupFavorites(favIds);
        }
        else {
            if (validate) {
                throw new StuffNotFoundException("Group with id " + id + " in favorites ");
            }
        }
    }

    @GET
    @Cache(maxAge = 600)
    @Path("{id}")
    @ApiOperation(value = "Get info about a user", responseClass = "UserRest")
    public Response getUserDetails(@ApiParam(value = "Login of the user")
    @PathParam("id")
    String loginName, @Context
    Request request, @Context
    HttpHeaders headers) {

        Subject subject = subjectManager.getSubjectByName(loginName);
        if (subject == null) {
            throw new StuffNotFoundException("User with login " + loginName);
        }

        EntityTag eTag = new EntityTag(Long.toOctalString(subject.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);

        if (builder == null) {
            UserRest user = new UserRest(subject.getId(), subject.getName());
            user.setFirstName(subject.getFirstName());
            user.setLastName(subject.getLastName());
            user.setEmail(subject.getEmailAddress());
            user.setTel(subject.getPhoneNumber());

            MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
            builder = Response.ok(user, mediaType);
            builder.tag(eTag);

        }
        return builder.build();
    }

    @GET
    @Path("logout")
    @ApiOperation(value = "Force a REST logout by clearing the user cache")
    public void logout() {
        SimplePrincipal principal = new SimplePrincipal(caller.getName());
        log.debug("Forcing REST logout of user: [" + principal + "]");
        Object cachedValue = this.authCache.remove(principal);
        if (cachedValue != null) {
            log.debug("REST user logged out: [" + principal + "]" );
        }

    }

    private void updateResourceFavorites(Set<Integer> favIds) {
        Configuration conf = caller.getUserConfiguration();
        if (conf==null) {
            conf = new Configuration();
        }

        StringBuilder builder = buildFavStringFromSet(favIds);
        PropertySimple prop = conf.getSimple(RESOURCE_HEALTH_RESOURCES);
        if (prop == null) {
            conf.put(new PropertySimple(RESOURCE_HEALTH_RESOURCES, builder.toString()));
        }
        else {
            prop.setStringValue(builder.toString());
        }
        caller.setUserConfiguration(conf);
        subjectManager.updateSubject(caller, caller);
    }

    private void updateGroupFavorites(Set<Integer> favIds) {
        Configuration conf = caller.getUserConfiguration();
        if (conf==null) {
            conf = new Configuration();
        }

        StringBuilder builder = buildFavStringFromSet(favIds);
        PropertySimple prop = conf.getSimple(GROUP_HEALTH_GROUPS);
        if (prop == null) {
            conf.put(new PropertySimple(GROUP_HEALTH_GROUPS, builder.toString()));
        }
        else {
            prop.setStringValue(builder.toString());
        }
        caller.setUserConfiguration(conf);
        subjectManager.updateSubject(caller, caller);
    }

    private Set<Integer> getResourceIdsForFavorites() {
        Configuration conf = caller.getUserConfiguration();
        if (conf==null)
            return new HashSet<Integer>();
        String favsString = conf.getSimpleValue(RESOURCE_HEALTH_RESOURCES, "");
        Set<Integer> favIds = getIdsFromFavString(favsString);
        return favIds;
    }

    private Set<Integer> getGroupIdsForFavorites() {
        Configuration conf = caller.getUserConfiguration();
        if (conf==null) {
            return  new HashSet<Integer>();
        }
        String favsString = conf.getSimpleValue(GROUP_HEALTH_GROUPS, "");
        Set<Integer> favIds = getIdsFromFavString(favsString);
        return favIds;
    }

    /**
     * Parse the String with favorites.
     * The list of favorites is stored in the server as a list
     * of ids separated by a pipe '|' character.
     *
     * @param favsString String as stored in for the user
     * @return Set of ids of the favorites
     */
    private Set<Integer> getIdsFromFavString(String favsString) {
        Set<Integer> favIds = new TreeSet<Integer>();
        if (!favsString.isEmpty()) {
            String[] favStringArray = favsString.split("\\|");
            for (String tmp : favStringArray) {
                favIds.add(Integer.valueOf(tmp));
            }
        }
        return favIds;
    }

    /**
     * Create the String with favorites to store for the user
     * The list of favorites is stored in the server as a list
     * of ids separated by a pipe '|' character
     * @param favIds Set of favorite ids
     * @return String representation
     */
    private StringBuilder buildFavStringFromSet(Set<Integer> favIds) {
        StringBuilder builder = new StringBuilder();
        Iterator<Integer> iter = favIds.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (iter.hasNext()) {
                builder.append('|');
            }
        }
        return builder;
    }

}
