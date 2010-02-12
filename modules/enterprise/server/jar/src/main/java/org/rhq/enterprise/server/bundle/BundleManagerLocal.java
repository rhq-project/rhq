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
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Local interface to the manager responsible for creating and managing bundles.
 *  
 * @author John Mazzitelli
 */
@Local
public interface BundleManagerLocal {
    Bundle createBundle(Subject subject, Bundle b);

    BundleType createBundleType(Subject subject, BundleType bundleType);

    BundleVersion createBundleVersion(Subject subject, BundleVersion bundleVersion);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The remaining methods are shared with the Remote Interface.
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    PageList<BundleDeployDefinition> findBundleDeployDefinitionsByCriteria(Subject subject,
        BundleDeployDefinitionCriteria criteria);

    PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria);

    PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria);

    List<BundleType> getAllBundleTypes(Subject subject);

    void deleteBundles(Subject subject, int[] bundleIds);
}
