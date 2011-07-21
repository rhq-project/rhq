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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.drift.wizard.DriftAddConfigWizard;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A view that displays a paginated table of {@link org.rhq.core.domain.drift.DriftConfiguration}s, along with the
 * ability to filter (maybe) those drift configs, sort those drift configs, double-click a row to view/edit the
 * drift config, and perform various actionns: add/delete, etc.
 * This view full respects the user's authorization, and will not allow actions on the drifts unless the user is
 * either the inventory manager or has MANAGE_DRIFT permission on every resource corresponding to the drift configs
 * being operated on.
 *
 * @author Jay Shaughnessy
 */
public class DriftConfigurationView extends TableSection<DriftConfigurationDataSource> {

    public static final ViewName SUBSYSTEM_VIEW_ID = new ViewName("DriftConfigs", MSG.common_title_configuration());

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(DriftConfigurationDataSource.ATTR_NAME,
        SortDirection.ASCENDING);

    private static final Criteria INITIAL_CRITERIA = new Criteria();

    private EntityContext context;
    private boolean hasWriteAccess;
    private DriftConfigurationDataSource dataSource;

    static {
        DriftCategory[] categoryValues = DriftCategory.values();
        String[] categoryNames = new String[categoryValues.length];
        int i = 0;
        for (DriftCategory c : categoryValues) {
            categoryNames[i++] = c.name();
        }

        // Add any INITIAL_CRITERIA here (non currently)
    }

    // for subsystem views
    public DriftConfigurationView(String locatorId) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(), false);
    }

    public DriftConfigurationView(String locatorId, EntityContext entityContext) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), entityContext, false);
    }

    public DriftConfigurationView(String locatorId, String tableTitle, EntityContext entityContext) {
        this(locatorId, tableTitle, entityContext, false);
    }

    protected DriftConfigurationView(String locatorId, String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(locatorId, tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasWriteAccess = hasWriteAccess;

        setInitialCriteriaFixed(false);
        setDataSource(getDataSource());
    }

    @Override
    public DriftConfigurationDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new DriftConfigurationDataSource(context);
        }
        return this.dataSource;
    }

    @Override
    protected void configureTableFilters() {
        // currently no table filters
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));
        setupTableInteractions(this.hasWriteAccess);

        super.configureTable();
    }

    private void setupTableInteractions(final boolean hasWriteAccess) {
        TableActionEnablement deleteEnablement = hasWriteAccess ? TableActionEnablement.ANY
            : TableActionEnablement.NEVER;
        TableActionEnablement detectNowEnablement = hasWriteAccess ? TableActionEnablement.SINGLE
            : TableActionEnablement.NEVER;

        addTableAction("Add", MSG.common_button_add(), null, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return hasWriteAccess;
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                add();
            }
        });

        addTableAction("Delete", MSG.common_button_delete(), MSG.view_drift_delete_confirm(), new AbstractTableAction(
            deleteEnablement) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                delete(selection);
            }
        });

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

        addTableAction("DetectNow", MSG.view_drift_button_detectNow(), null, new AbstractTableAction(
            detectNowEnablement) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                detectDrift(selection);
            }
        });
    }

    private void add() {
        DriftAddConfigWizard.showWizard(context, this);
    }

    private void delete(ListGridRecord[] records) {
        final int[] driftConfigIds = new int[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer driftConfigId = record.getAttributeAsInt("id");
            driftConfigIds[i] = driftConfigId;
        }

        GWTServiceLookup.getDriftService().deleteDriftConfigurations(driftConfigIds, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_drift_success_deleteConfigs(String.valueOf(resultCount)),
                        Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_delete(Arrays.toString(driftConfigIds)),
                    caught);
            }
        });
    }

    private void deleteAll() {
        GWTServiceLookup.getDriftService().deleteDriftConfigurationsByContext(context, new AsyncCallback<Integer>() {
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

    private void detectDrift(ListGridRecord[] records) {
        // why only the [0] item?
        DriftConfiguration driftConfig = (DriftConfiguration) records[0]
            .getAttributeAsObject(DriftConfigurationDataSource.ATTR_ENTITY);
        GWTServiceLookup.getDriftService().detectDrift(context, driftConfig, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_drift_success_detectNow(), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_detectNow(), caught);
            }
        });
    }

    @Override
    public Canvas getDetailsView(int driftConfigId) {
        return new DriftConfigurationEditView(extendLocatorId("ConfigEdit"), context, driftConfigId, hasWriteAccess);
    }

    public EntityContext getContext() {
        return context;
    }
}
