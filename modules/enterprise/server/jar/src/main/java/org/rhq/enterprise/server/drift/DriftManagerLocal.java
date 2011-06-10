/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.server.drift;

import java.io.File;
import java.io.InputStream;

import javax.ejb.Local;

@Local
public interface DriftManagerLocal {

    /**
     * This method initiates an out-of-band (JMS-Based) server-side pull of the change-set file. Upon successful
     * upload of the change-set, it is processed. This may in turn generated requests for drift files to
     * be persisted.
     *  
     * @param resourceId The resource for which the change-set is being reported.
     * @param zipSize The size of the zip waiting to be streamed.
     * @param zipStream The change-set zip file stream
     * @throws Exception
     */
    void addChangeset(int resourceId, long zipSize, InputStream zipStream) throws Exception;

    /**
     * This method initiates an out-of-band (JMS-Based) server-side pull of the drift file zip. Upon successful
     * upload of the zip, the files are stored.
     *  
     * @param resourceId The resource from which the drift file is being supplied.
     * @param zipSize The size of the zip waiting to be streamed.
     * @param zipStream The drift files zip file stream
     * @throws Exception
     */
    void addFiles(int resourceId, long zipSize, InputStream zipStream) throws Exception;

    /**
     * This method stores the provided change-set file for the resource. The version will be incremented based
     * on the max version of existing change-sets for the resource. The change-set will be processed generating
     * requests for drift file content and/or drift instances as required.
     *  
     * @param resourceId The resource for which the change-set is being reported.
     * @param zipSize The size of the zip waiting to be streamed.
     * @param zipStream The change-set zip file stream
     * @throws Exception
     */
    void storeChangeset(int resourceId, File changeset) throws Exception;

}
