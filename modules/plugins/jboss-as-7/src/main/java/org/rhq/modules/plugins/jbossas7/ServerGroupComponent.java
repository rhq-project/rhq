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
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.content.PackageType;
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

/**
 * Component dealing with server group specific things
 * @author Heiko W. Rupp
 */
public class ServerGroupComponent extends DomainComponent implements ContentFacet, CreateChildResourceFacet {

    private final Log log = LogFactory.getLog(ServerGroupComponent.class);

    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages,
                                                 ContentServices contentServices) {

        ContentContext cctx = context.getContentContext();

        String uploadUrl = "http://localhost:9990/domain-api/add-content";

        for (ResourcePackageDetails details : packages) {
            ASUploadConnection uploadConnection = new ASUploadConnection();
            OutputStream out = uploadConnection.getOutputStream(details.getFileName());
            contentServices.downloadPackageBits(cctx, details.getKey(), out, false);
            JsonNode result = uploadConnection.finishUpload();
        }


        return null;  // TODO: Customise this generated block
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        return null;  // TODO: Customise this generated block
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

        ASUploadConnection connection = new ASUploadConnection();
        OutputStream out = connection.getOutputStream(details.getFileName());
//        contentServices.downloadPackageBits(cctx,details.getKey(),out,false);
        contentServices.downloadPackageBitsForChildResource(cctx, resourceTypeName, details.getKey(), out);

        JsonNode result = connection.finishUpload();
        System.out.println(result);


        report.setStatus(CreateResourceStatus.SUCCESS)       ;

        return report;

    }
}
