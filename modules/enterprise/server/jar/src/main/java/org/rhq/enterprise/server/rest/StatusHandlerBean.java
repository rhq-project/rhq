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

import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.rhq.core.domain.cloud.Server;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.rest.domain.Status;
import org.rhq.enterprise.server.rest.domain.StringValue;
import org.rhq.enterprise.server.system.SystemInfoManagerLocal;

/**
 * Return system status
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class StatusHandlerBean extends AbstractRestBean implements StatusHandlerLocal {


    @EJB
    SystemInfoManagerLocal infoMgr;
    @EJB
    ServerManagerLocal serverManager;

    @Override
    public Response getStatus(HttpHeaders httpHeaders) {

        Map<String,String> statusMap = infoMgr.getSystemInformation(caller);
        Status status = new Status();
        status.setValues(statusMap);

        MediaType mediaType = httpHeaders.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            String htmlString = renderTemplate("status",status);
            builder = Response.ok(htmlString,mediaType);
        } else  {
            builder = Response.ok(status, httpHeaders.getAcceptableMediaTypes().get(0));
        }

        return builder.build();
    }

    @Override
    public StringValue serverState() {
        Server server = serverManager.getServer();
        StringValue sv = new StringValue(server.getOperationMode().name());
        return sv;
    }
}
