/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.drift;

import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view that displays a tree of all change sets and their related drift trails.
 *
 * @author John Mazzitelli
 */
public class DriftChangeSetsView extends LocatableVLayout {

    private EntityContext context;
    private boolean hasWriteAccess;

    protected DriftChangeSetsView(String locatorId, String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(locatorId);
        this.context = context;
        this.hasWriteAccess = hasWriteAccess;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        final ResourceDriftChangeSetsTreeView tree = new ResourceDriftChangeSetsTreeView(extendLocatorId("Tree"),
            this.hasWriteAccess, this.context);
        addMember(tree);

        ToolStrip toolStrip = new LocatableToolStrip(extendLocatorId("toolstrip"));
        toolStrip.setBackgroundImage(null);
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(3);
        toolStrip.setPadding(3);
        IButton refreshButton = new LocatableIButton(extendLocatorId("refreshButton"), MSG.common_button_refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                tree.refresh();
            }
        });
        toolStrip.addMember(refreshButton);
        addMember(toolStrip);
    }

    public EntityContext getContext() {
        return context;
    }

    protected boolean hasWriteAccess() {
        return this.hasWriteAccess;
    }
}
