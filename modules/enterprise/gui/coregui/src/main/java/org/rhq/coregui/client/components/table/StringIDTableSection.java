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

package org.rhq.coregui.client.components.table;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * The TableSection abstract implementation that supports IDs as basic Strings.
 * 
 * Use this if you have tabular data whose rows are not identified with Integers but
 * some other non-numeric string.
 * 
 * If you have tabular data whose rows have integer IDs, use {@link TableSection}.
 *
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public abstract class StringIDTableSection<DS extends RPCDataSource> extends AbstractTableSection<DS, String> {

    public StringIDTableSection(String tableTitle, boolean autoFetchData) {
        super(tableTitle, autoFetchData);
    }

    public StringIDTableSection(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames, boolean autoFetchData) {
        super(tableTitle, criteria, sortSpecifiers, excludedFieldNames, autoFetchData);
    }

    public StringIDTableSection(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        super(tableTitle, criteria, sortSpecifiers, excludedFieldNames);
    }

    public StringIDTableSection(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers) {
        super(tableTitle, criteria, sortSpecifiers);
    }

    public StringIDTableSection(String tableTitle, Criteria criteria) {
        super(tableTitle, criteria);
    }

    public StringIDTableSection(String tableTitle, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames) {
        super(tableTitle, sortSpecifiers, excludedFieldNames);
    }

    public StringIDTableSection(String tableTitle, SortSpecifier[] sortSpecifiers) {
        super(tableTitle, sortSpecifiers);
    }

    public StringIDTableSection(String tableTitle) {
        super(tableTitle);
    }

    @Override
    protected String getId(ListGridRecord record) {
        String id = null;
        if (record != null) {
            id = record.getAttribute("id");
        }

        if (id == null || id.length() == 0) {
            String msg = MSG.view_tableSection_error_noId(this.getClass().toString());
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalStateException(msg);
        }
        return id;
    }

    @Override
    public void showDetails(String id) {
        if (id != null && id.length() > 0) {
            CoreGUI.goToView(getBasePath() + "/" + convertIDToCurrentViewPath(id));
        } else {
            String msg = MSG.view_tableSection_error_badId(this.getClass().toString(),
                (id == null) ? "null" : id.toString());
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public abstract Canvas getDetailsView(String id);

    // the main CoreGUI class will assume anything with a digit as the first character in a path segment in the URL is an ID.
    public static final String ID_PREFIX = "0id_"; // the prefix to be placed in front of the string IDs in URLs

    @Override
    protected String convertCurrentViewPathToID(String path) {
        if (!path.startsWith(ID_PREFIX)) {
            return path; // prefixed has already been stripped
        }
        return path.substring(ID_PREFIX.length()); // skip the initial "0id_" - see convertIDToCurrentViewPath for what this is all about
    }

    @Override
    protected String convertIDToCurrentViewPath(String id) {
        // Because we aren't assured the given ID will be a digit, let's prepend the digit here and make it 
        // look like an ID to CoreGUI. We will strip this off when we convert this back to an ID - see convertCurrentViewPathToID
        if (id.startsWith(ID_PREFIX)) {
            return id; // it is already prefixed
        }
        return ID_PREFIX + id;
    }
}
