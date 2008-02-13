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
package org.rhq.enterprise.gui.configuration.resource;

import java.util.Hashtable;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is a JSF managed bean for the resource Configuration view and edit pages. The contents of the page are
 * dynamically generated based on the ConfigurationDefinition (metadata) and current Configuration (data) for a
 * specified resource.
 *
 * <p/>REQUEST PARAMETERS:
 * <li>id - a {@link Resource} id</li>
 * <li>listName - the name of a list property within the Configuration (only required if the mapPanel property is being
 *   accessed)</li>
 * <li>listIndex - the index of a map property within the above list property (only required if the mapPanel property is
 *   being accessed)</li>
 *
 * @author Ian Springer
 */
public abstract class AbstractResourceConfigurationUIBean {
    protected static final String SUCCESS_OUTCOME = "success";
    protected static final String FAILURE_OUTCOME = "failure";

    protected ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    private Map<Integer, ConfigurationDefinition> configurationDefinitionMap; // maps subclass-specified Integer keys to resource ConfigurationDefinitions
    private Map<Integer, Configuration> configurationMap; // maps subclass-specified Integer keys to resource Configurations

    protected AbstractResourceConfigurationUIBean() {
        this.configurationDefinitionMap = new Hashtable<Integer, ConfigurationDefinition>();
        this.configurationMap = new Hashtable<Integer, Configuration>();
    }

    @Nullable
    protected abstract ConfigurationDefinition lookupConfigurationDefinition();

    protected abstract int getConfigurationDefinitionKey();

    public void setConfigurationDefinition(@NotNull
    ConfigurationDefinition configurationDefinition) {
        this.configurationDefinitionMap.put(getConfigurationDefinitionKey(), configurationDefinition);
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition configurationDefinition = this.configurationDefinitionMap
            .get(getConfigurationDefinitionKey());
        if (configurationDefinition == null) {
            configurationDefinition = lookupConfigurationDefinition();
            if (configurationDefinition != null) {
                this.configurationDefinitionMap.put(getConfigurationDefinitionKey(), configurationDefinition);
            }
        }

        return configurationDefinition;
    }

    @Nullable
    protected abstract Configuration lookupConfiguration();

    protected abstract int getConfigurationKey();

    public void setConfiguration(@NotNull
    Configuration configuration) {
        this.configurationMap.put(getConfigurationKey(), configuration);
    }

    public Configuration getConfiguration() {
        Configuration configuration = this.configurationMap.get(getConfigurationKey());
        if (configuration == null) {
            configuration = lookupConfiguration();
            if (configuration != null) {
                this.configurationMap.put(getConfigurationKey(), configuration);
            }
        }

        return configuration;
    }

    protected void clearConfiguration() {
        this.configurationMap.remove(getConfigurationKey());
    }

    public boolean isUpdateInProgress() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        return this.configurationManager.isResourceConfigurationUpdateInProgress(subject, resourceId);
    }

    public String getNullConfigurationDefinitionMessage() {
        return "This resource does not expose a configuration.";
    }

    public String getNullConfigurationMessage() {
        return "This resource's configuration has not yet been initialized.";
    }
}