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
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.AbstractPlugin;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.plugin.ServerPluginManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ControlDefinition;

//@Scope(ScopeType.CONVERSATION)
//@Name("ServerPluginControlUIBean")
public class ServerPluginControlUIBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ServerPluginManagerLocal serverPluginsBean = LookupUtil.getServerPluginManager();

    //@In("plugin")
    private AbstractPlugin abstractPlugin;

    //@RequestParameter
    private String control;

    private ServerPlugin serverPlugin;
    private String serverPluginType;
    private String serverPluginName;
    private PluginKey serverPluginKey;
    private List<ControlDefinition> serverPluginControlDefinitions;

    private Configuration params;
    private ControlResults results;

    //@Create
    public void init() throws Exception {
        this.serverPlugin = (ServerPlugin) this.abstractPlugin;

        this.serverPluginType = this.serverPlugin.getType();
        this.serverPluginName = this.serverPlugin.getName();
        this.serverPluginKey = PluginKey.createServerPluginKey(this.serverPluginType, this.serverPluginName);

        ArrayList<ControlDefinition> defs = new ArrayList<ControlDefinition>();
        if (getPermission()) {
            defs.addAll(this.serverPluginsBean.getServerPluginControlDefinitions(this.serverPluginKey));
        }
        this.serverPluginControlDefinitions = defs;

        setSelectedControl(this.control);
    }

    public PluginKey getServerPluginKey() {
        return this.serverPluginKey;
    }

    public ServerPlugin getServerPlugin() {
        return this.serverPlugin;
    }

    public void setSelectedControl(String controlName) {
        if (controlName != null) {
            this.control = controlName;
            if (getParamsDefinition() != null) {
                this.params = getParamsDefinition().getDefaultTemplate().createConfiguration();
            } else {
                this.params = null;
            }
        } else {
            this.control = null;
            this.params = null;
        }
        this.results = null;
    }

    public List<String[]> getControls() throws Exception {
        if (this.serverPluginControlDefinitions == null) {
            return null;
        }

        List<String[]> items = new ArrayList<String[]>();
        for (ControlDefinition def : this.serverPluginControlDefinitions) {
            items.add(new String[] { def.getName(), def.getDisplayName(), def.getDescription() });
        }
        return items;
    }

    public Configuration getParamsConfiguration() {
        return this.params;
    }

    public ConfigurationDefinition getParamsDefinition() {
        if (this.serverPluginControlDefinitions != null) {
            for (ControlDefinition def : this.serverPluginControlDefinitions) {
                if (def.getName().equals(this.control)) {
                    ConfigurationDefinition paramsDef = def.getParameters();
                    return paramsDef;
                }
            }
        }

        return null; // return null to indicate that there are no params defined
    }

    public boolean getResultsAvailable() {
        return this.results != null;
    }

    public ConfigurationDefinition getResultsDefinition() {
        if (this.serverPluginControlDefinitions != null) {
            for (ControlDefinition def : this.serverPluginControlDefinitions) {
                if (def.getName().equals(this.control)) {
                    ConfigurationDefinition resultsDef = def.getResults();
                    return resultsDef;
                }
            }
        }

        return null;
    }

    public Configuration getResultsConfiguration() {
        return this.results != null ? this.results.getComplexResults() : null;
    }

    public String getResultsError() {
        return this.results != null ? this.results.getError() : null;
    }

    public String invokeControl() {
        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            this.results = this.serverPluginsBean.invokeServerPluginControl(subject, this.serverPluginKey,
                this.control, getParamsConfiguration());
            if (this.results.isSuccess()) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                    "Plugin invoked the control operation successfully");
            } else {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Plugin reported an error: " + this.results.getError());
            }
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to invoke the plugin control", e);
        }

        return "success";
    }

    public boolean getPermission() throws Exception {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (!LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_SETTINGS)) {
            return false;
        }
        return true;
    }
}
