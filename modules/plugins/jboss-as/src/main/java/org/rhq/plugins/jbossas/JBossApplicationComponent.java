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
package org.rhq.plugins.jbossas;

import java.util.Set;
import java.io.File;

import org.rhq.plugins.applications.ApplicationResourceComponent;
import org.rhq.plugins.jbossas.helper.MainDeployer;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * A ResourceComponent that extends the generic ApplicationResourceComponent from the Tomcat plugin so that when an EAR
 * or WAR file is successfully written to the filesystem, it will be immediately deployed by calling redeploy() on the
 * MainDeployer MBean.
 *
 * @author Ian Springer
 */
public class JBossApplicationComponent extends ApplicationResourceComponent<JBossASServerComponent> {
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        DeployPackagesResponse response = super.deployPackages(packages, contentServices);
        if (response.getOverallRequestResult() == ContentResponseResult.SUCCESS) {
            Configuration pluginConfig = getResourceContext().getPluginConfiguration();
            File path = new File(pluginConfig.getSimple(FILENAME_PLUGIN_CONFIG_PROP).getStringValue());
            MainDeployer mainDeployer = getParentResourceComponent().getMainDeployer();
            try {
                mainDeployer.redeploy(path);
            }
            catch (Exception e) {
                ResourcePackageDetails packageDetails = packages.iterator().next();
                return failApplicationDeployment(ThrowableUtil.getAllMessages(e), packageDetails);
            }
        }
        return response;
    }
}
