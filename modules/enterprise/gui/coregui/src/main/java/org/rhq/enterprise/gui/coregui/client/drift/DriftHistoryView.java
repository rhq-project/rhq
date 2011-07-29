/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.drift;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection2;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A view that displays a paginated table of {@link org.rhq.core.domain.drift.RhqDrift}s, along with the
 * ability to filter those drifts, sort those drifts, double-click a row to view full details a drift, and perform
 * various actions on the the drifts: delete selected, delete all from source, etc.
 * This view full respects the user's authorization, and will not allow acttions on the drifts unless the user is
 * either the inventory manager or has MANAGE_DRIFT permission on every resource corresponding to the drifts being
 * operated on.
 *
 * @author Jay Shaughnessy
 */
public class DriftHistoryView extends TableSection2<DriftDataSource> {

    public static final ViewName SUBSYSTEM_VIEW_ID = new ViewName("RecentDrifts", MSG.common_title_recent_drifts());

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(DriftDataSource.ATTR_CTIME,
        SortDirection.DESCENDING);

    private static final Criteria INITIAL_CRITERIA = new Criteria();

    private EntityContext context;
    private boolean hasWriteAccess;
    private DriftDataSource dataSource;

    static {
        DriftCategory[] categoryValues = DriftCategory.values();
        String[] categoryNames = new String[categoryValues.length];
        int i = 0;
        for (DriftCategory c : categoryValues) {
            categoryNames[i++] = c.name();
        }

        INITIAL_CRITERIA.addCriteria(DriftDataSource.FILTER_CATEGORIES, categoryNames);
    }

