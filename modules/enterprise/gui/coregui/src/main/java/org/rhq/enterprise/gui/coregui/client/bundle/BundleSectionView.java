/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
import java.util.Set;

import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.bundle.group.BundleGroupsListView;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;


/**
 * This is a section stack view for the bundle main view (the right hand side) showing a bundles list view section
 * and a bundle groups list view section.
 * 
 * @author Jay Shaughnessy
 */
public class BundleSectionView extends SectionStack implements RefreshableView {

    final static Messages MSG = CoreGUI.getMessages();

    private SectionStackSection bundlesSection;
    private SectionStackSection bundleGroupsSection;
    private BundlesListView bundlesListView;
    private BundleGroupsListView bundleGroupsListView;

    private Set<Permission> globalPermissions;

    public BundleSectionView(Set<Permission> globalPermissions) {
        super();

        this.globalPermissions = globalPermissions;

        setVisibilityMode(VisibilityMode.MULTIPLE);
        setWidth100();
        setHeight100();
        
        init();
    }

    public void init() {
        bundlesSection = new SectionStackSection(MSG.common_title_bundles());
        bundlesListView = new BundlesListView(globalPermissions);
        bundlesSection.addItem(bundlesListView);
        bundlesSection.setExpanded(true);
        this.addSection(bundlesSection);

        bundleGroupsSection = new SectionStackSection(MSG.common_title_bundleGroups());
        bundleGroupsListView = new BundleGroupsListView(globalPermissions);
        bundleGroupsSection.addItem(bundleGroupsListView);
        bundleGroupsSection.setExpanded(true);
        this.addSection(bundleGroupsSection);

        setExpansion(true, true);
    }

    @Override
    public void refresh() {
        bundlesListView.refresh();
        bundleGroupsListView.refresh();
        markForRedraw();
    }

    public void setExpansion(boolean bundlesExpanded, boolean bundleGroupsExpanded) {
        bundlesSection.setExpanded(bundlesExpanded);
        bundleGroupsSection.setExpanded(bundleGroupsExpanded);
        markForRedraw();
    }

}
