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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.rest.domain.IntegerValue;
import org.rhq.enterprise.server.rest.domain.StringValue;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Deal with content
 * @author Heiko W. Rupp
 */
@Path("/content")
@Api(value="Content related", description = "This endpoint deals with content (upload)")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class ContentHandlerBean extends AbstractRestBean {

    private static final String TMP_FILE_PREFIX = "rhq-rest-";
    private static final String TMP_FILE_SUFFIX = ".bin";

    @POST
    @Path("/fresh")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
    @ApiOperation("Upload content to the server. This will return a handle that can be used later to retrieve and further process the content")
    public Response uploadContent(
        InputStream contentStream,
        @Context HttpHeaders headers,
        @Context UriInfo uriInfo) throws IOException
    {

        String tmpDirName = System.getProperty("java.io.tmpdir");

        File tmpDir = new File(tmpDirName);
        File outFile = File.createTempFile(TMP_FILE_PREFIX, TMP_FILE_SUFFIX,tmpDir);

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile));

        StreamUtil.copy(contentStream, bos, true);

        String fileHandle = outFile.getName();
        StringValue sv = new StringValue(fileHandle);

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/content/{handle}");
        URI uri = uriBuilder.build(fileHandle);


        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        Response.ResponseBuilder builder = Response.created(uri);
        builder.entity(sv);
        builder.type(mediaType);

        return builder.build();
    }

    @GET
    @Path("/{handle}/info")
    @ApiOperation("Retrieve the length of the content with the passed handle")
    public IntegerValue getInfo(
        @PathParam("handle") String handle
    )
    {

        File content = getFileForHandle(handle);

        if (!content.exists() || !content.canRead())
            throw new StuffNotFoundException("Content with handle " + handle);

        long len = content.length();

        IntegerValue iv = new IntegerValue((int)len); // TODO

        return iv;
    }

    @PUT
    @Path("/{handle}/plugins")
    @ApiErrors({
        @ApiError(code = 406, reason = "No name provided or invalid combination of parameters supplied"),
        @ApiError(code = 404, reason = "No content for handle found"),
        @ApiError(code = 403, reason = "Caller has not rights to upload plugins")
    })
    @ApiOperation(value="Put the uploaded content into the plugin drop box. ",
        notes = "This endpoint allows to deploy previously uploaded content as a plugin. You need to provide" +
            "a valid plugin (file) name in order for the plugin processing to succeed. Optionally you can" +
            "request that a plugin scan will be started and the plugin be registered in the system. You can also" +
            "specify a delay in milliseconds after which the plugin will be automatically pushed out to the agents." +
            "Note that a non-negative \"pushOutDelay\" only makes sense when the \"scan\" is set to true, otherwise" +
            "no update on the agents can occur because there will be no updated plugins on the server. If a " +
            "non-negative \"pushOutDelay\" is given together with \"scan\" set to false a 406 error is returned." +
            "The content identified by the handle is not removed. Note that this method is deprecated - use a PUT to /plugins.")
    @Deprecated
    public Response provideAsPlugin(
        @ApiParam("Name of the handle retrieved from upload") @PathParam("handle") String handle,
        @ApiParam("Name of the plugin file") @QueryParam("name") String name,
        @ApiParam("Should a discovery scan be started?") @QueryParam("scan") @DefaultValue("false") boolean startScan,
        @ApiParam(value = "The delay in millis before the agents update their plugins. Any negative value disables " +
            "the automatic update of agents", defaultValue = "-1")
            @QueryParam("pushOutDelay") @DefaultValue("-1") long pushOutDelay,
        @Context HttpHeaders headers
    ) {

        if (name==null || name.isEmpty()) {
            throw new BadArgumentException("A valid 'name' must be given");
        }

        File content = getFileForHandle(handle);
        if (!content.exists() || !content.canRead()) {
            throw new StuffNotFoundException("Content with handle " + handle);
        }

        Response.ResponseBuilder builder;

        try {
            boolean isAllowed = LookupUtil.getAuthorizationManager().hasGlobalPermission(caller,
                Permission.MANAGE_SETTINGS);
            if (!isAllowed) {
                log.error("An unauthorized user [" + caller + "] attempted to upload a plugin");
                throw new PermissionException("You are not authorized to do this");
            }

            //sanity checks before we do any real work
            if (pushOutDelay >= 0 && !startScan) {
                throw new BadArgumentException(
                    "Pushing changes to agents without starting a scan for changes first doesn't make sense.");
            }

            File dir = LookupUtil.getPluginManager().getPluginDropboxDirectory();
            File targetFile = new File(dir,name);


            FileOutputStream fos = new FileOutputStream(targetFile);
            try {
                FileInputStream fis = new FileInputStream(content);
                try {
                    StreamUtil.copy(fis, fos);
                } finally {
                    fis.close();
                }
            } finally {
                fos.close();
            }

            if (startScan) {
                PluginDeploymentScannerMBean scanner = LookupUtil.getPluginDeploymentScanner();
                scanner.scanAndRegister();
            }

            if (pushOutDelay >= 0) {
                LookupUtil.getPluginManager().schedulePluginUpdateOnAgents(caller, pushOutDelay);
            }

            builder=Response.ok();
        }
        catch (Exception e) {

            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            builder.entity(e.getMessage());
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        builder.type(mediaType);

        return builder.build();

    }


    @DELETE
    @Path("/{handle}")
    @ApiOperation(value = "Remove the content with the passed handle", notes = "This operation is by default idempotent, returning 204." +
                "If you want to check if the content existed at all, you need to pass the 'validate' query parameter.")
    @ApiErrors({
        @ApiError(code = 204, reason = "Content was deleted or did not exist with validation not set"),
        @ApiError(code = 404, reason = "Content did not exist and validate was set")
    })
    public Response removeUploadedContent(
        @PathParam("handle") String handle,
        @ApiParam("Validate if the content exists") @QueryParam("validate") @DefaultValue("false") boolean validate) {

        File content = getFileForHandle(handle);

        Response.ResponseBuilder builder;
        if (!content.exists()) {
            if (validate) {
                throw new StuffNotFoundException("Content with handle " + handle);
            }
            builder = Response.noContent();
        }
        else {
            boolean deleted = content.delete();
            if (deleted)
                builder = Response.noContent();
            else {
                builder = Response.serverError();
                System.err.println("Deletion of " + content.getAbsolutePath() + " failed");
            }
        }
        return builder.build();
    }

    private File getFileForHandle(String handle) {
        String tmpDirName = System.getProperty("java.io.tmpdir");

        File tmpDir = new File(tmpDirName);
        return new File(tmpDir,handle);
    }

}
