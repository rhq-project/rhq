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

import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * @author John Sanda
 * @author Jay Shaughnessy
 */
public class TwoLevelTab extends NamedTab {
    private SubTabLayout layout;
    
    private TwoLevelTab actualNext;
    private TwoLevelTab visibleNext;

    /**
     * Use the more type safe version instead.<br/>
     * <code>TwoLevelTab(String locatorId, ViewName viewName, String icon)</code>
     * @param locatorId
     * @param viewName
     * @param icon String representation of Icon not as typesafe as IconEnum.
     */
    @Deprecated
    public TwoLevelTab(String locatorId, ViewName viewName, String icon) {
        super(locatorId, viewName, icon);

        layout = new SubTabLayout();
    }

    public TwoLevelTab(String locatorId, ViewName viewName, IconEnum iconEnum) {
        super(locatorId, viewName, iconEnum.getIcon16x16DisabledPath());

        layout = new SubTabLayout();
    }

    public void registerSubTabs(SubTab... subTabs) {
        layout.registerSubTabs(subTabs);
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

    /**
     * This is the successor or tab immediately to the right of this tab when all tabs
     * are visible. The tab to which actualNext refers does not change whereas the tab to
     * which {@link #getVisibleNext visibleNext} refers can change.
     *
     * @return The successor or tab immediately to the right of this tab when all tabs are
     * visible.
     */
    public TwoLevelTab getActualNext() {
        return actualNext;
    }

    /**
     * @param actualNext The successor or tab immediately to the right of this tab when all
     * tabs are visible. The tab to which actualNext refers does not change whereas the tab
     * to which {@link #getVisibleNext visibleNext} refers can change.
     */
    public void setActualNext(TwoLevelTab actualNext) {
        this.actualNext = actualNext;
    }

    /**
     * The successor or tab immediately to the right of this tab among the set of visible
     * tabs. The tab to which visibleNext refers can change whereas the tab to which
     * {@link #getActualNext actualNext} refers will not change.
     * 
     * @return The successor or tab immediately to the right of this tab among the set of
     * visible tabs.
     */
    public TwoLevelTab getVisibleNext() {
        return visibleNext;
    }

    /**
     * @param visibleNext The successor or tab immediately to the right of this tab among
     * the set of visible tabs. The tab to which visibleNext refers can change whereas the
     * tab to which {@link #getActualNext actualNext} refers will not change.
     */
    public void setVisibleNext(TwoLevelTab visibleNext) {
        this.visibleNext = visibleNext;
    }

    @Override
    public String toString() {
        return "TwoLevelTab[title=" + getTitle() + ", locatorId=" + getLocatorId() + "]";
    }
}
