/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.modules.plugins.jbossas7.ASConnection.verbose;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.content.FileContentDelegate;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.Base64;
import org.rhq.core.util.ByteUtil;
import org.rhq.core.util.file.ContentFileInfo;
import org.rhq.core.util.file.JarContentFileInfo;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Deal with deployments
 * @author Heiko W. Rupp
 */
public class DeploymentComponent extends BaseComponent<ResourceComponent<?>> implements OperationFacet, ContentFacet {


    private static final String DOMAIN_DATA_CONTENT_SUBDIR = "/data/content";
    private File deploymentFile;

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException,
        Exception {
        super.start(context);
        deploymentFile = determineDeploymentFile();
    }

    @Override
    public AvailabilityType getAvailability() {
        Operation op = new ReadAttribute(getAddress(), "enabled");
        Result res = getASConnection().execute(op);
        if (!res.isSuccess())
            return AvailabilityType.DOWN;

        if (res.getResult() == null || !(Boolean) (res.getResult()))
            return AvailabilityType.DOWN;

        return AvailabilityType.UP;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        if (name.equals("enable")) {
            return invokeSimpleOperation("deploy");
        } else if (name.equals("disable")) {
            return invokeSimpleOperation("undeploy");
        } else if (name.equals("restart")) {
            OperationResult result = invokeSimpleOperation("undeploy");

            if (result.getErrorMessage() == null) {
                result = invokeSimpleOperation("deploy");
            }

            return result;
        } else {
            return super.invokeOperation(name, parameters);
        }
    }

    private OperationResult invokeSimpleOperation(String action) {
        Operation op = new Operation(action, getAddress());
        Result res = getASConnection().execute(op);
        OperationResult result = new OperationResult();
        if (res.isSuccess()) {
            result.setSimpleResult("Success");
            if ("enable".equals(action)) {
                context.getAvailabilityContext().enable();
            }
            if ("disable".equals(action)) {
                context.getAvailabilityContext().disable();
            }
        } else {
            result.setErrorMessage(res.getFailureDescription());
        }

        return result;
    }

    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return new ArrayList<DeployPackageStep>();
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        getLog().debug("Starting deployment..");
        DeployPackagesResponse response = new DeployPackagesResponse();

