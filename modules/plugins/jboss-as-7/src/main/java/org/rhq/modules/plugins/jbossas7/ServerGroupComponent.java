/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;

/**
 * Component dealing with server group specific things
 * @author Heiko W. Rupp
 */
public class ServerGroupComponent extends DomainComponent implements ContentFacet, CreateChildResourceFacet {

    private static final String SUCCESS = "success";
    private static final String OUTCOME = "outcome";
    private final Log log = LogFactory.getLog(ServerGroupComponent.class);

    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages,
                                                 ContentServices contentServices) {

        ContentContext cctx = context.getContentContext();

        DeployPackagesResponse response = new DeployPackagesResponse();

        for (ResourcePackageDetails details : packages) {

            ASUploadConnection uploadConnection = new ASUploadConnection();
            String fileName = details.getFileName();
            OutputStream out = uploadConnection.getOutputStream(fileName);
            contentServices.downloadPackageBits(cctx, details.getKey(), out, false);
            JsonNode uploadResult = uploadConnection.finishUpload();
            if (uploadResult.has(OUTCOME)) {
                String outcome = uploadResult.get(OUTCOME).getTextValue();
                if (outcome.equals(SUCCESS)) { // Upload was successful, so now add the file to the server group
                    JsonNode resultNode = uploadResult.get("result");
                    String hash = resultNode.get("BYTES_VALUE").getTextValue();
                    ASConnection connection = getASConnection();

                    List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
                    deploymentsAddress.add(new PROPERTY_VALUE("deployment", fileName));
                    Operation step1 = new Operation("add",deploymentsAddress);
                    step1.addAdditionalProperty("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
                    step1.addAdditionalProperty("name", fileName);

                    List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>(1);
                    serverGroupAddress.add(new PROPERTY_VALUE("server-group",serverGroupFromKey()));
                    serverGroupAddress.add(new PROPERTY_VALUE("deployment", fileName));
                    Operation step2 = new Operation("add",serverGroupAddress,"enabled","true");

                    CompositeOperation cop = new CompositeOperation();
                    cop.addStep(step1);
                    cop.addStep(step2);

                    JsonNode result = connection.execute(cop);
                    if (ASConnection.isErrorReply(result)) // TODO get failure message into response
                        response.addPackageResponse(new DeployIndividualPackageResponse(details.getKey(),ContentResponseResult.FAILURE));
                    else
                        response.addPackageResponse(new DeployIndividualPackageResponse(details.getKey(),ContentResponseResult.SUCCESS));
                }
                else
                    response.addPackageResponse(new DeployIndividualPackageResponse(details.getKey(),ContentResponseResult.FAILURE));
            }
            else {
                response.addPackageResponse(
                        new DeployIndividualPackageResponse(details.getKey(), ContentResponseResult.FAILURE));
            }
        }


        return response;
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {

        List<PROPERTY_VALUE> serverGroupAddress = pathToAddress(path);

        Operation op = new ReadChildrenNames(serverGroupAddress,"deployment"); // TODO read full packages not onyl names
        JsonNode node = connection.execute(op);
        if (ASConnection.isErrorReply(node))
            return null;

        JsonNode result = node.get("result");
        Iterator<JsonNode> iter = result.getElements();
        Set<ResourcePackageDetails> details = new HashSet<ResourcePackageDetails>();
        while (iter.hasNext()) {
            JsonNode jNode = iter.next();
            String file = jNode.getTextValue();
            String t;
            if (file.contains("."))
                t = file.substring(file.lastIndexOf(".")+1);
            else
                t = "-none-";

            ResourcePackageDetails detail = new ResourcePackageDetails(new PackageDetailsKey(file,"1.0",t,"all"));
            details.add(detail);
        }
        return details;

    }

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {


        ResourcePackageDetails details = report.getPackageDetails();

        ContentContext cctx = context.getContentContext();
        ContentServices contentServices = cctx.getContentServices();
        String resourceTypeName = report.getResourceType().getName();

        ASUploadConnection uploadConnection = new ASUploadConnection();
        OutputStream out = uploadConnection.getOutputStream(details.getFileName());
//        contentServices.downloadPackageBits(cctx,details.getKey(),out,false);
        contentServices.downloadPackageBitsForChildResource(cctx, resourceTypeName, details.getKey(), out);

        JsonNode uploadResult = uploadConnection.finishUpload();
        System.out.println(uploadResult);
        if (ASConnection.isErrorReply(uploadResult)) {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage(ASConnection.getFailureDescription(uploadResult));

            return report;
        }

        String fileName = report.getUserSpecifiedResourceName();

        JsonNode resultNode = uploadResult.get("result");
        String hash = resultNode.get("BYTES_VALUE").getTextValue();
        ASConnection connection = getASConnection();

        List<PROPERTY_VALUE> deploymentsAddress = new ArrayList<PROPERTY_VALUE>(1);
        deploymentsAddress.add(new PROPERTY_VALUE("deployment", fileName));
        Operation step1 = new Operation("add",deploymentsAddress);
        step1.addAdditionalProperty("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        step1.addAdditionalProperty("name", fileName);  // TODO set a random name here - or wait on AS to "fix" this
        step1.addAdditionalProperty("runtime-name", fileName);

        List<PROPERTY_VALUE> serverGroupAddress = new ArrayList<PROPERTY_VALUE>(1);
        serverGroupAddress.add(new PROPERTY_VALUE("server-group",serverGroupFromKey()));
        serverGroupAddress.add(new PROPERTY_VALUE("deployment", fileName));
        Operation step2 = new Operation("add",serverGroupAddress,"enabled","true");

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(step1);
        cop.addStep(step2);

        JsonNode result = connection.execute(cop);
        if (ASConnection.isErrorReply(result)) {
            report.setErrorMessage(ASConnection.getFailureDescription(resultNode));
            report.setStatus(CreateResourceStatus.FAILURE);
        }
        else {
            report.setStatus(CreateResourceStatus.SUCCESS);
        }

        return report;

    }


    private String serverGroupFromKey() {
        String key1 = context.getResourceKey();
        return key1.substring(key1.lastIndexOf("/")+1);
    }
}
