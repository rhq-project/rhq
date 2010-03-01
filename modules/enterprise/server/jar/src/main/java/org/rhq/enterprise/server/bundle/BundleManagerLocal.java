/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.bundle;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeployDefinitionCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * Local interface to the manager responsible for creating and managing bundles.
 *  
 * @author John Mazzitelli
 */
@Local
public interface BundleManagerLocal {

    /**
     * @param subject must be InventoryManager
     * @param bundle required fields: name 
     * @return the persisted Bundle (id is assigned)
     */
    Bundle createBundle(Subject subject, Bundle bundle) throws Exception;

    /**
     * @param subject must be InventoryManager
     * @param bundleType required fields: name 
     * @return the persisted BundleType (id is assigned)
     */
    BundleType createBundleType(Subject subject, BundleType bundleType) throws Exception;

    /**
     * @param subject must be InventoryManager
     * @param int bundleId the bundle for which this will be the next version
     * @param bundleVersion required fields: recipe (the full recipe as a String)
     * @return the persisted BundleVersion (id is assigned)
     */
    BundleVersion createBundleVersion(Subject subject, int bundleId, BundleVersion bundleVersion) throws Exception;

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The remaining methods are shared with the Remote Interface.
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    PageList<BundleDeployDefinition> findBundleDeployDefinitionsByCriteria(Subject subject,
        BundleDeployDefinitionCriteria criteria);

    PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria);

    PageList<BundleVersion> findBundleVersionsByCriteria(Subject subject, BundleVersionCriteria criteria);

    PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria);

    List<BundleType> getAllBundleTypes(Subject subject);

    void deleteBundles(Subject subject, int[] bundleIds);

    void deleteBundleVersions(Subject subject, int[] bundleVersionIds);
}