        if (packages.size() != 1) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("Can only deploy one package at a time");
            getLog().warn("deployPackages can only deploy one package at a time");
        }

        ResourcePackageDetails detail = packages.iterator().next();

        ASUploadConnection uploadConnection = ASUploadConnection.newInstanceForServerPluginConfiguration(
            getServerComponent().getServerPluginConfiguration(), detail.getKey().getName());
        OutputStream out = uploadConnection.getOutputStream();
        if (out == null) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response
                .setOverallRequestErrorMessage("An error occured while the agent was preparing for content download");
            return response;
        }
        ResourceType resourceType = context.getResourceType();

        getLog().info("Deploying " + resourceType.getName() + " Resource with key [" + detail.getKey() + "]...");

        try {
            contentServices.downloadPackageBits(context.getContentContext(), detail.getKey(), out, true);
        } catch (Exception e) {
            uploadConnection.cancelUpload();
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("An error occured while the agent was downloading the content");
            return response;
        }

        JsonNode uploadResult = uploadConnection.finishUpload();
        if (verbose) {
            getLog().info(uploadResult);
        }

        if (ASUploadConnection.isErrorReply(uploadResult)) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage(ASUploadConnection.getFailureDescription(uploadResult));
            return response;
        }

        JsonNode resultNode = uploadResult.get("result");
        String hash = resultNode.get("BYTES_VALUE").getTextValue();

        try {
            redeployOnServer(detail.getKey().getName(), hash);
            response.setOverallRequestResult(ContentResponseResult.SUCCESS);
            //we just deployed a different file on the AS7 server, so let's refresh ourselves
            deploymentFile = determineDeploymentFile();
            DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(detail.getKey(),
                ContentResponseResult.SUCCESS);
            response.addPackageResponse(packageResponse);

        } catch (Exception e) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
        }

        getLog().info("Result of deployment of " + resourceType.getName() + " Resource with key [" + detail.getKey() + "]: "
            + response);

        return response;
    }

    private void redeployOnServer(String name, String hash) throws Exception {

        Operation op = new Operation("full-replace-deployment", new Address());
        op.addAdditionalProperty("name", name);
        List<Object> content = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        content.add(contentValues);
        op.addAdditionalProperty("content", content);
        Result result = getASConnection().execute(op);
        if (result.isRolledBack())
            throw new Exception(result.getFailureDescription());
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        RemovePackagesResponse response = new RemovePackagesResponse(ContentResponseResult.NOT_PERFORMED);
        response.setOverallRequestErrorMessage("Removal of packages backing the deployments is not supported.");
        return response;
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        if (deploymentFile == null) {
            return Collections.emptySet();
        }

        String name = getDeploymentName();
        String sha256 = getSHA256(deploymentFile);
        String version = getVersion(sha256);

        PackageDetailsKey key = new PackageDetailsKey(name, version, type.getName(), "noarch");
        ResourcePackageDetails details = new ResourcePackageDetails(key);

        details.setDisplayVersion(getDisplayVersion(deploymentFile));
        details.setFileCreatedDate(null); //TODO figure this out from Sigar somehow?
        details.setFileName(name);
        details.setFileSize(deploymentFile.length());
        details.setInstallationTimestamp(deploymentFile.lastModified());
        details.setLocation(deploymentFile.getAbsolutePath());
        details.setSHA256(sha256);

        return Collections.singleton(details);
    }

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        try {
            return deploymentFile == null ? new ByteArrayInputStream(new byte[0]) : new FileInputStream(deploymentFile);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Deployment file seems to have disappeared");
        }
    }

    /**
     * Determine the location of the physical content of a deployment.
     * We need to check several cases here:
     * <ul>
     *     <li>Standalone server</li>
     *     <li>Domain Deployment: here the content is under /deployment=xxx and in the filesystem at $AS/domain/data/content</li>
     *     <li>Server-group deployment: there is no real physical content, it is a logical link to a domain deployment</li>
     *     <li>Manages server: here the content exists below /host=xx/server=server-name/deployment=xxx</li>
     * </ul>
     * @return A file object pointing to the deployed file or null if there is no content
     */
    private File determineDeploymentFile() {
        Operation op = new ReadAttribute(getAddress(), "content");
        Result result = getASConnection().execute(op);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.getResult();
        if (content == null || content.isEmpty()) {
            // No content -> check for server group
            if (path.startsWith(("server-group="))) {
                // Server group has no content of its own - use the domain deployment
                String name = (String) path.substring(path.lastIndexOf("=") + 1);
                op = new ReadResource(new Address("deployment", name));
                result = getASConnection().execute(op);
                if (result.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentMap = (Map<String, Object>) result.getResult();
                    content = (List<Map<String, Object>>) contentMap.get("content");
                    if (content.get(0).containsKey("path")) {
                        String path = (String) content.get(0).get("path");
                        String relativeTo = (String) content.get(0).get("relative-to");
                        deploymentFile = getDeploymentFileFromPath(relativeTo, path);
                    } else if (content.get(0).containsKey("hash")) {
                        @SuppressWarnings("unchecked")
                        String base64Hash = ((Map<String, String>) content.get(0).get("hash")).get("BYTES_VALUE");
                        byte[] hash = Base64.decode(base64Hash);
                        ServerGroupComponent sgc = (ServerGroupComponent) context.getParentResourceComponent();
                        String baseDir = ((HostControllerComponent) sgc.context.getParentResourceComponent()).pluginConfiguration
                            .getSimpleValue("baseDir");
                        String contentPath = new File(baseDir, "/data/content").getAbsolutePath();
                        deploymentFile = getDeploymentFileFromHash(hash, contentPath);
                    }
                    return deploymentFile;
                }
            } else {
                getLog().warn("Could not determine the location of the deployment - the content descriptor wasn't found for deployment"
                    + getAddress() + ".");
                return null;
            }
        }

        Boolean archive = (Boolean) content.get(0).get("archive");
        if (archive != null && !archive) {
            getLog().debug("Exploded deployments not supported for retrieving the content.");
            return null;
        }

        File deploymentFile = null;
        if (content.get(0).containsKey("path")) {
            String path = (String) content.get(0).get("path");
            String relativeTo = (String) content.get(0).get("relative-to");
            deploymentFile = getDeploymentFileFromPath(relativeTo, path);
        } else if (content.get(0).containsKey("hash")) {
            @SuppressWarnings("unchecked")
            String base64Hash = ((Map<String, String>) content.get(0).get("hash")).get("BYTES_VALUE");
            byte[] hash = Base64.decode(base64Hash);
            Address contentPathAddress;
            if (context.getParentResourceComponent() instanceof ManagedASComponent) {
                // -> managed server we need to check for host=x/server=y, but the path brings host=x,server-config=y
                String p = ((ManagedASComponent) context.getParentResourceComponent()).getPath();
                p = p.replaceAll("server-config=", "server=");
                contentPathAddress = new Address(p);
                contentPathAddress.add("core-service", "server-environment");
            } else {
                // standalone
                contentPathAddress = new Address("core-service", "server-environment");
            }
            op = new ReadAttribute(contentPathAddress, "content-dir");
            result = getASConnection().execute(op);

            String contentPath;
            if (result.isSuccess()) {
                contentPath = (String) result.getResult();
            } else {
                // No success above -> check if this is a domain deployment
                if (this instanceof DomainDeploymentComponent) {
                    String baseDir = ((HostControllerComponent) context.getParentResourceComponent()).pluginConfiguration
                        .getSimpleValue("baseDir");
                    contentPath = new File(baseDir, DOMAIN_DATA_CONTENT_SUBDIR).getAbsolutePath();
                } else {
                    contentPath = "-unknown-";
                }
            }
            deploymentFile = getDeploymentFileFromHash(hash, contentPath);
        } else {
            getLog().warn("Failed to determine the deployment file of " + getAddress()
                + " deployment. Neither path nor hash attributes were available.");
        }

        return deploymentFile;
    }

    private File getDeploymentFileFromPath(String relativeTo, String path) {
        if (relativeTo == null || relativeTo.trim().isEmpty()) {
            return new File(path);
        } else {
            //Transform the property name into the name used in the server environment
            if (relativeTo.startsWith("jboss.server")) {
                relativeTo = relativeTo.substring("jboss.server.".length());
                relativeTo = relativeTo.replace('.', '-');

                //now look for the transformed relativeTo in the server environment
                Operation op = new ReadAttribute(new Address("core-service", "server-environment"), relativeTo);
                Result res = getASConnection().execute(op);

                relativeTo = (String) res.getResult();

                return new File(relativeTo, path);
            } else {
                getLog().warn("Unsupported property used as a base for deployment path specification: " + relativeTo);
                return null;
            }
        }
    }

    private File getDeploymentFileFromHash(byte[] hash, String contentPath) {
        String hashStr = ByteUtil.toHexString(hash);

        String head = hashStr.substring(0, 2);
        String tail = hashStr.substring(2);

        File hashPath = new File(new File(head, tail), "content");

        return new File(contentPath, hashPath.getPath());
    }

    /**
     * Retrieve SHA256 for a deployed app.
     *
     * Shamelessly copied from the AS5 plugin.
     *
     * @param file application file
     * @return SHA256 of the content
     */
    private String getSHA256(File file) {
        String sha256 = null;

        try {
            FileContentDelegate fileContentDelegate = new FileContentDelegate();
            sha256 = fileContentDelegate.retrieveDeploymentSHA(file, context.getResourceDataDirectory());
        } catch (Exception iex) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Problem calculating digest of package [" + file.getPath() + "]." + iex.getMessage());
            }
        }

        return sha256;
    }

    /**
     * Shamelessly copied from the AS5 plugin.
     *
     * @param sha256
     * @return
     */
    private static String getVersion(String sha256) {
        return "[sha256=" + sha256 + "]";
    }

    /**
     * Retrieve the display version for the component. The display version should be stored
     * in the manifest of the application (implementation and/or specification version).
     * It will attempt to retrieve the version for both archived or exploded deployments.
     *
     * Shamelessly copied from the AS5 plugin
     *
     * @param file component file
     * @return
     */
    private String getDisplayVersion(File file) {
        //JarContentFileInfo extracts the version from archived and exploded deployments
        ContentFileInfo contentFileInfo = new JarContentFileInfo(file);
        return contentFileInfo.getVersion(null);
    }

    private String getDeploymentName() {
        Operation op = new ReadAttribute(getAddress(), "name");
        Result res = getASConnection().execute(op);

        return (String) res.getResult();
    }
}
