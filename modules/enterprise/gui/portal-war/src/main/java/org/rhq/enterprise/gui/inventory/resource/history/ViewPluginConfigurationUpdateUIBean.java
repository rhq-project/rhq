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
package org.rhq.enterprise.gui.inventory.resource.history;

import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.configuration.resource.ExistingResourceConfigurationUIBean;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * @author Joseph Marques
 */
public class ViewPluginConfigurationUpdateUIBean extends ExistingResourceConfigurationUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewPluginConfigurationUpdateUIBean";

    @Override
    protected Configuration lookupConfiguration() {
        Integer configId = FacesContextUtility.getOptionalRequestParameter(ParamConstants.CONFIG_ID_PARAM,
            Integer.class);
        if (configId == null) {
            return super.lookupConfiguration();
        }

        AbstractResourceConfigurationUpdate configurationUpdate = this.configurationManager
            .getPluginConfigurationUpdate(EnterpriseFacesContextUtility.getSubject(), configId);
        Configuration configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;

        return configuration;
    }

    @Override
    protected ConfigurationDefinition lookupConfigurationDefinition() {
        int resourceTypeId = EnterpriseFacesContextUtility.getResource().getResourceType().getId();
        ConfigurationDefinition configurationDefinition = this.configurationManager
            .getPluginConfigurationDefinitionForResourceType(EnterpriseFacesContextUtility.getSubject(), resourceTypeId);
        return configurationDefinition;
    }

    protected int getConfigurationKey() {
        return FacesContextUtility.getOptionalRequestParameter(ParamConstants.CONFIG_ID_PARAM, Integer.class);
    }

    @Override
    public String getNullConfigurationMessage() {
        int configId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.CONFIG_ID_PARAM, Integer.class);
        return "This connection properties update with id " + configId + " has not been initialized.";
    }

    @Override
    public String getNullConfigurationDefinitionMessage() {
        int configId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.CONFIG_ID_PARAM, Integer.class);
        return "Could not lookup structure for conneciton properties with id " + configId + " for rendering.";
    }
}