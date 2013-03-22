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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import com.wordnik.swagger.annotations.ApiOperation;

import org.rhq.enterprise.server.rest.domain.IntegerValue;
import org.rhq.enterprise.server.rest.domain.StringValue;

/**
 * Deal with content
 * @author Heiko W. Rupp
 */
@Path("/content")
@Api(value="Resource related", description = "This endpoint deals with individual resources, not resource groups")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class ContentHandlerBean extends AbstractRestBean {


    @POST
    @Path("/fresh")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
    @ApiOperation("Upload content to the server. This will return a handle that can be used later to retrieve the content")
    public Response uploadContent(
        InputStream contentStream,
        @Context HttpHeaders headers,
        @Context UriInfo uriInfo) throws IOException
    {

        String tmpDirName = System.getProperty("java.io.tmpdir");

        File tmpDir = new File(tmpDirName);
        File outFile = File.createTempFile("rhq-rest-",".bin",tmpDir);

        FileOutputStream fos = new FileOutputStream(outFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        BufferedInputStream bis = new BufferedInputStream(contentStream);

        byte[] buf = new byte[32768]; // 32k

        int data;
        int off=0;
        while ((data=bis.read(buf))!=-1) {
            bos.write(buf,off,data);
            off+=data;
        }

        bos.flush();
        bos.close();
        bis.close();

        String fileHandle = outFile.getName();
        StringValue sv = new StringValue(fileHandle);

        System.out.println("Uploaded content to " + outFile.getAbsolutePath());

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


    @DELETE
    @Path("/{handle}")
    public Response removeUploadedContent(
        @PathParam("handle") String handle
        )
    {
        File content = getFileForHandle(handle);

        Response.ResponseBuilder builder;
        if (!content.exists())
            builder = Response.noContent();

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
