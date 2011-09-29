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

package org.rhq.enterprise.gui.coregui.client.components.table;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * The TableSection abstract implementation that supports IDs as Integers.
 * 
 * Since most master/detail table views have Integers for IDs that uniquely identify
 * rows in the table, this is the typical superclass implementation used for
 * most of RHQ's concrete TableSection views. 
 * 
 * @author John Mazzitelli
 */
public abstract class TableSection<DS extends RPCDataSource> extends AbstractTableSection<DS, Integer> {

    public TableSection(String locatorId, String tableTitle, boolean autoFetchData) {
        super(locatorId, tableTitle, autoFetchData);
    }

    public TableSection(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames, boolean autoFetchData) {
        super(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames, autoFetchData);
    }

    public TableSection(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        super(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames);
    }

    public TableSection(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers) {
        super(locatorId, tableTitle, criteria, sortSpecifiers);
    }

    public TableSection(String locatorId, String tableTitle, Criteria criteria) {
        super(locatorId, tableTitle, criteria);
    }

    public TableSection(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames) {
        super(locatorId, tableTitle, sortSpecifiers, excludedFieldNames);
    }

    public TableSection(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers) {
        super(locatorId, tableTitle, sortSpecifiers);
    }

    public TableSection(String locatorId, String tableTitle) {
        super(locatorId, tableTitle);
    }

    @Override
    protected Integer getId(ListGridRecord record) {
        Integer id = (record != null) ? record.getAttributeAsInt("id") : 0;
        if (id == null) {
            String msg = MSG.view_tableSection_error_noId(this.getClass().toString());
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalStateException(msg);
        }
        return id;
    }

    @Override
    public void showDetails(Integer id) {
        if (id != null && id.intValue() > 0) {
            CoreGUI.goToView(getBasePath() + "/" + id);
        } else {
            String msg = MSG.view_tableSection_error_badId(this.getClass().toString(), (id == null) ? "null" : id
                .toString());
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public abstract Canvas getDetailsView(Integer id);

    @Override
    protected Integer convertCurrentViewPathToID(String path) {
        return Integer.valueOf(path);
    }

    @Override
    protected String convertIDToCurrentViewPath(Integer id) {
        if (id == null) {
            return "0";
        }
        return id.toString();
    }
}
