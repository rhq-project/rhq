/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.report.operation;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDetailsView;

/**
 * @author Ian Springer
 */
public class SubsystemOperationHistoryListView extends AbstractOperationHistoryListView {

    public static final ViewName VIEW_ID = new ViewName("RecentOperations", MSG.common_title_recent_operations());

    private Set<Permission> globalPermissions;

    public SubsystemOperationHistoryListView(String locatorId) {
        super(locatorId, new ResourceOperationHistoryDataSource(), VIEW_ID.getTitle());

        this.globalPermissions = EnumSet.noneOf(Permission.class);
        loadGlobalPermissions();
    }

    @Override
    protected boolean hasControlPermission() {
        loadGlobalPermissions();
        return this.globalPermissions.contains(Permission.MANAGE_INVENTORY);
    }

    private void loadGlobalPermissions() {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            public void onPermissionsLoaded(Set<Permission> permissions) {
                globalPermissions = permissions;
            }
        });
    }

    @Override
    protected List<ListGridField> createFields() {
        List<ListGridField> fields = super.createFields();

        ListGridField resourceField = createResourceField();
        resourceField.setWidth("20%");
        fields.add(1, resourceField);

        ListGridField ancestryField = createAncestryField();
        resourceField.setWidth("20%");
        fields.add(2, ancestryField);

        for (ListGridField field : fields) {
            String fieldName = field.getName();
            if (fieldName.equals(AbstractOperationHistoryDataSource.Field.SUBJECT)) {
                field.setWidth("10%");
            }
            if (fieldName.equals(AbstractOperationHistoryDataSource.Field.OPERATION_NAME)
                || fieldName.equals(AbstractOperationHistoryDataSource.Field.STARTED_TIME)) {
                field.setWidth("25%");
            }
        }

        return fields;
    }

    @Override
    public Canvas getDetailsView(int id) {
        return new ResourceOperationHistoryDetailsView(this.extendLocatorId("DetailsView"), true);
    }

}
