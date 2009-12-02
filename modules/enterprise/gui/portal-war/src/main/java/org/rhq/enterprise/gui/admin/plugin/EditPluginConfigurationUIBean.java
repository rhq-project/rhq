/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import java.io.Serializable;
import javax.faces.application.FacesMessage;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Scope(ScopeType.PAGE)
@Name("editPluginConfigurationUIBean")
public class EditPluginConfigurationUIBean extends AbstractPluginConfigurationUIBean implements Serializable {

    @Logger
    private Log log;

    @Create
    public void init() {
        checkPermission();
        lookupConfigDefinitions();
    }

    // The superclass holds the plugin instance variable, but in this
    // case we want it injected via the PluginFactory - so this serves as
    // the injection point
    @In(value="plugin", required=false)
    public void setCurrentPlugin(ServerPlugin currentPlugin) {
        if (currentPlugin != null) {
            setPlugin(currentPlugin);
        }
    }

    public void updatePlugin() {
        try {
            ServerPluginsLocal serverPlugins = LookupUtil.getServerPlugins();
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            serverPlugins.updateServerPluginExceptContent(subject, getPlugin());
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Configuration settings saved.");
        } catch (Exception e) {
            log.error("Error updating the plugin configurations.", e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "There was an error changing the configuration settings.", e);
        }
    }
}
