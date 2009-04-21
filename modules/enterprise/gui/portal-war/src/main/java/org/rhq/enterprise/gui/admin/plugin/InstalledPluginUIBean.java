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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class InstalledPluginUIBean {
    private final Log log = LogFactory.getLog(InstalledPluginUIBean.class);

    public static final String MANAGED_BEAN_NAME = "InstalledPluginUIBean";

    private Plugin plugin;

    private ResourceMetadataManagerLocal resourceMetadataManagerBean = LookupUtil.getResourceMetadataManager();

    public InstalledPluginUIBean() {
        this.plugin = lookupPlugin();
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    private Plugin lookupPlugin() {
        hasPermission();
        String pluginName = FacesContextUtility.getRequiredRequestParameter("plugin", String.class);
        return resourceMetadataManagerBean.getPlugin(pluginName);
    }

    public void setEnabled(boolean enabled) {
        // TODO is this supposed to be empty?
    }

    public void undeploy() {
    }

    /**
     * Throws a permission exception if the user is not allowed to access this functionality. 
     */
    private void hasPermission() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (!LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_SETTINGS)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have the proper permissions to view or manage plugins");
        }
    }
}