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

package org.rhq.coregui.server.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
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
import org.rhq.coregui.client.gwt.BundleGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class BundleGWTServiceImpl extends AbstractGWTServiceImpl implements BundleGWTService {
    private static final long serialVersionUID = 1L;

    private BundleManagerLocal bundleManager = LookupUtil.getBundleManager();

    @Override
    public ResourceTypeBundleConfiguration getResourceTypeBundleConfiguration(int compatGroupId)
        throws RuntimeException {
        try {
            ResourceTypeBundleConfiguration results = bundleManager.getResourceTypeBundleConfiguration(
                getSessionSubject(), compatGroupId);
            return SerialUtility.prepare(results, "getResourceTypeBundleConfiguration");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleVersion createOrStoreBundleVersionViaURL(String url, String username, String password)
        throws RuntimeException {
        try {
            BundleVersion results = bundleManager.createBundleVersionViaURL(getSessionSubject(), url, username,
                password);
            return SerialUtility.prepare(results, "createOrStoreBundleVersionViaURL");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleGroup createBundleGroup(BundleGroup bundleGroup) throws RuntimeException {
        try {
            BundleGroup results = bundleManager.createBundleGroup(getSessionSubject(), bundleGroup);
            return SerialUtility.prepare(results, "createBundleGroup");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleVersion createBundleVersionViaRecipe(String recipe) throws RuntimeException {
        try {
            BundleVersion results = bundleManager.createBundleVersionViaRecipe(getSessionSubject(), recipe);
            return SerialUtility.prepare(results, "createBundleVersionViaRecipe");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleVersion createInitialBundleVersionViaRecipe(int[] bundleGroupIds, String recipe)
        throws RuntimeException {
        try {
            BundleVersion results = bundleManager.createInitialBundleVersionViaRecipe(getSessionSubject(),
                bundleGroupIds, recipe);
            return SerialUtility.prepare(results, "createInitialBundleVersionViaRecipe");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleVersion createInitialBundleVersionViaToken(int[] bundleGroupIds, String token) throws RuntimeException {
        try {
            BundleVersion results = bundleManager.createInitialBundleVersionViaToken(getSessionSubject(),
                bundleGroupIds, token);
            return SerialUtility.prepare(results, "createInitialBundleVersionViaToken");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleDeployment createBundleDeployment(int bundleVersionId, int bundleDestinationId, String description,
        Configuration configuration, boolean enforcePolicy, int enforcementInterval, boolean pinToBundle)
        throws RuntimeException {

        try {
            BundleDeployment result = bundleManager.createBundleDeployment(getSessionSubject(), bundleVersionId,
                bundleDestinationId, description, configuration);
            return SerialUtility.prepare(result, "createBundleDeployment");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleDestination createBundleDestination(int bundleId, String name, String description,
        String destBaseDirName, String deployDir, int groupId) throws RuntimeException {

        try {
            BundleDestination result = bundleManager.createBundleDestination(getSessionSubject(), bundleId, name,
                description, destBaseDirName, deployDir, groupId);
            return SerialUtility.prepare(result, "createBundleDestination");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deleteBundles(int[] bundleIds) throws RuntimeException {
        try {
            bundleManager.deleteBundles(getSessionSubject(), bundleIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deleteBundle(int bundleId) throws RuntimeException {
        try {
            bundleManager.deleteBundle(getSessionSubject(), bundleId);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deleteBundleGroups(int[] bundleGroupIds) throws RuntimeException {
        try {
            bundleManager.deleteBundleGroups(getSessionSubject(), bundleGroupIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deleteBundleDeployment(int bundleDeploymentId) throws RuntimeException {
        try {
            bundleManager.deleteBundleDeployment(getSessionSubject(), bundleDeploymentId);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deleteBundleDestination(int bundleDestinationId) throws RuntimeException {
        try {
            bundleManager.deleteBundleDestination(getSessionSubject(), bundleDestinationId);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void deleteBundleVersion(int bundleVersionId, boolean deleteBundleIfEmpty) throws RuntimeException {
        try {
            bundleManager.deleteBundleVersion(getSessionSubject(), bundleVersionId, deleteBundleIfEmpty);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<BundleType> getAllBundleTypes() throws RuntimeException {
        try {
            ArrayList<BundleType> bundleTypes = new ArrayList<BundleType>();
            bundleTypes.addAll(bundleManager.getAllBundleTypes(getSessionSubject()));
            return SerialUtility.prepare(bundleTypes, "getAllBundleTypes");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public HashMap<String, Boolean> getAllBundleVersionFilenames(int bundleVersionId) throws RuntimeException {
        HashMap<String, Boolean> results = new HashMap<String, Boolean>();
        try {
            results.putAll(bundleManager.getAllBundleVersionFilenames(getSessionSubject(), bundleVersionId));
            return SerialUtility.prepare(results, "getAllBundleVersionFilenames");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public String getBundleDeploymentName(int bundleDestinationId, int bundleVersionId, int prevDeploymentId)
        throws RuntimeException {
        String result;
        try {
            result = bundleManager.getBundleDeploymentName(getSessionSubject(), bundleDestinationId, bundleVersionId,
                prevDeploymentId);
            return SerialUtility.prepare(result, "getBundleDeploymentName");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleDeployment scheduleBundleDeployment(int bundleDeploymentId, boolean isCleanDeployment)
        throws RuntimeException {
        try {
            BundleDeployment result = bundleManager.scheduleBundleDeployment(getSessionSubject(), bundleDeploymentId,
                isCleanDeployment);
            return SerialUtility.prepare(result, "scheduleBundleDeployment");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleDeployment scheduleRevertBundleDeployment(int bundleDeploymentId, String deploymentDescription,
        boolean isCleanDeployment) throws RuntimeException {
        try {
            BundleDeployment result = bundleManager.scheduleRevertBundleDeployment(getSessionSubject(),
                bundleDeploymentId, deploymentDescription, isCleanDeployment);
            return SerialUtility.prepare(result, "scheduleRevertBundleDeployment");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<Bundle> findBundlesByCriteria(BundleCriteria criteria) throws RuntimeException {
        try {
            PageList<Bundle> results = bundleManager.findBundlesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "findBundlesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<BundleGroup> findBundleGroupsByCriteria(BundleGroupCriteria criteria) throws RuntimeException {
        try {
            PageList<BundleGroup> results = bundleManager.findBundleGroupsByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "findBundleGroupsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<BundleDeployment> findBundleDeploymentsByCriteria(BundleDeploymentCriteria criteria)
        throws RuntimeException {
        try {
            PageList<BundleDeployment> result = bundleManager.findBundleDeploymentsByCriteria(getSessionSubject(),
                criteria);
            return SerialUtility.prepare(result, "BundleService.findBundleDeploymentsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<BundleDestination> findBundleDestinationsByCriteria(BundleDestinationCriteria criteria)
        throws RuntimeException {
        try {
            PageList<BundleDestination> result = bundleManager.findBundleDestinationsByCriteria(getSessionSubject(),
                criteria);
            return SerialUtility.prepare(result, "BundleService.findBundleDestinationsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<BundleFile> findBundleFilesByCriteria(BundleFileCriteria criteria) throws RuntimeException {
        try {
            PageList<BundleFile> result = bundleManager.findBundleFilesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(result, "BundleService.findBundleFilesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria(
        BundleResourceDeploymentCriteria criteria) throws RuntimeException {
        try {
            PageList<BundleResourceDeployment> result = bundleManager.findBundleResourceDeploymentsByCriteria(
                getSessionSubject(), criteria);
            return SerialUtility.prepare(result, "BundleService.findBundleResourceDeploymentsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<BundleVersion> findBundleVersionsByCriteria(BundleVersionCriteria criteria) throws RuntimeException {
        try {
            PageList<BundleVersion> results = bundleManager.findBundleVersionsByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "findBundleVersionsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria(
        BundleCriteria criteria) throws RuntimeException {
        try {
            PageList<BundleWithLatestVersionComposite> results;
            results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "findBundlesWithLatestVersionCompositesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void purgeBundleDestination(int bundleDestinationId) throws RuntimeException {
        try {
            bundleManager.purgeBundleDestination(getSessionSubject(), bundleDestinationId);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleGroup updateBundleGroup(BundleGroup bundleGroup) throws RuntimeException {
        try {
            BundleGroup results = bundleManager.updateBundleGroup(getSessionSubject(), bundleGroup);
            return SerialUtility.prepare(results, "updateBundleGroup");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public BundleGroupAssignmentComposite getAssignableBundleGroups(int bundleId) throws RuntimeException {
        try {
            Subject subject = getSessionSubject();
            BundleGroupAssignmentComposite results = bundleManager
                .getAssignableBundleGroups(subject, subject, bundleId);
            return SerialUtility.prepare(results, "getAssignableBundleGroups");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void assignBundlesToBundleGroups(int[] bundleGroupIds, int[] bundleIds) throws RuntimeException {
        try {
            Subject subject = getSessionSubject();
            bundleManager.assignBundlesToBundleGroups(subject, bundleGroupIds, bundleIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void unassignBundlesFromBundleGroups(int[] bundleGroupIds, int[] bundleIds) throws RuntimeException {
        try {
            Subject subject = getSessionSubject();
            bundleManager.unassignBundlesFromBundleGroups(subject, bundleGroupIds, bundleIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public List<BundleResourceDeploymentHistory> getBundleResourceDeploymentHistories(int resourceDeploymentId) {
        try {
            Subject subject = getSessionSubject();
            List<BundleResourceDeploymentHistory> results = bundleManager.getBundleResourceDeploymentHistories(subject,
                    resourceDeploymentId);
            return SerialUtility.prepare(results, "getBundleResourceDeploymentHistories");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}
