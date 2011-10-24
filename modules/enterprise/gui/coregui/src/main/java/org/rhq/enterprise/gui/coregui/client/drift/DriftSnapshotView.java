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
import java.util.Collection;
import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.StringIDTableSection;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.drift.wizard.DriftPinTemplateWizard;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A view that displays a nested table view of a snapshot. The outer table is a list of directories in the snapshot
 * and the inner table displays the files for that directory. Data is fetched as needed to avoid large fetched for
 * snapshots involving potentially thousands of files.
 *
 * @author Jay Shaughnessy
 */
public class DriftSnapshotView extends Table<DriftSnapshotDataSource> {

    private static final String DEFAULT_TITLE = MSG.view_drift_table_snapshot();

    private Integer templateId;
    private Integer driftDefId;
    private Integer resourceId;
    private Integer version;
    private boolean hasWriteAccess;

    private String templateChangeSetId;

    protected DriftSnapshotDataSource dataSource;
    protected DriftSnapshotListGrid listGrid;

    public DriftSnapshotView(String locatorId, String tableTitle, int templateId, boolean hasWriteAccess) {
        this(locatorId, (null == tableTitle) ? DEFAULT_TITLE : tableTitle, templateId, null, null, null, hasWriteAccess);
    }

    public DriftSnapshotView(String locatorId, String tableTitle, int resourceId, int driftDefId, int version,
        boolean hasWriteAccess) {
        this(locatorId, (null == tableTitle) ? DEFAULT_TITLE : tableTitle, null, resourceId, driftDefId, version,
            hasWriteAccess);
    }

    private DriftSnapshotView(String locatorId, String tableTitle, Integer templateId, Integer resourceId,
        Integer driftDefId, Integer version, boolean hasWriteAccess) {

        super(locatorId, tableTitle);

        this.templateId = templateId;
        this.resourceId = resourceId;
        this.driftDefId = driftDefId;
        this.version = version;
        this.hasWriteAccess = hasWriteAccess;

        setDataSource(getDataSource());
    }

    @Override
    public DriftSnapshotDataSource getDataSource() {
        if (null == dataSource) {
            if (null != templateId) {
                dataSource = new DriftSnapshotDataSource(templateId);
            } else {
                dataSource = new DriftSnapshotDataSource(driftDefId, version);
            }
        }

        return dataSource;
    }

    @Override
    protected LocatableListGrid createListGrid(String locatorId) {
        return new DriftSnapshotListGrid(locatorId);
    }

