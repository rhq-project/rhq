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
package org.rhq.enterprise.gui.configuration.group;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.configuration.propset.ConfigurationSetMember;
import org.rhq.core.gui.configuration.propset.ConfigurationSet;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

import org.jboss.seam.faces.Redirect;

/**
 * An abstract base class for the Seam components for viewing and editing group Configurations. Requires the 'groupId'
 * request parameter to be specified.
 *
 * @author Ian Springer
 */
public abstract class AbstractGroupResourceConfigurationUIBean
{
    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    private ResourceGroup group;
    private Map<Integer, Configuration> resourceConfigurations;
    private ConfigurationSet configurationSet;

    /**
     * Load the ConfigurationDefinition and member Configurations for the current compatible group.
     */
    protected void loadConfigurations() {
        try {
            this.group = loadGroup();
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_FATAL, e.getMessage());
            return;
        }
        ConfigurationDefinition configurationDefinition = this.configurationManager
            .getResourceConfigurationDefinitionForResourceType(EnterpriseFacesContextUtility.getSubject(), this.group
                .getResourceType().getId());
        this.resourceConfigurations = this.configurationManager.getResourceConfigurationsForCompatibleGroup(this.group);
        List<ConfigurationSetMember> configurationSetMembers = new ArrayList(resourceConfigurations.size());
        for (Integer resourceId : this.resourceConfigurations.keySet()) {
            String label = createLabel(resourceId);
            Configuration configuration = this.resourceConfigurations.get(resourceId);
            ConfigurationSetMember configurationSetMember = new ConfigurationSetMember(label, configuration);
            configurationSetMembers.add(configurationSetMember);
        }
        this.configurationSet = new ConfigurationSet(configurationDefinition, configurationSetMembers);
        Redirect.instance().setParameter(ParamConstants.GROUP_ID_PARAM, this.group.getId());
        return;
    }

    private ResourceGroup loadGroup() throws Exception {
        ResourceGroup group;
        try {
            group = EnterpriseFacesContextUtility.getResourceGroup();
        } catch (Exception e) {
            throw new Exception("No group is associated with this request ('groupId' request parameter is not set).");
        }
        if (group.getGroupCategory() != GroupCategory.COMPATIBLE) {
            throw new Exception("Group with id " + group.getId() + " is not a compatible group.");
        }
        return group;
    }

    private String createLabel(Integer resourceId) {
        List<Resource> resourceLineage = this.resourceManager.getResourceLineage(resourceId);
        String previousName = resourceLineage.get(0).getName();
        StringBuilder label = new StringBuilder(previousName);
        for (int i = 1; i < resourceLineage.size(); i++) {
            Resource resource = resourceLineage.get(i);
            String name = resource.getName();
            name = (name.startsWith(previousName)) ? name.substring(previousName.length()) : name;
            label.append(" > ").append(name);
        }
        label.append(" (id=").append(resourceId).append(")");
        return label.toString();
    }

    public ConfigurationManagerLocal getConfigurationManager()
    {
        return configurationManager;
    }

    public ResourceManagerLocal getResourceManager()
    {
        return resourceManager;
    }

    public ResourceGroup getGroup()
    {
        return group;
    }

    public Map<Integer, Configuration> getResourceConfigurations()
    {
        return resourceConfigurations;
    }

    public ConfigurationSet getConfigurationSet()
    {
        return configurationSet;
    }
}
