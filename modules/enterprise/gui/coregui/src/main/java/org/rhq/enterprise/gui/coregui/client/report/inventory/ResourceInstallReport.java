/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.report.inventory;

import com.smartgwt.client.data.Criteria;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A tabular report that shows the types of resources are installed and how many
 * of them are installed.
 * 
 * @author John Mazzitelli
 */
public class ResourceInstallReport extends EnhancedVLayout implements BookmarkableView, HasViewName {

    public static final ViewName VIEW_ID = new ViewName("InventorySummary", MSG.common_title_inventorySummary(),
        IconEnum.INVENTORY_SUMMARY);

    private ResourceSearchView resourceList;

    public ResourceInstallReport() {
        super();
        setHeight100();
        setWidth100();
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            int resourceTypeId = Integer.parseInt(viewPath.getCurrent().getPath());
            viewPath.next();
            Criteria criteria;
            if (!viewPath.isEnd()) {
                String resourceVersion = viewPath.getCurrent().getPath();
                criteria = createResourceSearchViewCriteria(resourceTypeId, resourceVersion);
            } else {
                criteria = createResourceSearchViewCriteria(resourceTypeId);
            }
            showResourceList(criteria);
        } else {
            hideResourceList();
        }
    }

    @Override
    protected void onInit() {
        super.onInit();
        addMember(new InventorySummaryReportTable());
    }

    protected Criteria createResourceSearchViewCriteria(int resourceTypeId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(ResourceDataSourceField.TYPE.propertyName(), resourceTypeId);
        return criteria;
    }

    protected Criteria createResourceSearchViewCriteria(int resourceTypeId, String resourceVersion) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(ResourceDataSourceField.TYPE.propertyName(), resourceTypeId);
        criteria.addCriteria("version", resourceVersion);
        return criteria;
    }

    private void showResourceList(Criteria criteria) {
        hideResourceList();
        resourceList = new ResourceSearchView(criteria, true);
        addMember(resourceList);
        markForRedraw();
    }

    private void hideResourceList() {
        if (resourceList != null) {
            removeMember(resourceList);
            resourceList.destroy();
            resourceList = null;
        }
        markForRedraw();
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

}
