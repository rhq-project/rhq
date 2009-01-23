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
package org.rhq.enterprise.gui.admin.plugin;

import java.util.Collection;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployment.scanner.URLDeploymentScannerMBean;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 */
public class InstalledPluginsUIBean {
    private final Log log = LogFactory.getLog(InstalledPluginsUIBean.class);

    public static final String MANAGED_BEAN_NAME = "InstalledPluginsUIBean";

    private ResourceMetadataManagerLocal resourceMetadataManagerBean = LookupUtil.getResourceMetadataManager();

    public InstalledPluginsUIBean() {
    }

    public Collection<Plugin> getInstalledPlugins() {
        return resourceMetadataManagerBean.getPlugins();
    }

    public void scan() {
        try {
            URLDeploymentScannerMBean scanner = LookupUtil.getAgentPluginURLDeploymentScanner();
            scanner.scan();
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Done scanning for updated agent plugins.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to scan for updated agent plugins", e);
        }
    }

    public void uploadPlugin() {
    }

}