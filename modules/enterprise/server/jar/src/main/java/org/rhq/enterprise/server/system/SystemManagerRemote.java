/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.system;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;

/**
 * @author John Mazzitelli
 */
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface SystemManagerRemote {
    /**
     * Provides version information of the server processing the request. 
     * 
     * @return server version information
     */
    @WebMethod
    ServerVersion getServerVersion( //
        @WebParam(name = "subject") Subject subject) throws Exception;

    @WebMethod
    ProductInfo getProductInfo(
        @WebParam(name = "subject") Subject subject);
}
