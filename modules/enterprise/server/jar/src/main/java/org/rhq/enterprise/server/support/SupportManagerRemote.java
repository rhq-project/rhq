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
package org.rhq.enterprise.server.support;

import java.net.URL;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.rhq.core.domain.auth.Subject;

/**
 * Provides some methods that are useful for supporting managed resources. This includes being
 * able to take a snapshot report of a managed resource, such as its log files, data files, and anything
 * the managed resource wants to expose.
 * 
 * @author John Mazzitelli
 */
@WebService
@Remote
public interface SupportManagerRemote {
    /**
     * Asks that a snapshot report be taken of the given resource. Snapshot reports consist of things like
     * log files, data files and configuration files. What is included in snapshot reports is controlled
     * by the resource's plugin configuration. A snapshot report is compressed as a zip file.
     * 
     * @param subject the user requesting the snapshot
     * @param resourceId the resource whose snapshot report is to be taken
     * @param name the name of the snapshot report
     * @param description a description for the caller to use to describe the purpose for taking the snapshot report 
     * @return a URL that the caller can use to obtain the snapshot report
     * @throws Exception
     */
    @WebMethod
    URL getSnapshotReport( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "description") String description) throws Exception;
}
