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
package org.rhq.coregui.client.drift;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.Layout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftComplianceStatus;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.admin.templates.DriftDefinitionTemplateTypeView;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.drift.wizard.DriftAddDefinitionWizard;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.message.Message;

/**
 * A list view that displays a paginated table of {@link org.rhq.core.domain.drift.DriftDefinitionTemplate}s. It 
 * offers various options on the list like filtering (maybe) and sorting, add new/delete. Double-click drills
 * down to the detail view of the underlying Config. Rows expand to reveal the drift definitions derived from
 * the template.  This view fully respects the user's authorization, and will not allow actions on the templates
 * unless the user is either the inventory manager or has MANAGE_DRIFT permission.
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
    public DriftDefinitionTemplatesView(ResourceType type, boolean hasWriteAccess) {
        this(getTitle(type), type, hasWriteAccess);
    }

    protected DriftDefinitionTemplatesView(String tableTitle, ResourceType type, boolean hasWriteAccess) {
        super(tableTitle, null, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });

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

    @Override
    protected ListGrid createListGrid() {
        return new DriftDefinitionTemplatesListGrid();
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

        addTableAction(MSG.common_button_new(), ButtonColor.BLUE, new AbstractTableAction(addEnablement) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                add();
            }
        });

        addTableAction(MSG.common_button_delete(), MSG.view_drift_confirm_deleteTemplate(), ButtonColor.RED, new AbstractTableAction(
            deleteEnablement) {

            boolean result = false;

            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                if (super.isEnabled(selection)) {
                    for (ListGridRecord record : selection) {
                        if (!record.getAttributeAsBoolean(DriftDefinitionTemplateDataSource.ATTR_IS_USER_DEFINED)
                            .booleanValue()) {
                            return false;
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

    private void delete(final ListGridRecord[] records) {
        int[] templateIds = new int[records.length];
        for (int i = 0; i < records.length; ++i) {
            templateIds[i] = Integer.parseInt(records[i].getAttribute(DriftDefinitionTemplateDataSource.ATTR_ID));
        }
        GWTServiceLookup.getDriftService().deleteDriftDefinitionTemplates(templateIds, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_deleteTemplates(), caught);
                DriftDefinitionTemplatesView.this.refresh();
            }

            @Override
            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_drift_success_deleteTemplate(Integer.toString(records.length)),
                        Message.Severity.Info));
                DriftDefinitionTemplatesView.this.refresh();
            }
        });
    }

    @Override
    public void renderView(ViewPath viewPath) {
        // we have two detail views for drift def templates, the config editor and the pinned snapshot. Figure out which
        // one we're dealing with. The default is the editor. 
        if (!viewPath.isEnd()) {
            this.useSnapshotDetailsView = !viewPath.isNextEnd() && "Snapshot".equals(viewPath.getNext().getPath());
            snapshotDriftDetailsId = null;
            if (viewPath.viewsLeft() > 1) {
                snapshotDriftDetailsId = viewPath.getViewForIndex(viewPath.getCurrentIndex() + 2).getPath()
                    .substring("0id_".length());
            }
        }

        super.renderView(viewPath);
    }

    @Override
    public Canvas getDetailsView(Integer driftTemplateId) {
        if (this.useSnapshotDetailsView) {
            if (null == snapshotDriftDetailsId) {
                return new DriftDefinitionTemplateSnapshotView(driftTemplateId);
            }
            return new DriftDetailsView(snapshotDriftDetailsId);
        }

        return new DriftDefinitionTemplateEditView(driftTemplateId, hasWriteAccess);
    }

    /**
     * The expandable list grid     
     */
    private class DriftDefinitionTemplatesListGrid extends ListGrid {

        public DriftDefinitionTemplatesListGrid() {
            super();

            setCanExpandRecords(true);
            setCanExpandMultipleRecords(true);
            setExpansionMode(ExpansionMode.RELATED);
        }

        @Override
        protected Canvas getExpansionComponent(ListGridRecord record) {
            int templateId = record.getAttributeAsInt(DriftDefinitionTemplateDataSource.ATTR_ID);
            String templateName = record.getAttribute(DriftDefinitionTemplateDataSource.ATTR_NAME);

            return new TemplateDefinitionsView(templateId);
        }
    }

    /**
     * The expanded row table view of definitions derived from the template
     */
    public class TemplateDefinitionsView extends Table<TemplateDefinitionsView.TemplateDefinitionsDataSource> {
        private int templateId;
        private TemplateDefinitionsDataSource dataSource;

        public TemplateDefinitionsView(int templateId) {
            super(null, true);

            this.templateId = templateId;

            setShowFilterForm(false);
            setShowHeader(false);
            setShowFooter(false);

            setDataSource(getDataSource());
        }

        @Override
        public TemplateDefinitionsDataSource getDataSource() {
            if (null == dataSource) {
                dataSource = new TemplateDefinitionsDataSource();
            }

            return dataSource;
        }

        @Override
        protected void configureTable() {
            ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
            getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));

            super.configureTable();
        }

        @Override
        protected void configureTableContents(Layout contents) {
            contents.setWidth100();
            contents.setHeight100();
            contents.setOverflow(Overflow.VISIBLE);
        }

        @Override
        protected ListGrid createListGrid() {
            return new TemplateDefinitionsListGrid();
        }

        @Override
        protected void configureListGrid(ListGrid grid) {
            grid.setDefaultHeight(1);
            grid.setAutoFitData(Autofit.VERTICAL);
        }

        /**
         * The definitions list grid     
         */
        private class TemplateDefinitionsListGrid extends ListGrid {

            public TemplateDefinitionsListGrid() {
                super();
            }
        }

        public class TemplateDefinitionsDataSource extends RPCDataSource<DriftDefinition, DriftDefinitionCriteria> {

            public TemplateDefinitionsDataSource() {
                addDataSourceFields();
            }

            public ArrayList<ListGridField> getListGridFields() {
                ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

                ListGridField nameField = new ListGridField(DriftDefinitionDataSource.ATTR_NAME,
                    MSG.common_title_name());
                nameField.setCellFormatter(new CellFormatter() {
                    public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                        Integer resourceId = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                        Integer driftDefId = listGridRecord.getAttributeAsInt("id");
                        String url = LinkManager.getDriftDefinitionCarouselLink(resourceId, driftDefId);
                        return LinkManager.getHref(url, o.toString());
                    }
                });
                fields.add(nameField);

                ListGridField descriptionField = new ListGridField(DriftDefinitionDataSource.ATTR_DESCRIPTION,
                    MSG.common_title_description());
                fields.add(descriptionField);

                ListGridField attachedField = new ListGridField(DriftDefinitionDataSource.ATTR_ATTACHED,
                    MSG.view_drift_table_attached());
                fields.add(attachedField);

                ListGridField enabledField = new ListGridField(DriftDefinitionDataSource.ATTR_IS_ENABLED_ICON,
                    MSG.common_title_enabled());
                enabledField.setType(ListGridFieldType.IMAGE);
                enabledField.setAlign(Alignment.CENTER);
                fields.add(enabledField);

                ListGridField inComplianceField = new ListGridField(DriftDefinitionDataSource.ATTR_COMPLIANCE_ICON,
                    MSG.common_title_in_compliance());
                inComplianceField.setType(ListGridFieldType.IMAGE);
                inComplianceField.setAlign(Alignment.CENTER);
                inComplianceField.setShowHover(true);
                inComplianceField.setHoverCustomizer(new HoverCustomizer() {
                    @Override
                    public String hoverHTML(Object o, ListGridRecord record, int row, int column) {
                        int complianceCode = record.getAttributeAsInt(DriftDefinitionDataSource.ATTR_COMPLIANCE);
                        DriftComplianceStatus complianceStatus = DriftComplianceStatus.fromCode(complianceCode);
                        switch (complianceStatus) {
                        case OUT_OF_COMPLIANCE_NO_BASEDIR:
                            return MSG.view_drift_table_hover_outOfCompliance_noBaseDir();
                        case OUT_OF_COMPLIANCE_DRIFT:
                            return MSG.view_drift_table_hover_outOfCompliance_drift();
                        default:
                            return "";
                        }
                    }
                });
                fields.add(inComplianceField);

                ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME,
                    MSG.common_title_resource());
                resourceNameField.setCellFormatter(new CellFormatter() {
                    public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                        Integer resourceId = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                        String url = LinkManager.getResourceLink(resourceId);
                        return LinkManager.getHref(url, o.toString());
                    }
                });
                resourceNameField.setShowHover(true);
                resourceNameField.setHoverCustomizer(new HoverCustomizer() {
                    public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                        return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                    }
                });
                fields.add(resourceNameField);

                ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
                fields.add(ancestryField);

                nameField.setWidth("15%");
                descriptionField.setWidth("25%");
                attachedField.setWidth(70);
                enabledField.setWidth(60);
                inComplianceField.setWidth(100);
                resourceNameField.setWidth("20%");
                ancestryField.setWidth("*");

                return fields;
            }

            @Override
            protected void executeFetch(final DSRequest request, final DSResponse response,
                DriftDefinitionCriteria criteria) {

                GWTServiceLookup.getDriftService().findDriftDefinitionsByCriteria(criteria,
                    new AsyncCallback<PageList<DriftDefinition>>() {

                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                            response.setStatus(RPCResponse.STATUS_FAILURE);
                            processResponse(request.getRequestId(), response);
                        }

                        public void onSuccess(final PageList<DriftDefinition> result) {
                            response.setData(buildRecords(result));
                            response.setTotalRows(result.getTotalSize());
                            processResponse(request.getRequestId(), response);
                        }
                    });
            }

            @Override
            protected DriftDefinitionCriteria getFetchCriteria(DSRequest request) {

                DriftDefinitionCriteria criteria = new DriftDefinitionCriteria();
                criteria.addFilterTemplateId(templateId);
                criteria.fetchResource(true);

                return criteria;
            }

            @Override
            public DriftDefinition copyValues(Record from) {
                return null;
            }

            @Override
            public ListGridRecord copyValues(DriftDefinition from) {
                ListGridRecord record = new ListGridRecord();

                record.setAttribute(DriftDefinitionDataSource.ATTR_ID, from.getId());

                record.setAttribute(DriftDefinitionDataSource.ATTR_NAME, from.getName());
                record.setAttribute(DriftDefinitionDataSource.ATTR_DESCRIPTION, from.getDescription());

                record.setAttribute(DriftDefinitionDataSource.ATTR_IS_ENABLED, String.valueOf(from.isEnabled()));
                record.setAttribute(DriftDefinitionDataSource.ATTR_IS_ENABLED_ICON,
                    ImageManager.getAvailabilityIcon(from.isEnabled()));
                record.setAttribute(DriftDefinitionDataSource.ATTR_COMPLIANCE, from.getComplianceStatus().ordinal());
                record
                    .setAttribute(DriftDefinitionDataSource.ATTR_COMPLIANCE_ICON, ImageManager.getAvailabilityIcon(from
                        .getComplianceStatus() == DriftComplianceStatus.IN_COMPLIANCE));

                record.setAttribute(DriftDefinitionDataSource.ATTR_ATTACHED, from.isAttached() ? MSG.common_val_yes()
                    : MSG.common_val_no());

                // for ancestry handling
                Resource resource = from.getResource();
                record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
                record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
                record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
                record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

                return record;
            }
        }
    }

}
