/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.bundle.create;

import java.util.Set;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.enterprise.gui.coregui.client.bundle.group.BundleGroupSelector;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;

/**
 * @author Jay Shaughnessy
 */
public class BundleGroupsStep extends AbstractWizardStep {

    private AbstractBundleCreateWizard wizard = null;
    private BundleGroupSelector selector = null;

    public BundleGroupsStep(AbstractBundleCreateWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas() {
        if (selector == null) {
            selector = new BundleGroupSelector();
        }
        return selector;
    }

    public Set<BundleGroup> getSelectedBundleGroups() {
        return selector.getSelectedItems();
    }

    public boolean nextPage() {
        return true;
    }

    public String getName() {
        return MSG.common_title_bundleGroups();
    }

}
