/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary;

import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The content pane for the group Summary>Activity subtab.
 *
 * @author Ian Springer
 */
// TODO: Implement this.
public class ActivityView extends LocatableVLayout implements RefreshableView {

    private ResourceGroupComposite groupComposite;

    public ActivityView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);
        this.groupComposite = groupComposite;
    }

    @Override
    protected void onInit() {
        super.onInit();

        Label label = new Label("<h3>Coming soon...</h3>");
        addMember(label);
    }

    @Override
    public void refresh() {
        // TODO: Reload the data.
    }

}
