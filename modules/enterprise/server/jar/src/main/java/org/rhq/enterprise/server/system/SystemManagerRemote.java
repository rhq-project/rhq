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

import java.util.Properties;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;

/**
 * @author John Mazzitelli
 */
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface SystemManagerRemote {
    /**
     * Provides product information suitable for "About" details. 
     * 
     * @param subject user making the request
     * 
     * @return the product info
     */
    @WebMethod
    ProductInfo getProductInfo( //
        @WebParam(name = "subject") Subject subject);

    /**
     * Provides details (such as product version) of the server processing the request.  Requires MANAGE_SETTINGS. 
     * 
     * @param subject user making the request
     * 
     * @return server details
     */
    @WebMethod
    ServerDetails getServerDetails( //
        @WebParam(name = "subject") Subject subject);

    /**
     * Get the server cloud configuration. These are the server configurations that will be
     * the same for all servers in the HA server cloud.
     *
     * @param subject user making the request
     *
     * @return Properties
     */
    @WebMethod
    Properties getSystemConfiguration( //
        @WebParam(name = "subject") Subject subject);

    /**
     * Set the server cloud configuration.  The given properties will be the new settings
     * for all servers in the HA server cloud.
     *
     * @param subject        the user who wants to change the settings
     * @param properties     the new system configuration settings
     * @param skipValidation if true, validation will not be performed on the properties
     */
    @WebMethod
    void setSystemConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "properties") Properties properties, //
        @WebParam(name = "skipValidation") boolean skipValidation) throws Exception;
}
