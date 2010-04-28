/*
 * RHQ Management
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.bundle.BundleAgentService;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleRequest;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleResponse;
import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentAction;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.util.MessageDigestGenerator;

/**
 * Manages the bundle subsystem, which allows bundles of content to be installed. 
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author John Mazzitelli
 */
public class BundleManager extends AgentService implements BundleAgentService, BundleManagerProvider, ContainerService {
    private final Log log = LogFactory.getLog(BundleManager.class);

    private PluginContainerConfiguration configuration;

    public BundleManager() {
        super(BundleAgentService.class);
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void initialize() {
    }

    public void shutdown() {
    }

    public List<PackageVersion> getAllBundleVersionPackageVersions(BundleVersion bundleVersion) throws Exception {
        int bvId = bundleVersion.getId();
        List<PackageVersion> pvs = getBundleServerService().getAllBundleVersionPackageVersions(bvId);
        return pvs;
    }

    public long getFileContent(PackageVersion packageVersion, OutputStream outputStream) throws Exception {
        outputStream = remoteOutputStream(outputStream);
        long size = getBundleServerService().downloadPackageBits(packageVersion, outputStream);
        return size;
    }

    public BundleScheduleResponse schedule(BundleScheduleRequest request) {
        BundleScheduleResponse response = new BundleScheduleResponse();

        try {
            BundleResourceDeployment resourceDeployment = request.getBundleResourceDeployment();
            BundleDeployment bundleDeployment = resourceDeployment.getBundleDeployment();

            // find the resource that will handle the bundle processing
            InventoryManager im = PluginContainer.getInstance().getInventoryManager();
            BundleType bundleType = bundleDeployment.getBundleVersion().getBundle().getBundleType();
            ResourceType resourceType = bundleType.getResourceType();
            Set<Resource> resources = im.getResourcesWithType(resourceType);
            if (resources.isEmpty()) {
                throw new Exception("No bundle plugin supports bundle type [" + bundleType + "]");
            }
            int bundleHandlerResourceId = resources.iterator().next().getId();

            auditDeployment(resourceDeployment, BundleDeploymentAction.DEPLOYMENT_SCHEDULED,
                BundleDeploymentStatus.SUCCESS, "Scheduled deployment time: "
                    + request.getRequestedDeployTimeAsString() + " (immediate)");

            // TODO: The logic below this point should be executed asynchronously in response to an actual
            //       scheduling mechanism.  For now all deployments are "immediate" so just do it here.

            // pull down the bundle files that the plugin will need in order to process the bundle
            ResourceContainer resourceContainer = im.getResourceContainer(bundleHandlerResourceId);
            if (null == resourceContainer.getResourceContext()) {
                throw new Exception("No bundle plugin resource available to handle deployment for bundle type ["
                    + bundleType + "]. Ensure it is imported into inventory.");
            }
            File pluginTmpDir = resourceContainer.getResourceContext().getTemporaryDirectory();
            File bundleFilesDir = new File(pluginTmpDir, "" + bundleDeployment.getBundleVersion().getId());
            Map<PackageVersion, File> downloadedFiles = downloadBundleFiles(resourceDeployment, bundleFilesDir);

            // get the bundle facet object that will process the bundle
            int facetMethodTimeout = 4 * 60 * 60 * 1000; // 4 hours is given to the bundle plugin to do its thing
            BundleFacet bundlePluginComponent = getBundleFacet(bundleHandlerResourceId, facetMethodTimeout);

            // deploy the bundle utilizing the bundle facet object
            String deploymentMessage = "Deployment [" + bundleDeployment + "] to [" + resourceDeployment.getResource()
                + "]";
            auditDeployment(resourceDeployment, BundleDeploymentAction.DEPLOYMENT, BundleDeploymentStatus.INPROGRESS,
                deploymentMessage);

            BundleDeployRequest deployRequest = new BundleDeployRequest();
            deployRequest.setBundleManagerProvider(this);
            deployRequest.setResourceDeployment(resourceDeployment);
            deployRequest.setBundleFilesLocation(bundleFilesDir);
            deployRequest.setPackageVersionFiles(downloadedFiles);
            BundleDeployResult result = bundlePluginComponent.deployBundle(deployRequest);
            if (!result.isSuccess()) {
                response.setErrorMessage(result.getErrorMessage());
            }
            completeDeployment(resourceDeployment, BundleDeploymentStatus.SUCCESS, deploymentMessage);
        } catch (Throwable t) {
            log.error("Failed to schedule bundle request: " + request, t);
            response.setErrorMessage(t);
        }

        return response;
    }

    public void auditDeployment(BundleResourceDeployment bundleResourceeployment, BundleDeploymentAction action,
        BundleDeploymentStatus status, String message) {
        if (null == status) {
            status = BundleDeploymentStatus.NOCHANGE;
        }
        BundleResourceDeploymentHistory history = new BundleResourceDeploymentHistory("Bundle Plugin", action, status,
            message);
        log.debug("Reporting deployment step [" + history + "] to Server...");
        getBundleServerService().addDeploymentHistory(bundleResourceeployment.getId(), history);
    }

    /**
     * Downloads the bundle's files into the bundle plugin's tmp directory and returns that tmp directory. 
     * 
     * @param resourceDeployment access to deployment information, including what bundle files need to be downloaded
     * @param downloadDir location where the bundle files should be downloaded
     * @return map of the package versions to their files that were downloaded
     * @throws Exception
     */
    private Map<PackageVersion, File> downloadBundleFiles(BundleResourceDeployment resourceDeployment, File downloadDir)
        throws Exception {

        BundleDeployment bundleDeployment = resourceDeployment.getBundleDeployment();
        BundleVersion bundleVersion = bundleDeployment.getBundleVersion();

        // download all the bundle files to the bundle plugin's tmp directory
        auditDeployment(resourceDeployment, BundleDeploymentAction.FILE_DOWNLOAD, BundleDeploymentStatus.INPROGRESS,
            null);

        Map<PackageVersion, File> packageVersionFiles = new HashMap<PackageVersion, File>();
        List<PackageVersion> packageVersions = getAllBundleVersionPackageVersions(bundleVersion);
        for (PackageVersion packageVersion : packageVersions) {
            File packageFile = new File(downloadDir, packageVersion.getFileName());

            try {
                verifyHash(packageVersion, packageFile);
            } catch (Exception e) {
                // file either doesn't exist or it hash doesn't match, download a new copy
                packageFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(packageFile);
                try {
                    auditDeployment(resourceDeployment, BundleDeploymentAction.DEPLOYMENT_STEP,
                        BundleDeploymentStatus.NOCHANGE, "Downloading [" + packageVersion + "]");
                    long size = getFileContent(packageVersion, fos);
                    if (packageVersion.getFileSize() != null && size != packageVersion.getFileSize().longValue()) {
                        log.warn("Downloaded bundle file [" + packageVersion + "] but its size was [" + size
                            + "] when it was expected to be [" + packageVersion.getFileSize() + "].");
                    }
                } finally {
                    fos.close();
                }

                // now try to verify it again, if this throws an exception, that is very bad and we need to abort
                verifyHash(packageVersion, packageFile);
            }

            packageVersionFiles.put(packageVersion, packageFile);
        }

        auditDeployment(resourceDeployment, BundleDeploymentAction.FILE_DOWNLOAD, BundleDeploymentStatus.SUCCESS, null);

        return packageVersionFiles;
    }

    private void completeDeployment(BundleResourceDeployment resourceDeployment, BundleDeploymentStatus status,
        String message) {
        getBundleServerService().setBundleDeploymentStatus(resourceDeployment.getId(), status);
        auditDeployment(resourceDeployment, BundleDeploymentAction.DEPLOYMENT, status, message);
    }

    /**
     * Checks to see if the package file's hash matches that of the given package version.
     * If the file doesn't exist or the hash doesn't match, an exception is thrown.
     * This method returns normally if the hash matches the file.
     * If there is no known hash in the package version, this method returns normally.
     * 
     * @param packageVersion contains the hash that is expected
     * @param packageFile the local file whose hash is to be checked
     * @throws Exception if the file does not match the hash or the file doesn't exist
     */
    private void verifyHash(PackageVersion packageVersion, File packageFile) throws Exception {
        if (!packageFile.exists()) {
            throw new Exception("Package version [" + packageVersion + "] does not exist, cannot check hash");
        }

        if (packageVersion.getMD5() != null) {
            String realMD5 = new MessageDigestGenerator(MessageDigestGenerator.MD5).calcDigestString(packageFile);
            if (!packageVersion.getMD5().equals(realMD5)) {
                throw new Exception("Package version [" + packageVersion + "] failed MD5 check. expected=["
                    + packageVersion.getMD5() + "], actual=[" + realMD5 + "]");
            }
        } else if (packageVersion.getSHA256() != null) {
            FileInputStream is = new FileInputStream(packageFile);
            try {
                String realSHA256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(is);
                if (!packageVersion.getSHA256().equals(realSHA256)) {
                    throw new Exception("Package version [" + packageVersion + "] failed SHA256 check. expected=["
                        + packageVersion.getSHA256() + "], actual=[" + realSHA256 + "]");
                }
            } finally {
                is.close();
            }

        } else {
            log.debug("Package version [" + packageVersion + "] has no MD5/SHA256 hash - not verifying it");
        }

        return;
    }

    /**
     * If this manager can talk to a server-side {@link BundleServerService}, a proxy to that service is returned.
     *
     * @return the server-side proxy; <code>null</code> if this manager doesn't have a server to talk to
     */
    private BundleServerService getBundleServerService() {
        if (configuration.getServerServices() != null) {
            return configuration.getServerServices().getBundleServerService();
        }

        throw new IllegalStateException("There is no bundle server service available to obtain bundle files");
    }

    /**
     * Given a resource, this obtains that resource's {@link BundleFacet} interface.
     * If the resource does not support that facet, an exception is thrown.
     * The resource must be in the STARTED (i.e. connected) state.
     *
     * @param  resourceId identifies the resource that is to perform the bundle activities
     * @param  timeout    if any facet method invocation thread has not completed after this many milliseconds, interrupt
     *                    it; value must be positive
     *
     * @return the resource's bundle facet interface
     *
     * @throws PluginContainerException on error
     */
    private BundleFacet getBundleFacet(int resourceId, long timeout) throws PluginContainerException {
        return ComponentUtil.getComponent(resourceId, BundleFacet.class, FacetLockType.READ, timeout, false, true);
    }
}
