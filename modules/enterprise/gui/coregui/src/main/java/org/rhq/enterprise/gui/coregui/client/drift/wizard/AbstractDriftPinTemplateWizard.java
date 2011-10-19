/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.drift.wizard;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardView;

/**
 * @author Jay Shaughnessy
 */
public abstract class AbstractDriftPinTemplateWizard extends AbstractWizard {

    private ResourceType resourceType;
    private DriftDefinition snapshotDriftDef;
    private int snapshotVersion;
    private boolean createTemplate;
    private DriftDefinition newDriftDefinition;

    private DriftDefinitionTemplate selectedTemplate;

    private WizardView view;

    public AbstractDriftPinTemplateWizard(ResourceType resourceType, DriftDefinition snapshotDriftDef,
        int snapshotVersion) {

        this.resourceType = resourceType;
        this.snapshotDriftDef = snapshotDriftDef;
        this.snapshotVersion = snapshotVersion;
    }

    abstract public void execute();

    public String getSubtitle() {
        return null;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public DriftDefinition getSnapshotDriftDef() {
        return snapshotDriftDef;
    }

    public void setSnapshotDriftDef(DriftDefinition snapshotDriftDef) {
        this.snapshotDriftDef = snapshotDriftDef;
    }

    public int getSnapshotVersion() {
        return snapshotVersion;
    }

    public DriftDefinitionTemplate getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(DriftDefinitionTemplate template) {
        selectedTemplate = template;
    }

    public boolean isCreateTemplate() {
        return createTemplate;
    }

    public void setCreateTemplate(boolean createTemplate) {
        this.createTemplate = createTemplate;
    }

    public DriftDefinition getNewDriftDefinition() {
        return newDriftDefinition;
    }

    public void setNewConfiguration(Configuration newDriftDefinitionConfig) {
        newDriftDefinition = new DriftDefinition(newDriftDefinitionConfig);
    }

    public void display() {
        view = new WizardView(this);
        view.displayDialog();
    }

    public void cancel() {
        // nothing to do
    }

}
