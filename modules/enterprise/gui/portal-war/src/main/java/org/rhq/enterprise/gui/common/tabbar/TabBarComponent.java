/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.common.tabbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * A component that represents a tab bar containing tabs, and optionally subtabs.
 *
 * @author Ian Springer
 */
public class TabBarComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.jboss.on.TabBar";
    public static final String COMPONENT_FAMILY = "org.jboss.on.TabBar";

    private String selectedTabName;
    private Map<String, String> parameters = new HashMap<String, String>();

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public String getSelectedTabName() {
        if (this.selectedTabName == null) {
            this.selectedTabName = FacesComponentUtility.getExpressionAttribute(this, "selectedTabName");
        }

        return selectedTabName;
    }

    public void setSelectedTabName(String selectedTabName) {
        this.selectedTabName = selectedTabName;
    }

    @NotNull
    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(@NotNull
    Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void selectTab(@NotNull
    String fullTabName) {
        String tabName;
        String subtabName = null;

        // The full tab name is formatted as "tabName[.subtabName]" (e.g. "Inventory" or "Monitor.Visibility").
        // Break it down...
        if (fullTabName.contains(".")) {
            int dotIndex = fullTabName.indexOf('.');
            tabName = fullTabName.substring(0, dotIndex);
            subtabName = fullTabName.substring(dotIndex + 1);
        } else {
            tabName = fullTabName;
        }

        TabComponent selectedTab = getTabByName(tabName);
        if (selectedTab == null) {
            throw new IllegalStateException("'" + tabName + "' is not a valid tab name for this tab bar.");
        }

        selectedTab.setSelected(true);
        if (subtabName != null) {
            SubtabComponent selectedSubtab = selectedTab.getSubtabByName(subtabName);
            if (selectedSubtab == null) {
                throw new IllegalStateException("'" + subtabName + "' is not a valid subtab name for the '"
                    + selectedTab.getName() + "' tab.");
            }

            selectedSubtab.setSelected(true);
        }
    }

    @NotNull
    public List<TabComponent> getTabs() {
        List<TabComponent> tabs = new ArrayList<TabComponent>();
        if (getChildCount() == 0) {
            return tabs;
        }

        List<UIComponent> children = getChildren();
        for (UIComponent child : children) {
            if (child instanceof TabComponent) {
                tabs.add((TabComponent) child);
            }
        }

        return tabs;
    }

    @Nullable
    public TabComponent getTabByName(String tabName) {
        TabComponent selectedTab = null;
        for (TabComponent tab : getTabs()) {
            if (tab.getName().equals(tabName)) {
                selectedTab = tab;
                break;
            }
        }

        return selectedTab;
    }

    @NotNull
    public TabComponent getSelectedTab() {
        List<TabComponent> tabs = getTabs();
        for (TabComponent tab : tabs) {
            if (tab.isSelected()) {
                return tab;
            }
        }

        throw new IllegalStateException("No tab is selected.");
    }

    private Object[] stateValues;

    public Object saveState(FacesContext facesContext) {
        if (this.stateValues == null) {
            this.stateValues = new Object[2];
        }

        this.stateValues[0] = super.saveState(facesContext);
        this.stateValues[1] = this.selectedTabName;
        return this.stateValues;
    }

    public void restoreState(FacesContext context, Object stateValues) {
        this.stateValues = (Object[]) stateValues;
        super.restoreState(context, this.stateValues[0]);
        this.selectedTabName = (String) this.stateValues[1];
    }
}