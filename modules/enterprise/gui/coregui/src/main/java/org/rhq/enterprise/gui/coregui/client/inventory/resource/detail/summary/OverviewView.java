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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The Resource Summary>Overview tab.
 *
 * @author Lukas Krejci
 *
 * @deprecated This can be removed, since we now display the overview info in the collapsible pane above
 *             the Resource tabs (ips, 12/10/10).
 */
@Deprecated
public class OverviewView extends LocatableVLayout implements RefreshableView {

    private OverviewForm form;

    public OverviewView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);
        this.form = new OverviewForm(extendLocatorId("form"), resourceComposite);
        HTMLFlow separator = new HTMLFlow();
        separator.setContents("<hr>");

        form.setHeight("200");
        setLeft("10%");

        addMember(form);
    }

    @Override
    public void refresh() {
        form.loadData();        
    }

}
