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
package org.rhq.enterprise.gui.coregui.client.components.tab;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
public class TwoLevelTab extends NamedTab {
    private SubTabLayout layout;

    public TwoLevelTab(String locatorId, ViewName viewName, String icon) {
        super(locatorId, viewName, icon);

        layout = new SubTabLayout(locatorId);
    }

    public void registerSubTabs(SubTab... subTabs) {
        for (SubTab subTab : subTabs) {
            layout.registerSubTab(subTab);
        }
    }

    public void setVisible(SubTab subTab, boolean visible) {
        if (visible) {
            layout.showSubTab(subTab);
        } else {
            layout.hideSubTab(subTab);
        }
    }

    public void setSubTabEnabled(SubTab subTab, boolean enabled) {
        if (enabled) {
            layout.enableSubTab(subTab);
        } else {
            layout.disableSubTab(subTab);
        }
    }

    public SubTab getSubTabByName(String name) {
        return this.layout.getSubTabByName(name);
    }

    public SubTab getSubTabByLocatorId(String locatorId) {
        return this.layout.getSubTabByLocatorId(locatorId);
    }

    public SubTab getDefaultSubTab() {
        return this.layout.getDefaultSubTab();
    }

    public SubTabLayout getLayout() {
        return layout;
    }

    @Override
    public Canvas getPane() {
        return layout;
    }

    @Override
    public String toString() {
        return "TwoLevelTab[title=" + getTitle() + ", locatorId=" + getLocatorId() + "]";
    }
}
