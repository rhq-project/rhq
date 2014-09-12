/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.bundle.composite.BundleGroupAssignmentComposite;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleGroupCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;

public interface BundleGWTService extends RemoteService {

    ResourceTypeBundleConfiguration getResourceTypeBundleConfiguration(int compatGroupId) throws RuntimeException;

    BundleVersion createOrStoreBundleVersionViaURL(String url, String username, String password)
        throws RuntimeException;

    BundleVersion createInitialBundleVersionViaRecipe(int[] bundleGroupIds, String recipe) throws RuntimeException;

    BundleVersion createInitialBundleVersionViaToken(int[] bundleGroupIds, String token) throws RuntimeException;

    BundleVersion createBundleVersionViaRecipe(String recipe) throws RuntimeException;

    BundleDeployment createBundleDeployment(int bundleVersionId, int bundleDestinationId, String description,
        Configuration configuration, boolean enforcePolicy, int enforcementInterval, boolean pinToBundle)
        throws RuntimeException;

    BundleDestination createBundleDestination(int bundleId, String name, String description, String destBaseDirName,
        String deployDir, int groupId) throws RuntimeException;

    void deleteBundles(int[] bundleIds) throws RuntimeException;

    void deleteBundle(int bundleId) throws RuntimeException;

    void deleteBundleDeployment(int bundleDeploymentId) throws RuntimeException;

    void deleteBundleDestination(int bundleDestinationId) throws RuntimeException;

    void deleteBundleVersion(int bundleVersionId, boolean deleteBundleIfEmpty) throws RuntimeException;

    PageList<Bundle> findBundlesByCriteria(BundleCriteria criteria) throws RuntimeException;

    PageList<BundleGroup> findBundleGroupsByCriteria(BundleGroupCriteria criteria) throws RuntimeException;

    PageList<BundleDeployment> findBundleDeploymentsByCriteria(BundleDeploymentCriteria criteria)
        throws RuntimeException;

    PageList<BundleDestination> findBundleDestinationsByCriteria(BundleDestinationCriteria criteria)
        throws RuntimeException;

    PageList<BundleFile> findBundleFilesByCriteria(BundleFileCriteria criteria) throws RuntimeException;

    PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria(BundleResourceDeploymentCriteria criteria)
        throws RuntimeException;

    PageList<BundleVersion> findBundleVersionsByCriteria(BundleVersionCriteria criteria) throws RuntimeException;

    PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria(BundleCriteria criteria)
        throws RuntimeException;

    HashMap<String, Boolean> getAllBundleVersionFilenames(int bundleVersionId) throws RuntimeException;

    ArrayList<BundleType> getAllBundleTypes() throws RuntimeException;

    String getBundleDeploymentName(int bundleDestinationId, int bundleVersionId, int prevDeploymentId)
        throws RuntimeException;

    BundleDeployment scheduleBundleDeployment(int bundleDeploymentId, boolean isCleanDeployment)
        throws RuntimeException;

    BundleDeployment scheduleRevertBundleDeployment(int bundleDestinationId, String deploymentDescription,
        boolean isCleanDeployment) throws RuntimeException;

    void purgeBundleDestination(int bundleDestinationId) throws RuntimeException;

    BundleGroup createBundleGroup(BundleGroup bundleGroup) throws RuntimeException;

    void deleteBundleGroups(int[] bundleGroupIds) throws RuntimeException;

    BundleGroup updateBundleGroup(BundleGroup bundleGroup) throws RuntimeException;

    BundleGroupAssignmentComposite getAssignableBundleGroups(int bundleId) throws RuntimeException;

    void assignBundlesToBundleGroups(int[] bundleGroupIds, int[] bundleIds) throws RuntimeException;

    void unassignBundlesFromBundleGroups(int[] bundleGroupIds, int[] bundleIds) throws RuntimeException;

    List<BundleResourceDeploymentHistory> getBundleResourceDeploymentHistories(int resourceDeploymentId);
}
