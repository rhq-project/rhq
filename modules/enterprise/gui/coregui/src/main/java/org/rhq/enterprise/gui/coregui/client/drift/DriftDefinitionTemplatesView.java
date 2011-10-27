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

import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.admin.templates.DriftDefinitionTemplateTypeView;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.drift.wizard.DriftAddDefinitionWizard;

/**
 * A list view that displays a paginated table of {@link org.rhq.core.domain.drift.DriftDefinitionTemplate}s. It 
 * offers various options on the list like filtering (maybe) and sorting, add new/delete. Double-click drills
 * down to the detail view of the underlying Config. This view fully respects the user's authorization, and will not
 * allow actions on the templates  unless the user is either the inventory manager or has MANAGE_DRIFT permission.
 *
 * @author Jay Shaughnessy
 */
public class DriftDefinitionTemplatesView extends TableSection<DriftDefinitionTemplateDataSource> {

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(
        DriftDefinitionTemplateDataSource.ATTR_NAME, SortDirection.ASCENDING);

    private ResourceType type;
    private boolean hasWriteAccess;
    private DriftDefinitionTemplateDataSource dataSource;
    private boolean useSnapshotDetailsView;
    private String snapshotDriftDetailsId;

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
    public DriftDefinitionTemplatesView(String locatorId, ResourceType type, boolean hasWriteAccess) {
        this(locatorId, getTitle(type), type, hasWriteAccess);
    }

    protected DriftDefinitionTemplatesView(String locatorId, String tableTitle, ResourceType type,
        boolean hasWriteAccess) {
        super(locatorId, tableTitle, null, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });

        this.type = type;
        this.hasWriteAccess = hasWriteAccess;

        setDataSource(getDataSource());
    }

    @Override
    public DriftDefinitionTemplateDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new DriftDefinitionTemplateDataSource(type.getId());
        }
        return this.dataSource;
    }

    public static String getTitle(ResourceType type) {
        return DriftDefinitionTemplateTypeView.VIEW_ID.getTitle() + " [" + type.getName() + "]";
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

        TableActionEnablement addEnablement = hasWriteAccess ? TableActionEnablement.ALWAYS
            : TableActionEnablement.NEVER;
        TableActionEnablement deleteEnablement = hasWriteAccess ? TableActionEnablement.ANY
            : TableActionEnablement.NEVER;

        addTableAction("New", MSG.common_button_new(), new AbstractTableAction(addEnablement) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                add();
            }
        });

        addTableAction("Delete", MSG.common_button_delete(), MSG.view_drift_delete_defConfirm(),
            new AbstractTableAction(deleteEnablement) {

                boolean result = false;

                @Override
                public boolean isEnabled(ListGridRecord[] selection) {
                    if (super.isEnabled(selection)) {
                        for (ListGridRecord record : selection) {
                            if (!record.getAttributeAsBoolean(DriftDefinitionTemplateDataSource.ATTR_IS_USER_DEFINED)
                                .booleanValue()) {
                                break;
                            }
                        }
                        result = true;
                    }

                    return result;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    delete(selection);
                }
            });
    }

    private void add() {
        DriftAddDefinitionWizard.showWizard(EntityContext.forTemplate(type.getId()), this);

        // we can refresh the table buttons immediately since the wizard is a dialog, the
        // user can't access enabled buttons anyway.
        DriftDefinitionTemplatesView.this.refreshTableInfo();
    }

    private void delete(ListGridRecord[] records) {
        // TODO

        //        final String[] driftDefNames = new String[records.length];
        //        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
        //            ListGridRecord record = records[i];
        //            String driftDefName = record.getAttribute(DriftDefinitionDataSource.ATTR_NAME);
        //            driftDefNames[i] = driftDefName;
        //        }

        //deleteDriftDefinitionTemplatesByName(driftDefNames);
    }

    //    private void deleteDriftDefinitionsByName(final String[] driftDefNames) {
    //        GWTServiceLookup.getDriftService().deleteDriftDefinitionsByContext(context, driftDefNames,
    //            new AsyncCallback<Integer>() {
    //                public void onSuccess(Integer resultCount) {
    //                    CoreGUI.getMessageCenter().notify(
    //                        new Message(MSG.view_drift_success_deleteDefs(String.valueOf(resultCount)),
    //                            Message.Severity.Info));
    //                    refresh();
    //                }
    //
    //                public void onFailure(Throwable caught) {
    //                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_deleteDefs(), caught);
    //                }
    //            });
    //    }

    @Override
    public void renderView(ViewPath viewPath) {
        // we have two detail views for drift def templates, the config editor and the pinned snapshot. Figure out which
        // one we're dealing with. The default is the editor. 
        if (!viewPath.isEnd()) {
            this.useSnapshotDetailsView = !viewPath.isNextEnd() && "Snapshot".equals(viewPath.getNext().getPath());
            snapshotDriftDetailsId = null;
            if (viewPath.viewsLeft() > 1) {
                snapshotDriftDetailsId = viewPath.getViewForIndex(viewPath.getCurrentIndex() + 2).getPath().substring(
                    "0id_".length());
            }
        }

        super.renderView(viewPath);
    }

    @Override
    public Canvas getDetailsView(Integer driftTemplateId) {
        if (this.useSnapshotDetailsView) {
            if (null == snapshotDriftDetailsId) {
                return new DriftDefinitionTemplateSnapshotView(extendLocatorId("TemplateSnapshot"), driftTemplateId);
            }
            return new DriftDetailsView(extendLocatorId("TemplateSnapshotDrift"), snapshotDriftDetailsId);
        }

        return new DriftDefinitionTemplateEditView(extendLocatorId("TemplateEdit"), driftTemplateId, hasWriteAccess);
    }
}
