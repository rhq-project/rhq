/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;

public interface BundleGWTService extends RemoteService {

    BundleVersion createBundleVersion(int bundleId, String name, String version, String recipe) throws Exception;

    BundleVersion createBundleVersionViaURL(String url) throws Exception;

    BundleVersion createBundleVersionViaRecipe(String recipe) throws Exception;

    BundleDeployment createBundleDeployment(int bundleVersionId, int bundleDestinationId, String description,
        Configuration configuration, boolean enforcePolicy, int enforcementInterval, boolean pinToBundle)
        throws Exception;

    BundleDestination createBundleDestination(int bundleId, String name, String description, String deployDir,
        int groupId) throws Exception;

    void deleteBundles(int[] bundleIds) throws Exception;

    void deleteBundle(int bundleId) throws Exception;

    void deleteBundleDeployment(int bundleDeploymentId) throws Exception;

    void deleteBundleDestination(int bundleDestinationId) throws Exception;

    void deleteBundleVersion(int bundleVersionId, boolean deleteBundleIfEmpty) throws Exception;

    PageList<Bundle> findBundlesByCriteria(BundleCriteria criteria) throws Exception;

    PageList<BundleDeployment> findBundleDeploymentsByCriteria(BundleDeploymentCriteria criteria);

    PageList<BundleDestination> findBundleDestinationsByCriteria(BundleDestinationCriteria criteria);

    PageList<BundleFile> findBundleFilesByCriteria(BundleFileCriteria criteria);

    PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria(BundleResourceDeploymentCriteria criteria);

    PageList<BundleVersion> findBundleVersionsByCriteria(BundleVersionCriteria criteria) throws Exception;

    PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria(BundleCriteria criteria)
        throws Exception;

    HashMap<String, Boolean> getAllBundleVersionFilenames(int bundleVersionId) throws Exception;

    ArrayList<BundleType> getAllBundleTypes() throws Exception;

    String getBundleDeploymentName(int bundleDestinationId, int bundleVersionId, int prevDeploymentId);

    BundleDeployment scheduleBundleDeployment(int bundleDeploymentId, boolean isCleanDeployment) throws Exception;

    BundleDeployment scheduleRevertBundleDeployment(int bundleDestinationId, String deploymentDescription,
        boolean isCleanDeployment) throws Exception;

}