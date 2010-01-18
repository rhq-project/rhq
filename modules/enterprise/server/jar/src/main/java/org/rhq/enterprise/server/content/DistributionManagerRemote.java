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
package org.rhq.enterprise.server.content;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionType;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.enterprise.server.system.ServerVersion;

import java.util.List;

/**
 * @author Pradeep Kilambi
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface DistributionManagerRemote {
    /**
     * Creates a new kickstart tree in the system. If the tree does not exist, it will be created.
     * If a tree exists with the specified version ID, a new one will not be created and the
     * existing tree will be returned.
     * @param subject
     * @param kslabel
     * @param basepath
     * @return newly created kickstart tree object
     */
    @WebMethod
    Distribution createDistribution( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "kslabel") String kslabel, //
        @WebParam(name = "basepath") String basepath, //
        @WebParam(name = "disttype") DistributionType disttype) throws DistributionException;

    /**
     * Deletes a given instance of kickstart tree object. If the object does not exist
     * @param subject
     * @param distId
     *
     */
    @WebMethod
    void deleteDistributionByDistId( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "distId") int distId) throws Exception;

    /**
     * get kickstart tree based on a given label
     * @param kslabel kickstart tree label
     * @return kickstart tree object
     */
    @WebMethod
    Distribution getDistributionByLabel(@WebParam(name = "kslabel") String kslabel);

    /**
     * get kickstart tree based on base path
     * @param basepath location on filesystem
     * @return kstree object
     */
    @WebMethod
    Distribution getDistributionByPath(@WebParam(name = "basepath") String basepath);

    /**
     * get distribution file based on distro id
     * @param distid
     * @return
     */
    @WebMethod
    List<DistributionFile> getDistributionFilesByDistId(@WebParam(name = "distid") int distid);

    /**
     * delete distribution file based on distro id
     * @param distid
     */
    @WebMethod
    void deleteDistributionFilesByDistId(@WebParam(name = "subject") Subject subject, //
         @WebParam(name = "distid") int distid);

    /**
     * Returns a DistributionType for given name
     * @param name name of distribution type
     * @return distribution type from db
     */
    @WebMethod
    DistributionType getDistributionTypeByName(@WebParam(name = "name") String name);

}
