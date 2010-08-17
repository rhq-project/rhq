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

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.DistributionType;

/**
 * @author Pradeep Kilambi
 */
@Local
public interface DistributionManagerLocal {
    /**
     * Creates a new distribution in the system. If the tree does not exist, it will be created.
     * If a tree exists with the specified version ID, a new one will not be created and the
     * existing tree will be returned.
     * @param subject
     * @param kslabel kickstart tree label
     * @param basepath  ks base path on filesystem
     * @return newly created distribution tree object
     */
    Distribution createDistribution(Subject subject, String kslabel, String basepath, DistributionType disttype)
        throws DistributionException;

    /**
     * Deletes a given instance of distribution object. If the object does not exist
     * @param subject
     * @param repoId
     */
    void deleteDistributionMappingsForRepo(Subject subject, int repoId);

    /**
     * Deletes a given instance of distribution object. If the object does not exist
     * @param subject
     * @param distId
     */
    void deleteDistributionByDistId(Subject subject, int distId);

    /**
     * get distribution based on a given label
     * @param kslabel distribution tree label
     * @return kickstart tree object
     */
    Distribution getDistributionByLabel(String kslabel);

    /**
     * get list of distribution files
     * @param distid
     * @return list of distro file by dist id
     */
    List<DistributionFile> getDistributionFilesByDistId(int distid);

    /**
    * delete list of distribution files
     * @param subject
    * @param distid
    *
    */
    void deleteDistributionFilesByDistId(Subject subject, int distid);

    /**
     * Returns a DistributionType for given name
     * @param name name of distribution type
     * @return distribution type from db
     */
    DistributionType getDistributionTypeByName(String name);
}
