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
package org.rhq.enterprise.gui.operation.definition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class OperationDefinitionParametersUIBean {
    public static final String MANAGED_BEAN_NAME = "OperationDefinitionParametersUIBean";

    private Configuration configuration;
    private ConfigurationDefinition configurationDefinition;

    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    public OperationDefinitionParametersUIBean() {
        this.configurationDefinition = lookupConfigurationDefinition();
        this.configuration = lookupConfiguration();
    }

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        return this.configurationDefinition;
    }

    public void setConfigurationDefinition(@NotNull
    ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    @Nullable
    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(@NotNull
    Configuration configuration) {
        this.configuration = configuration;
    }

    protected Configuration lookupConfiguration() {
        try {
            ConfigurationDefinition definition = lookupConfigurationDefinition();

            // call a SLSB method to get around lazy initialization of configDefs and configTemplates
            Configuration configuration = configurationManager.getConfigurationFromDefaultTemplate(definition);
            Configuration newConfiguration = configuration.deepCopy(false);

            return newConfiguration;
        } catch (Exception e) {
            return null;
        }
    }

    protected ConfigurationDefinition lookupConfigurationDefinition() {
        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int operationId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("opId"));

            OperationDefinition operationDefinition = operationManager.getOperationDefinition(subject, operationId);
            ConfigurationDefinition definition = operationDefinition.getParametersConfigurationDefinition();

            return definition;
        } catch (Exception e) {
            return null;
        }
    }

    public String getNullConfigurationDefinitionMessage() {
        return "This operation does not take any parameters.";
    }

    public String getNullConfigurationMessage() {
        return "This operation parameters definition has not been initialized.";
    }
}