    @Override
    protected void onDraw() {

        // Drift def snapshot view
        if (null != this.driftDefId) {
            DriftDefinitionCriteria defCriteria = new DriftDefinitionCriteria();
            defCriteria.addFilterId(driftDefId);
            defCriteria.fetchConfiguration(true);

            DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();
            driftService.findDriftDefinitionsByCriteria(defCriteria, new AsyncCallback<PageList<DriftDefinition>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                }

                public void onSuccess(PageList<DriftDefinition> result) {
                    DriftDefinition driftDef = result.get(0);
                    String defName = driftDef.getName();
                    String title;
                    if (0 == version) {
                        String isPinned = String.valueOf(driftDef.isPinned());
                        title = MSG.view_drift_table_title_initialSnapshot(defName, isPinned);
                    } else {
                        title = MSG.view_drift_table_title_snapshot(String.valueOf(version), defName);
                    }
                    setTitleString(title);
                    DriftSnapshotView.super.onDraw();
                }
            });
        } else {
            DriftDefinitionTemplateCriteria templateCriteria = new DriftDefinitionTemplateCriteria();
            templateCriteria.addFilterId(templateId);

            DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();
            driftService.findDriftDefinitionTemplatesByCriteria(templateCriteria,
                new AsyncCallback<PageList<DriftDefinitionTemplate>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                    }

                    public void onSuccess(PageList<DriftDefinitionTemplate> result) {
                        DriftDefinitionTemplate template = result.get(0);
                        String templateName = template.getName();
                        String title = MSG.view_drift_table_title_templateSnapshot(templateName);
                        setTitleString(title);
                        DriftSnapshotView.super.onDraw();
                    }
                });
        }
    }

    @Override
    protected void configureTable() {
        ListGrid grid = getListGrid();

        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        grid.setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));

        setupTableInteractions(this.hasWriteAccess);

        super.configureTable();
    }

    protected void setupTableInteractions(final boolean hasWriteAccess) {
        TableActionEnablement pinEnablement = hasWriteAccess ? TableActionEnablement.ALWAYS
            : TableActionEnablement.NEVER;

        addTableAction("PinToDef", MSG.view_drift_button_pinToDef(), MSG.view_drift_button_pinToDef_confirm(),
            new AbstractTableAction(pinEnablement) {

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    pinToDefinition();
                }
            });

        addTableAction("PinToTemplate", MSG.view_drift_button_pinToTemplate(), MSG
            .view_drift_button_pinToTemplate_confirm(), new AbstractTableAction(pinEnablement) {

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                pinToTemplate();
            }
        });
    }

    private void pinToDefinition() {
        GWTServiceLookup.getDriftService().pinSnapshot(driftDefId, version, new AsyncCallback<Void>() {
            public void onSuccess(Void x) {

                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_drift_success_pinToDef(String.valueOf(version)), Message.Severity.Info));
                CoreGUI.goToView(LinkManager.getDriftDefinitionsLink(resourceId));
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_pinToDef(), caught);
            }
        });
    }

    private void pinToTemplate() {
        DriftPinTemplateWizard.showWizard(driftDefId, version, DriftSnapshotView.this);
        // we can refresh the table buttons immediately since the wizard is a dialog, the
        // user can't access enabled buttons anyway.
        DriftSnapshotView.this.refreshTableInfo();
    }

    private class DriftSnapshotListGrid extends LocatableListGrid {

        public DriftSnapshotListGrid(String locatorId) {
            super(locatorId);

            setCanExpandRecords(true);
            setCanExpandMultipleRecords(true);
            setExpansionMode(ExpansionMode.RELATED);
        }

        @Override
        protected Canvas getExpansionComponent(ListGridRecord record) {

            String dirPath = record.getAttribute(DriftSnapshotDataSource.ATTR_DIR_PATH);

            return new DirectoryView(extendLocatorId(dirPath), dirPath);
        }
    }

    public class DirectoryView extends StringIDTableSection<DirectoryView.DirectoryViewDataSource> {

        private String directory;
        private DirectoryViewDataSource dataSource;

        public DirectoryView(String locatorId, String directory) {
            super(locatorId, null, true);

            this.directory = directory;

            setShowFilterForm(false);
            setShowHeader(false);
            setShowFooter(false);

            setWidth100();
            setHeight(250);

            setDataSource(getDataSource());
        }

        @Override
        public DirectoryViewDataSource getDataSource() {

            if (null == dataSource) {
                dataSource = new DirectoryViewDataSource();
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
        protected String getDetailsLinkColumnName() {
            return DriftDataSource.ATTR_CTIME;
        }

        @Override
        protected CellFormatter getDetailsLinkColumnCellFormatter() {
            // TODO: provide a drift detail view for the template snapshot view                    
            if (null == templateId) {
                return new CellFormatter() {
                    public String format(Object value, ListGridRecord record, int i, int i1) {
                        String driftId = getId(record);
                        String url = LinkManager.getDriftHistoryLink(resourceId, driftDefId, driftId);
                        String formattedValue = TimestampCellFormatter.format(value);
                        return SeleniumUtility.getLocatableHref(url, formattedValue, null);
                    }
                };
            } else {
                return new CellFormatter() {
                    public String format(Object value, ListGridRecord record, int i, int i1) {
                        return TimestampCellFormatter.format(value);
                    }
                };
            }
        }

        public class DirectoryViewDataSource extends RPCDataSource<Drift<?, ?>, GenericDriftChangeSetCriteria> {

            public DirectoryViewDataSource() {
                addDataSourceFields();
            }

            /**
             * The view that contains the list grid which will display this datasource's data will call this
             * method to get the field information which is used to control the display of the data.
             * 
             * @return list grid fields used to display the datasource data
             */
            public ArrayList<ListGridField> getListGridFields() {
                ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

                ListGridField ctimeField = new ListGridField(DriftDataSource.ATTR_CTIME, MSG.common_title_createTime());
                ctimeField.setCellFormatter(new TimestampCellFormatter());
                ctimeField.setShowHover(true);
                ctimeField.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer(DriftDataSource.ATTR_CTIME));
                fields.add(ctimeField);

                ListGridField categoryField = new ListGridField(DriftDataSource.ATTR_CATEGORY, MSG
                    .common_title_category());
                categoryField.setType(ListGridFieldType.IMAGE);
                categoryField.setAlign(Alignment.CENTER);
                categoryField.setShowHover(true);
                categoryField.setHoverCustomizer(new HoverCustomizer() {
                    @Override
                    public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                        String cat = record.getAttribute(DriftDataSource.ATTR_CATEGORY);
                        if (DriftDataSource.CATEGORY_ICON_ADD.equals(cat)) {
                            return MSG.view_drift_category_fileAdded();
                        } else if (DriftDataSource.CATEGORY_ICON_CHANGE.equals(cat)) {
                            return MSG.view_drift_category_fileChanged();
                        } else if (DriftDataSource.CATEGORY_ICON_REMOVE.equals(cat)) {
                            return MSG.view_drift_category_fileRemoved();
                        } else if (DriftDataSource.CATEGORY_ICON_NEW.equals(cat)) {
                            return MSG.view_drift_category_fileNew();
                        } else {
                            return ""; // will never get here
                        }
                    }
                });
                fields.add(categoryField);

                ListGridField changeSetVersionField = new ListGridField(DriftDataSource.ATTR_CHANGESET_VERSION, MSG
                    .view_drift_table_snapshot());
                fields.add(changeSetVersionField);

                ListGridField pathField = new ListGridField(DriftDataSource.ATTR_PATH, MSG.common_title_name());
                fields.add(pathField);

                ctimeField.setWidth(200);
                categoryField.setWidth(100);
                changeSetVersionField.setWidth(100);
                pathField.setWidth("*");

                return fields;
            }

            @Override
            protected void executeFetch(final DSRequest request, final DSResponse response,
                GenericDriftChangeSetCriteria criteria) {

                if (null == templateId) {
                    DriftSnapshotRequest snapshotRequest = new DriftSnapshotRequest(driftDefId, version, null,
                        directory, false, true);
                    executeGetSnapshot(request, response, snapshotRequest);

                } else {
                    if (null == templateChangeSetId) {

                        DriftDefinitionTemplateCriteria templateCriteria = new DriftDefinitionTemplateCriteria();
                        templateCriteria.addFilterId(templateId);

                        GWTServiceLookup.getDriftService().findDriftDefinitionTemplatesByCriteria(templateCriteria,
                            new AsyncCallback<PageList<DriftDefinitionTemplate>>() {

                                public void onSuccess(final PageList<DriftDefinitionTemplate> result) {
                                    templateChangeSetId = String.valueOf(result.get(0).getChangeSetId());
                                    DriftSnapshotRequest snapshotRequest = new DriftSnapshotRequest(
                                        templateChangeSetId, directory, false, true);
                                    executeGetSnapshot(request, response, snapshotRequest);
                                }

                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError("Failed to load definition.", caught);
                                }
                            });
                    } else {
                        DriftSnapshotRequest snapshotRequest = new DriftSnapshotRequest(templateChangeSetId, directory,
                            false, true);
                        executeGetSnapshot(request, response, snapshotRequest);
                    }
                }
            }

            private void executeGetSnapshot(final DSRequest request, final DSResponse response,
                DriftSnapshotRequest snapshotRequest) {

                DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();

                driftService.getSnapshot(snapshotRequest, new AsyncCallback<DriftSnapshot>() {

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                        response.setStatus(RPCResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }

                    public void onSuccess(DriftSnapshot result) {
                        Collection<Drift<?, ?>> drifts = result.getDriftInstances();
                        ListGridRecord[] records = buildRecords(drifts);
                        response.setData(records);
                        response.setTotalRows(drifts.size());
                        processResponse(request.getRequestId(), response);
                    }
                });
            }

            @Override
            protected GenericDriftChangeSetCriteria getFetchCriteria(DSRequest request) {
                return null;
            }

            @Override
            public Drift<?, ?> copyValues(Record from) {
                return null;
            }

            @Override
            public ListGridRecord copyValues(Drift<?, ?> from) {
                ListGridRecord record = new ListGridRecord();

                record.setAttribute(DriftDataSource.ATTR_ID, from.getId());

                record.setAttribute(DriftDataSource.ATTR_CTIME, new Date(from.getCtime()));

                switch (from.getChangeSet().getCategory()) {
                case COVERAGE:
                    record.setAttribute(DriftDataSource.ATTR_CATEGORY, ImageManager.getDriftCategoryIcon(null));
                    break;
                case DRIFT:
                    record.setAttribute(DriftDataSource.ATTR_CATEGORY, ImageManager.getDriftCategoryIcon(from
                        .getCategory()));
                    break;
                }

                record.setAttribute(DriftDataSource.ATTR_CHANGESET_VERSION, (null == from.getChangeSet()) ? "0" : from
                    .getChangeSet().getVersion());

                record.setAttribute(DriftDataSource.ATTR_PATH, getFileName(from.getPath(), "/"));

                return record;
            }
        }

        @Override
        public Canvas getDetailsView(String driftId) {
            return new DriftDetailsView(extendLocatorId("Details"), driftId);
        }
    }

    /**
     * Return just the filename portion (the portion right of the last path separator string)
     * @param path
     * @param separator
     * @return null if path is null, otherwise the trimmed filename  
     */
    public static String getFileName(String path, String separator) {
        if (null == path) {
            return null;
        }

        int i = path.lastIndexOf(separator);

        return (i < 0) ? path.trim() : path.substring(++i).trim();
    }

}
