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
package org.rhq.coregui.client.bundle.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.bundle.BundleSelector;
import org.rhq.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Jay Shaughnessy
 */
public class BundleGroupEditView extends AbstractRecordEditor<BundleGroupsDataSource> {

    private static final String HEADER_ICON = IconEnum.BUNDLE_GROUP.getIcon24x24Path();

    private Tab bundlesTab;
    private BundleSelector bundleSelector;
    private Set<Permission> globalPermissions;
    private Integer permissionCheckBundleGroupId = null;
    private boolean canManage;
    private boolean canAssign;
    private boolean canUnassign;

    public BundleGroupEditView(Set<Permission> globalPermissions, int bundleGroupId) {
        super(new BundleGroupsDataSource(), bundleGroupId, MSG.common_title_bundleGroups(), HEADER_ICON);

        this.globalPermissions = globalPermissions;
    }

    @Override
    public void renderView(final ViewPath viewPath) {
        final int bundleGroupId = Integer.parseInt(viewPath.getCurrent().getPath());

        // if we have already determined permissions for this bundle, just proceed
        if (null != permissionCheckBundleGroupId && permissionCheckBundleGroupId.intValue() == bundleGroupId) {
            authorizedRenderView(bundleGroupId, viewPath);
            return;
        }

        // check necessary global permissions
        canManage = globalPermissions.contains(Permission.MANAGE_BUNDLE_GROUPS);
        canAssign = canManage;
        canUnassign = canManage;

        // If the user has global perms to manage bundle groups then proceed, otherwise, we
        // need to see what bundle group level perms he has.
        if (canManage) {
            authorizedRenderView(bundleGroupId, viewPath);

        } else {
            new PermissionsLoader().loadBundleGroupPermissions(bundleGroupId, new PermissionsLoadedListener() {
                @Override
                public void onPermissionsLoaded(Set<Permission> bundleGroupPermissions) {
                    canAssign = bundleGroupPermissions.contains(Permission.CREATE_BUNDLES_IN_GROUP)
                        || bundleGroupPermissions.contains(Permission.ASSIGN_BUNDLES_TO_GROUP);
                    canUnassign = bundleGroupPermissions.contains(Permission.DELETE_BUNDLES_FROM_GROUP)
                        || bundleGroupPermissions.contains(Permission.UNASSIGN_BUNDLES_FROM_GROUP);

                    authorizedRenderView(bundleGroupId, viewPath);
                }
            });
        }
    }

    private void authorizedRenderView(final int bundleGroupId, final ViewPath viewPath) {
        permissionCheckBundleGroupId = bundleGroupId;

        super.renderView(viewPath);

        init(!(canManage || canAssign || canUnassign));
    }

    @Override
    protected EnhancedVLayout buildContentPane() {
        EnhancedVLayout contentPane = new EnhancedVLayout();
        contentPane.setWidth100();
        contentPane.setHeight100();
        contentPane.setOverflow(Overflow.AUTO);

        EnhancedDynamicForm form = buildForm();
        setForm(form);

        EnhancedVLayout topPane = new EnhancedVLayout();
        topPane.setWidth100();
        topPane.setHeight(80);
        topPane.addMember(form);

        contentPane.addMember(topPane);

        TabSet tabSet = new TabSet();
        tabSet.setWidth100();
        tabSet.setHeight100();

        this.bundlesTab = buildBundlesTab(tabSet);
        tabSet.addTab(bundlesTab);

        contentPane.addMember(tabSet);

        return contentPane;
    }

    @Override
    protected boolean isFormReadOnly() {
        return !canManage;
    }

    private Tab buildBundlesTab(TabSet tabSet) {
        Tab tab = new Tab(MSG.common_title_bundles(), ImageManager.getBundleIcon());
        // NOTE: We will set the tab content to the bundle selector later, once the Bundle Group has been fetched.

        return tab;
    }

    @Override
    protected Record createNewRecord() {
        BundleGroup bundleGroup = new BundleGroup();
        Record bundleGroupRecord = BundleGroupsDataSource.getInstance().copyValues(bundleGroup);
        return bundleGroupRecord;
    }

    @Override
    protected void editRecord(Record record) {
        super.editRecord(record);

        Record[] bundleRecords = record.getAttributeAsRecordArray(BundleGroupsDataSource.FIELD_BUNDLES);
        ListGridRecord[] bundleListGridRecords = toListGridRecordArray(bundleRecords);

        this.bundleSelector = new BundleSelector(bundleListGridRecords, canAssign, canUnassign);
        this.bundleSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {
            public void onSelectionChanged(AssignedItemsChangedEvent event) {
                BundleGroupEditView.this.onItemChanged();
            }
        });
        updateTab(this.bundlesTab, this.bundleSelector);
    }

    private static void updateTab(Tab tab, Canvas content) {
        if (tab == null) {
            throw new IllegalStateException("A null tab was specified.");
        }
        tab.getTabSet().updateTab(tab, content);
    }

    @Override
    protected List<FormItem> createFormItems(EnhancedDynamicForm form) {
        List<FormItem> items = new ArrayList<FormItem>();

        TextItem nameItem = new TextItem(BundleGroupsDataSource.FIELD_NAME);
        nameItem.setShowTitle(true);
        nameItem.setSelectOnFocus(true);
        nameItem.setTabIndex(1);
        nameItem.setAttribute(EnhancedDynamicForm.OUTPUT_AS_HTML_ATTRIBUTE, true);
        items.add(nameItem);

        TextItem descriptionItem = new TextItem(BundleGroupsDataSource.FIELD_DESCRIPTION);
        descriptionItem.setShowTitle(true);
        descriptionItem.setTabIndex(5);
        descriptionItem.setColSpan(form.getNumCols());
        descriptionItem.setAttribute(EnhancedDynamicForm.OUTPUT_AS_HTML_ATTRIBUTE, true);
        items.add(descriptionItem);

        return items;
    }

    @Override
    protected void save(DSRequest requestProperties) {
        // Grab the currently assigned bundles from the selector and stick them into the corresponding canvas
        // item on the form, so when the form is saved, they'll get submitted along with the rest of the simple fields
        // to the datasource's add or update methods.
        if (bundleSelector != null) {
            ListGridRecord[] bundleRecords = this.bundleSelector.getSelectedRecords();
            getForm().setValue(BundleGroupsDataSource.FIELD_BUNDLES, bundleRecords);
        }

        // Submit the form values to the datasource.
        super.save(requestProperties);
    }

    @Override
    protected void reset() {
        super.reset();

        if (this.bundleSelector != null) {
            this.bundleSelector.reset();
        }
    }

}
