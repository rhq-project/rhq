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
package org.rhq.enterprise.gui.coregui.client.components.wizard;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public abstract class AbstractWizardStep implements WizardStep {

    abstract public Canvas getCanvas();

    abstract public String getName();

    public boolean nextPage() {
        return true;
    }

    public boolean previousPage() {
        return true;
    }

    /**
     * Convenience routine returns a DynamicForm tagged for Selenium
     * @return the form
     */
    protected DynamicForm getDynamicForm() {
        return new LocatableDynamicForm(getName());
    }

    /**
     * Convenience routine returns a VLayout tagged for Selenium
     * @return the VLayout
     */
    protected VLayout getVLayout() {
        return new LocatableVLayout(getName());
    }
}
