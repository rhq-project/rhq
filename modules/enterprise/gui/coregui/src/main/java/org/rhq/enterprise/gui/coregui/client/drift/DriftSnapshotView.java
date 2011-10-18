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

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.drift.DriftSnapshotRequest;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.StringIDTableSection;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
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

    private int resourceId;
    private int driftDefId;
    private int version;

    //private EntityContext context;
    private boolean hasWriteAccess = true;

    protected DriftSnapshotDataSource dataSource;
    protected DriftSnapshotListGrid listGrid;

    public DriftSnapshotView(String locatorId, int resourceId, int driftDefId, int version) {
        this(locatorId, DEFAULT_TITLE, resourceId, driftDefId, version);
    }

    public DriftSnapshotView(String locatorId, String tableTitle, int resourceId, int driftDefId, int version) {
        super(locatorId, tableTitle);
        this.resourceId = resourceId;
        this.driftDefId = driftDefId;
        this.version = version;

        setDataSource(getDataSource());
    }

    protected int getDriftDefId() {
        return driftDefId;
    }

    protected void setDriftDefId(int driftDefId) {
        this.driftDefId = driftDefId;
    }

    protected int getVersion() {
        return version;
    }

    protected void setVersion(int version) {
        this.version = version;
    }

    @Override
    public DriftSnapshotDataSource getDataSource() {
        if (null == dataSource) {
            dataSource = new DriftSnapshotDataSource(driftDefId, version);
        }

        return dataSource;
    }

    @Override
    protected LocatableListGrid createListGrid(String locatorId) {
        return new DriftSnapshotListGrid(locatorId);
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
        // TODO
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

            return new DirectoryView(extendLocatorId(dirPath), EntityContext.forResource(resourceId), driftDefId,
                version, dirPath, hasWriteAccess);
        }
    }

    public static class DirectoryView extends StringIDTableSection<DirectoryView.DirectoryViewDataSource> {

        private int driftDefId;
        private String directory;
        private int version;
        private EntityContext context;
        private DirectoryViewDataSource dataSource;

        public DirectoryView(String locatorId, EntityContext context, int driftDefId, int version, String directory,
            boolean hasWriteAccess) {
            super(locatorId, null, true);

            this.driftDefId = driftDefId;
            this.directory = directory;
            this.version = version;
            this.context = context;

            setShowFilterForm(false);
            setShowHeader(false);
            setShowFooter(false);

            setWidth100();
            setHeight(250);

            setDataSource(getDataSource());
        }

        @Override
        public DirectoryViewDataSource getDataSource() {

            if (null == this.dataSource) {
                this.dataSource = new DirectoryViewDataSource();
            }

            return this.dataSource;
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
            return new CellFormatter() {
                public String format(Object value, ListGridRecord record, int i, int i1) {
                    Integer resourceId = context.getResourceId();
                    String driftId = getId(record);
                    String url = LinkManager.getDriftHistoryLink(resourceId, driftDefId, driftId);
                    String formattedValue = TimestampCellFormatter.format(value);
                    return SeleniumUtility.getLocatableHref(url, formattedValue, null);
                }
            };
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
                DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();
                DriftSnapshotRequest snapshotRequest = new DriftSnapshotRequest(driftDefId, version, null, directory,
                    false, true);

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
