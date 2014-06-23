/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource.detail;

import java.util.Arrays;
import java.util.List;

import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.help.RhAccessView;

public class OpenSupportCaseMenuItem extends MenuItem {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList("JBossAS7 Host Controller",
        "JBossAS7 Standalone Server", "Managed Server");
    private final ResourceComposite resourceComposite;

    public OpenSupportCaseMenuItem(ResourceComposite rc) {
        super(RhAccessView.PAGE_RESOURCE_CASE.getTitle());
        this.resourceComposite = rc;

        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(MenuItemClickEvent event) {
                CoreGUI.goToView(RhAccessView.VIEW_ID.getName() + "/" + RhAccessView.PAGE_RESOURCE_CASE.getName() + "/"
                    + resourceComposite.getResource().getId());
            }
        });
    }

    public boolean isToBeIncluded() {
        if (CoreGUI.isRHQ()) {
            //return false; TODO disable for RHQ !!
        }
        Resource resource = resourceComposite.getResource();
        if (resource.getResourceType().getPlugin().equals("JBossAS7")) {
            if (SUPPORTED_TYPES.contains(resource.getResourceType().getName())) {
                return true;
            }
        }
        return false;
    }

}
