/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.operation;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.AbstractConfigurationHistoryDataSource.Field;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDetailsView;

/**
 * A view that displays a paginated table of operation history. Support exists of subsystem and resource contexts.
 * Group operation history is (currently) handled separately as the view is comprised of group operation history
 * entities, not [resource] operation history entities. 
 (
 * @author Jay Shaughnessy
 */
public class OperationHistoryView extends TableSection<OperationHistoryDataSource> {

    public static final ViewName SUBSYSTEM_VIEW_ID = new ViewName("RecentOperations", MSG
        .common_title_recent_operations());

    private static final String HEADER_ICON = "subsystems/control/Operation_24.png";
    private static final SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(Field.CREATED_TIME,
        SortDirection.DESCENDING);

    EntityContext context;
    boolean hasControlPermission;
    OperationHistoryDataSource dataSource;

    // for subsystem views
    public OperationHistoryView(String locatorId) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(), false);
    }

    public OperationHistoryView(String locatorId, EntityContext entityContext) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), entityContext, false);
    }

    public OperationHistoryView(String locatorId, String tableTitle, EntityContext entityContext) {
        this(locatorId, tableTitle, entityContext, false);
    }

    protected OperationHistoryView(String locatorId, String tableTitle, EntityContext context,
        boolean hasControlPermission) {
        super(locatorId, tableTitle, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasControlPermission = hasControlPermission;

        setDataSource(getDataSource());
    }

    @Override
    public OperationHistoryDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new OperationHistoryDataSource(context);
        }
        return this.dataSource;
    }

    @Override
    protected void configureTableFilters() {
        SelectItem statusFilter = new SelectItem(Constant.OPERATION_STATUS, MSG.common_title_operation_status());
        statusFilter.setWrapTitle(false);
        statusFilter.setWidth(325);
        statusFilter.setMultiple(true);
        statusFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> stati = new LinkedHashMap<String, String>(4);
        stati.put(OperationRequestStatus.SUCCESS.name(), MSG.common_status_success());
        stati.put(OperationRequestStatus.INPROGRESS.name(), MSG.common_status_inprogress());
        stati.put(OperationRequestStatus.CANCELED.name(), MSG.common_status_canceled());
        stati.put(OperationRequestStatus.FAILURE.name(), MSG.common_status_failed());

        LinkedHashMap<String, String> statusIcons = new LinkedHashMap<String, String>(3);
        statusIcons.put(OperationRequestStatus.SUCCESS.name(), ImageManager
            .getOperationResultsIcon(OperationRequestStatus.SUCCESS));
        statusIcons.put(OperationRequestStatus.INPROGRESS.name(), ImageManager
            .getOperationResultsIcon(OperationRequestStatus.INPROGRESS));
        statusIcons.put(OperationRequestStatus.CANCELED.name(), ImageManager
            .getOperationResultsIcon(OperationRequestStatus.CANCELED));
        statusIcons.put(OperationRequestStatus.FAILURE.name(), ImageManager
            .getOperationResultsIcon(OperationRequestStatus.FAILURE));
        statusFilter.setValueMap(stati);
        statusFilter.setValueIcons(statusIcons);
        statusFilter.setValues(OperationRequestStatus.SUCCESS.name(), OperationRequestStatus.INPROGRESS.name(),
            OperationRequestStatus.FAILURE.name(), OperationRequestStatus.CANCELED.name());

        if (isShowFilterForm()) {
            setFilterFormItems(statusFilter);
        }
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));

        setupTableInteractions();

        super.configureTable();
    }

    protected boolean hasControlPermission() {
        return this.hasControlPermission;
    }

    protected void setupTableInteractions() {

        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), getDeleteConfirmMessage(),
            new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    return (count >= 1 && hasControlPermission());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteSelectedRecords();
                }
            });

        addTableAction(extendLocatorId("ForceDelete"), MSG.view_operationHistoryList_button_forceDelete(),
            getDeleteConfirmMessage(), new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    return (count >= 1 && hasControlPermission());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    DSRequest requestProperties = new DSRequest();
                    requestProperties.setAttribute("force", true);
                    deleteSelectedRecords(requestProperties);
                }
            });
    }

    public EntityContext getContext() {
        return context;
    }

    @Override
    public Canvas getDetailsView(int id) {
        return new ResourceOperationHistoryDetailsView(extendLocatorId("Detail"));
    }

}
