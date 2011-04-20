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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
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
    private static final Criteria INITIAL_CRITERIA = new Criteria();

    private static final String HEADER_ICON = "subsystems/control/Operation_24.png";
    private static final SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(
        OperationHistoryDataSource.Field.CREATED_TIME, SortDirection.DESCENDING);

    EntityContext context;
    boolean hasControlPermission;
    OperationHistoryDataSource dataSource;

    static {
        OperationRequestStatus[] statusValues = OperationRequestStatus.values();
        String[] statusNames = new String[statusValues.length];
        int i = 0;
        for (OperationRequestStatus s : statusValues) {
            statusNames[i++] = s.name();
        }

        INITIAL_CRITERIA.addCriteria(OperationHistoryDataSource.Field.STATUS, statusNames);
    }

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
        super(locatorId, tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasControlPermission = hasControlPermission;

        setInitialCriteriaFixed(false);
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
        LinkedHashMap<String, String> statusValues = new LinkedHashMap<String, String>(4);
        statusValues.put(OperationRequestStatus.SUCCESS.name(), MSG.common_status_success());
        statusValues.put(OperationRequestStatus.INPROGRESS.name(), MSG.common_status_inprogress());
        statusValues.put(OperationRequestStatus.CANCELED.name(), MSG.common_status_canceled());
        statusValues.put(OperationRequestStatus.FAILURE.name(), MSG.common_status_failed());
        LinkedHashMap<String, String> statusIcons = new LinkedHashMap<String, String>(3);
        statusIcons.put(OperationRequestStatus.SUCCESS.name(), ImageManager
            .getOperationResultsIcon(OperationRequestStatus.SUCCESS));
        statusIcons.put(OperationRequestStatus.INPROGRESS.name(), ImageManager
            .getOperationResultsIcon(OperationRequestStatus.INPROGRESS));
        statusIcons.put(OperationRequestStatus.CANCELED.name(), ImageManager
            .getOperationResultsIcon(OperationRequestStatus.CANCELED));
        statusIcons.put(OperationRequestStatus.FAILURE.name(), ImageManager
            .getOperationResultsIcon(OperationRequestStatus.FAILURE));

        SelectItem statusFilter = new EnumSelectItem(OperationHistoryDataSource.Field.STATUS, MSG
            .common_title_operation_status(), OperationRequestStatus.class, statusValues, statusIcons);
        statusFilter.setWidth(325);

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
