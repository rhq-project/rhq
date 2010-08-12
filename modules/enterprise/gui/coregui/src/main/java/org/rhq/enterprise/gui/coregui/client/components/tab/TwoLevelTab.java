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
import com.smartgwt.client.widgets.tab.Tab;

/**
 * @author Greg Hinkle
 */
public class TwoLevelTab extends Tab {

    private SubTabLayout layout;

    public TwoLevelTab(String title, String icon) {
        super(title, icon);

        layout = new SubTabLayout();
    }

    public void updateSubTab(String tab, Canvas canvas) {
        layout.updateSubTab(tab, canvas);
    }

    public void registerSubTabs(String... tabs) {
        for (String tab : tabs) {
            layout.registerSubTab(tab, null);
        }
    }

    public void setSubTabEnabled(String tab, boolean enabled) {
        if (enabled) {
            layout.enableSubTab(tab);
        } else {
            layout.disableSubTab(tab);
        }
    }

    public SubTabLayout getLayout() {
        return layout;
    }

    @Override
    public Canvas getPane() {
        return layout;
    }

}
