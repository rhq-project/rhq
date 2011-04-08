/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.factory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardView;

/**
 * @author Jay Shaughnessy
 */
public abstract class AbstractResourceFactoryWizard extends AbstractWizard {

    private Resource parentResource;
    private ResourceType childType;

    private String newResourceName;
    private String newResourceVersion;
    private Integer newResourceArchitectureId;
    // depending on the situation the next three fields may be for a plugin, resource or deploy time config 
    private ConfigurationDefinition newResourceConfigurationDefinition;
    private Configuration newResourceStartingConfiguration;
    private Configuration newResourceConfiguration;

    private WizardView view;

    public AbstractResourceFactoryWizard(Resource parentResource, ResourceType childType) {
        this.parentResource = parentResource;
        this.childType = childType;

        assert parentResource != null;
        assert childType != null;
    }

    public String getSubtitle() {
        return null;
    }

    abstract public void execute();

    public void display() {
        view = new WizardView(this);
        view.displayDialog();
    }

    public Resource getParentResource() {
        return parentResource;
    }

    public ResourceType getChildType() {
        return childType;
    }

    public String getNewResourceName() {
        return newResourceName;
    }

    public void setNewResourceName(String newResourceName) {
        this.newResourceName = newResourceName;
    }

    public String getNewResourceVersion() {
        return newResourceVersion;
    }

    public void setNewResourceVersion(String newResourceVersion) {
        this.newResourceVersion = newResourceVersion;
    }

    public Integer getNewResourceArchitectureId() {
        return newResourceArchitectureId;
    }

    public void setNewResourceArchitectureId(Integer newResourceArchitectureId) {
        this.newResourceArchitectureId = newResourceArchitectureId;
    }

    public ConfigurationDefinition getNewResourceConfigurationDefinition() {
        return newResourceConfigurationDefinition;
    }

    public void setNewResourceConfigurationDefinition(ConfigurationDefinition newResourceConfigurationDefinition) {
        if (null == this.newResourceConfigurationDefinition
            || !this.newResourceConfigurationDefinition.equals(newResourceConfigurationDefinition)) {
            this.newResourceConfigurationDefinition = newResourceConfigurationDefinition;
            if (newResourceConfigurationDefinition != null) {
                this.newResourceStartingConfiguration = this.newResourceConfigurationDefinition.getDefaultTemplate()
                    .createConfiguration();
            }
        }
    }

    public Configuration getNewResourceStartingConfiguration() {
        return newResourceStartingConfiguration;
    }

    public void setNewResourceStartingConfiguration(Configuration newResourceStartingConfiguration) {
        this.newResourceStartingConfiguration = newResourceStartingConfiguration;
    }

    public Configuration getNewResourceConfiguration() {
        return newResourceConfiguration;
    }

    public void setNewResourceConfiguration(Configuration newResourceConfiguration) {
        this.newResourceConfiguration = newResourceConfiguration;
    }

    public void cancel() {
        // nothing to do
    }

}
