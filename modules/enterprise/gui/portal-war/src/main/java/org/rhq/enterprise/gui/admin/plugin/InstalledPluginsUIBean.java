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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.event.UploadEvent;

import org.jboss.deployment.scanner.URLDeploymentScannerMBean;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;
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
            String err = "Failed to scan for updated agent plugins";
            log.error(err + " - Cause: " + e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, err, e);
        }
    }

    public void fileUploadListener(UploadEvent event) {
        try {
            File uploadedPlugin = event.getUploadItem().getFile();
            String newPluginFilename = event.getUploadItem().getFileName();
            log.info("A new plugin [" + newPluginFilename + "] has been uploaded to [" + uploadedPlugin + "]");

            if (uploadedPlugin == null || !uploadedPlugin.exists()) {
                throw new FileNotFoundException("The uploaded plugin file [" + uploadedPlugin + "] does not exist!");
            }

            // put the new plugin file in our agent plugin location
            ServerCommunicationsServiceMBean sc = ServerCommunicationsServiceUtil.getService();
            File dir = new File(sc.getConfiguration().getAgentFilesDirectory(), "rhq-plugins");
            File agentPlugin = new File(dir, newPluginFilename);
            FileOutputStream fos = new FileOutputStream(agentPlugin);
            FileInputStream fis = new FileInputStream(uploadedPlugin);
            StreamUtil.copy(fis, fos);
            log.info("A new plugin has been deployed [" + agentPlugin + "]. Will now perform a scan to register it...");

            scan();

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "New agent plugin processed: " + agentPlugin);
        } catch (Exception e) {
            String err = "Failed to process uploaded agent plugin";
            log.error(err + " - Cause: " + e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, err, e);
        }

        return;
    }
}