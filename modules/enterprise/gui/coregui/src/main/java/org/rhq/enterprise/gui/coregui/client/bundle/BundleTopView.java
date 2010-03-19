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
package org.rhq.enterprise.gui.coregui.client.bundle;

import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.enterprise.gui.coregui.client.bundle.create.BundleCreationWizard;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;

/**
 * @author Greg Hinkle
 */
public class BundleTopView extends HLayout {


    public BundleTopView() {
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        SectionStack sectionStack = new SectionStack();
        sectionStack.setShowResizeBar(true);
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth(250);
        sectionStack.setHeight100();

        sectionStack.addSection(new SectionStackSection("Bundles"));
        sectionStack.addSection(new SectionStackSection("Repositories"));
        sectionStack.addSection(new SectionStackSection("Providers"));

        addMember(sectionStack);

        addMember(new BundlesListView());
    }
}
