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
import javax.faces.model.SelectItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ControlDefinition;

@Scope(ScopeType.PAGE)
@Name("ServerPluginControlUIBean")
public class ServerPluginControlUIBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ServerPluginsLocal serverPluginsBean = LookupUtil.getServerPlugins();

    @RequestParameter
    private String plugin;
    @RequestParameter
    private String pluginType;

    private PluginKey serverPluginKey;
    private ServerPlugin serverPlugin;
    private List<ControlDefinition> serverPluginControlDefinitions;
    private String selectedControl;

    private Configuration params;
    private ControlResults results;

    @Create
    public void init() throws Exception {
        this.serverPluginKey = PluginKey.createServerPluginKey(this.pluginType, this.plugin);
        this.serverPlugin = this.serverPluginsBean.getServerPlugin(this.serverPluginKey);

        ArrayList<ControlDefinition> defs = new ArrayList<ControlDefinition>();
        if (checkPermission()) {
            defs.addAll(this.serverPluginsBean.getServerPluginControlDefinitions(this.serverPluginKey));
        }
        this.serverPluginControlDefinitions = defs;

        setSelectedControl(null);
    }

    public PluginKey getServerPluginKey() {
        return this.serverPluginKey;
    }

    public ServerPlugin getServerPlugin() {
        return this.serverPlugin;
    }

    public String getSelectedControl() {
        return this.selectedControl;
    }

    public void setSelectedControl(String controlName) {
        if (controlName != null) {
            this.selectedControl = controlName;
            this.params = (getParamsDefinition() != null) ? new Configuration() : null;
            this.results = null;
        } else {
            this.selectedControl = null;
            this.params = null;
            this.results = null;
        }
    }

    public List<SelectItem> getControlOptions() throws Exception {
        List<SelectItem> items = new ArrayList<SelectItem>();

        if (this.serverPluginControlDefinitions != null) {
            for (ControlDefinition def : this.serverPluginControlDefinitions) {
                items.add(new SelectItem(def.getName(), def.getDisplayName(), def.getDescription()));
            }
        }

        return items;
    }

    public Configuration getParamsConfiguration() {
        return this.params;
    }

    public ConfigurationDefinition getParamsDefinition() {
        // TODO: just mock somethign to test, remove me later
        ConfigurationDefinition def1 = new ConfigurationDefinition("test", "foo");
        PropertyDefinition prop = new PropertyDefinitionSimple("simple-pr", "description", false,
            PropertySimpleType.STRING);
        def1.put(prop);
        if (1 == 1)
            return def1;

        if (this.serverPluginControlDefinitions != null) {
            for (ControlDefinition def : this.serverPluginControlDefinitions) {
                if (def.getName().equals(this.selectedControl)) {
                    ConfigurationDefinition paramsDef = def.getParameters();
                    return paramsDef;
                }
            }
        }

        return null; // return null to indicate that there are no params defined
    }

    public boolean isResultsAvailable() {
        return this.results != null;
    }

    public Configuration getResultsConfiguration() {
        return this.results != null ? this.results.getComplexResults() : null;
    }

    public String getResultsError() {
        return this.results != null ? this.results.getError() : null;
    }

    public void invokeControl() {
        try {
            this.results = this.serverPluginsBean.invokeServerPluginControl(this.serverPluginKey, this.selectedControl,
                getParamsConfiguration());
            if (this.results.isSuccess()) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                    "Plugin invoked the control operation successfully");
            } else {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Plugin reported an error: "
                    + this.results.getError());
            }
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to invoke the plugin control", e);
        }
    }

    public void selectControl() {
        //TODO should not be neeed
        System.out.println("here");
    }

    private boolean checkPermission() throws Exception {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (!LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_SETTINGS)) {
            return false;
        }
        return true;
    }
}
