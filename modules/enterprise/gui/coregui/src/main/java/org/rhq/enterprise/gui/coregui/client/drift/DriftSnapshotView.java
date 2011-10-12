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

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;

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
    public DriftSnapshotListGrid getListGrid() {
        if (null == listGrid) {
            listGrid = new DriftSnapshotListGrid(extendLocatorId("ListGrid"));
        }

        return listGrid;
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
            //Integer defId = record.getAttributeAsInt(DriftSnapshotDataSource.ATTR_DEF_ID);

            return new DirectoryView(extendLocatorId(dirPath), EntityContext.forResource(resourceId), driftDefId,
                dirPath, hasWriteAccess);
        }
    }

    private static class DirectoryView extends DriftHistoryView {

        private int driftDefId;
        private String dirPath;

        public DirectoryView(String locatorId, EntityContext context, int driftDefId, String dirPath,
            boolean hasWriteAccess) {
            super(locatorId, null, context, hasWriteAccess);

            this.driftDefId = driftDefId;
            this.dirPath = dirPath;

            setShowFilterForm(false);
            setShowHeader(false);
            setShowFooter(false);

            setWidth100();
            setHeight(250);
        }

        @Override
        public DriftDataSource getDataSource() {
            if (null == this.dataSource) {
                this.dataSource = new DirectoryViewDataSource();
            }
            return this.dataSource;
        }

        //        @Override
        //        protected void configureTableContents(Layout contents) {
        //            contents.setWidth100();
        //            contents.setAutoHeight();
        //            contents.setOverflow(Overflow.VISIBLE);
        //        }

        private class DirectoryViewDataSource extends DriftDataSource {

            public DirectoryViewDataSource() {
                super(getContext());
            }

            @Override
            protected GenericDriftCriteria getFetchCriteria(DSRequest request) {

                GenericDriftCriteria criteria = new GenericDriftCriteria();

                criteria.addFilterDriftDefinitionId(driftDefId);
                criteria.addFilterDirectory(dirPath);
                criteria.setStrict(true);

                return criteria;
            }

        }
    }

}
