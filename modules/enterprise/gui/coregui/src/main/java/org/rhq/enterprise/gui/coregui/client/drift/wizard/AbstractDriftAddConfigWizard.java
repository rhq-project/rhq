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

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardView;

/**
 * @author Jay Shaughnessy
 */
public abstract class AbstractDriftAddConfigWizard extends AbstractWizard {

    private EntityContext context;
    private ResourceType type;

    private Configuration newStartingConfiguration;
    private DriftConfiguration newDriftConfiguration;

    private WizardView view;

    public AbstractDriftAddConfigWizard(final EntityContext context, ResourceType type) {
        this.context = context;
        assert context != null;

        this.type = type;
        assert type != null;
    }

    public String getSubtitle() {
        return null;
    }

    abstract public void execute();

    public void display() {
        view = new WizardView(this);
        view.displayDialog();
    }

    public EntityContext getEntityContext() {
        return context;
    }

    public ResourceType getType() {
        return type;
    }

    public Configuration getNewStartingConfiguration() {
        return newStartingConfiguration;
    }

    public void setNewStartingConfiguration(Configuration newStartingConfiguration) {
        this.newStartingConfiguration = newStartingConfiguration;
    }

    public DriftConfiguration getNewDriftConfiguration() {
        return newDriftConfiguration;
    }

    public void setNewConfiguration(Configuration newDriftConfiguration) {
        this.newDriftConfiguration = new DriftConfiguration(newDriftConfiguration);
    }

    public void cancel() {
        // nothing to do
    }

}