    // for subsystem views
    public DriftHistoryView(String locatorId) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(), false);
    }

    public DriftHistoryView(String locatorId, EntityContext entityContext) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), entityContext, false);
    }

    public DriftHistoryView(String locatorId, String tableTitle, EntityContext entityContext) {
        this(locatorId, tableTitle, entityContext, false);
    }

    protected DriftHistoryView(String locatorId, String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(locatorId, tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasWriteAccess = hasWriteAccess;

        setInitialCriteriaFixed(false);
        setDataSource(getDataSource());
    }

    @Override
    public DriftDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new DriftDataSource(context);
        }
        return this.dataSource;
    }

    @Override
    protected void configureTableFilters() {
        LinkedHashMap<String, String> categories = new LinkedHashMap<String, String>(3);
        categories.put(DriftCategory.FILE_ADDED.name(), MSG.view_drift_category_fileAdded());
        categories.put(DriftCategory.FILE_CHANGED.name(), MSG.view_drift_category_fileChanged());
        categories.put(DriftCategory.FILE_REMOVED.name(), MSG.view_drift_category_fileRemoved());
        LinkedHashMap<String, String> categoryIcons = new LinkedHashMap<String, String>(3);
        categoryIcons.put(DriftCategory.FILE_ADDED.name(), ImageManager.getDriftCategoryIcon(DriftCategory.FILE_ADDED));
        categoryIcons.put(DriftCategory.FILE_CHANGED.name(), ImageManager
            .getDriftCategoryIcon(DriftCategory.FILE_CHANGED));
        categoryIcons.put(DriftCategory.FILE_REMOVED.name(), ImageManager
            .getDriftCategoryIcon(DriftCategory.FILE_REMOVED));
        SelectItem categoryFilter = new EnumSelectItem(DriftDataSource.FILTER_CATEGORIES, MSG.common_title_category(),
            DriftCategory.class, categories, categoryIcons);

        if (isShowFilterForm()) {
            setFilterFormItems(categoryFilter);
        }
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));
        setupTableInteractions(this.hasWriteAccess);

        super.configureTable();
    }

    @Override
    protected String getDetailsLinkColumnName() {
        return DriftDataSource.ATTR_CTIME;
    }

    @Override
    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                Integer resourceId = record.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                String driftId = getId(record);
                String url = LinkManager.getSubsystemDriftHistoryLink(resourceId, driftId);
                String formattedValue = TimestampCellFormatter.format(value);
                return SeleniumUtility.getLocatableHref(url, formattedValue, null);
            }
        };
    }

    protected void setupTableInteractions(final boolean hasWriteAccess) {
        TableActionEnablement singleTargetEnablement = hasWriteAccess ? TableActionEnablement.ANY
            : TableActionEnablement.NEVER;

        addTableAction("DeleteDrift", MSG.common_button_delete(), MSG.view_drift_delete_confirm(),
            new AbstractTableAction(singleTargetEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    delete(selection);
                }
            });

        // TODO add ack
        /*
        addTableAction("AcknowledgeDrift", MSG.common_button_ack(), MSG.view_drift_ack_confirm(),
            new AbstractTableAction(singleTargetEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    acknowledge(selection);
                }
            });
        */

        addTableAction("DeleteAll", MSG.common_button_delete_all(), MSG.view_drift_delete_confirmAll(),
            new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    ListGrid grid = getListGrid();
                    ResultSet resultSet = (null != grid) ? grid.getResultSet() : null;
                    return (hasWriteAccess && grid != null && resultSet != null && !resultSet.isEmpty());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteAll();
                }
            });

        /*
        addTableAction("AcknowledgeAll", MSG.common_button_ack_all(), MSG.iew_alerts_ack_confirm_all(),
            new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    ListGrid grid = getListGrid();
                    ResultSet resultSet = (null != grid) ? grid.getResultSet() : null;
                    return (hasWriteAccess && grid != null && resultSet != null && !resultSet.isEmpty());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    acknowledgeAll();
                }
            });
        */
    }

    private void delete(ListGridRecord[] records) {
        final String[] driftIds = new String[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            String driftId = record.getAttributeAsString(DriftDataSource.ATTR_ID);
            driftIds[i] = driftId;
        }

        GWTServiceLookup.getDriftService().deleteDrifts(driftIds, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_drift_success_delete(String.valueOf(resultCount)), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_delete(Arrays.toString(driftIds)), caught);
            }
        });
    }

    private void deleteAll() {
        GWTServiceLookup.getDriftService().deleteDriftsByContext(context, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_drift_success_delete(String.valueOf(resultCount)), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_deleteAll(), caught);
            }
        });
    }

    //    private void acknowledge(ListGridRecord[] records) {
    //        final int[] alertIds = new int[records.length];
    //        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
    //            ListGridRecord record = records[i];
    //            Integer alertId = record.getAttributeAsInt(DriftDataSource.ATTR_ID);
    //            alertIds[i] = alertId;
    //        }
    //
    //        GWTServiceLookup.getAlertService().acknowledgeAlerts(alertIds, new AsyncCallback<Integer>() {
    //            public void onSuccess(Integer resultCount) {
    //                CoreGUI.getMessageCenter().notify(
    //                    new Message(MSG.iew_alerts_ack_success(String.valueOf(resultCount)), Message.Severity.Info));
    //                refresh();
    //            }
    //
    //            public void onFailure(Throwable caught) {
    //                CoreGUI.getErrorHandler().handleError(MSG.iew_alerts_ack_failure(Arrays.toString(alertIds)), caught);
    //            }
    //        });
    //    }

    //    private void acknowledgeAll() {
    //        GWTServiceLookup.getAlertService().acknowledgeAlertsByContext(context, new AsyncCallback<Integer>() {
    //            public void onSuccess(Integer resultCount) {
    //                CoreGUI.getMessageCenter().notify(
    //                    new Message(MSG.iew_alerts_ack_success(String.valueOf(resultCount)), Message.Severity.Info));
    //                refresh();
    //            }
    //
    //            public void onFailure(Throwable caught) {
    //                CoreGUI.getErrorHandler().handleError(MSG.iew_alerts_ack_failure_all(), caught);
    //            }
    //        });
    //    }


    @Override
    public Canvas getDetailsView(String driftId) {
        return DriftDetailsView.getInstance();
    }

    public EntityContext getContext() {
        return context;
    }
}
