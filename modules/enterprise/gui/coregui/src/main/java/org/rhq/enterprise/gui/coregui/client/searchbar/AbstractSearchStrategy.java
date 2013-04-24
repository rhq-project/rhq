/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.searchbar;

import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SearchGWTServiceAsync;

/**
 * AbstractSearchStrategy defines common search strategy behaviors the subclasses must implement.
 * Also, houses common functionality such as user auth and searching the domain.
 *
 * @author Mike Thompson
 */
public abstract class AbstractSearchStrategy implements RecordClickHandler, RecordDoubleClickHandler, CellFormatter {
    protected static final Messages MSG = CoreGUI.getMessages();
    public static final String ATTR_ID = "id";
    public static final String ATTR_KIND = "kind"; // not for saved search
    public static final String ATTR_NAME = "name";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_RESULT_COUNT = "resultCount";
    public static final String ATTR_PATTERN = "pattern";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_OPTIONAL = "value";

    protected final SearchGWTServiceAsync searchService = GWTServiceLookup.getSearchService();

    final Subject subject;
    protected final EnhancedSearchBar searchBar;

    public AbstractSearchStrategy(EnhancedSearchBar searchBar) {
        this.searchBar = searchBar;
        subject = UserSessionManager.getSessionSubject();
    }

    /**
     * Handle the key press event in the search bar. Must be overridden in subclass.
     * @param keyUpEvent
     */
    public abstract void searchKeyUpHandler(KeyUpEvent keyUpEvent);

    /**
     * Handle the focus event in the search bar. Must be overridden in subclass.
     */
    public abstract void searchFocusHandler();

    /**
     * Search results can have different heights (i.e. 1 row or 2 rows). Must be overridden
     * in subclass.
     * @return height in pixels
     */
    public abstract int getCellHeight();

    /**
     * When return key is pressed in the search bar do I want to do any further
     * customization. Optionally, overridden by subclass.
     * @param keyUpEvent
     */
    public void searchReturnKeyHandler(KeyUpEvent keyUpEvent) {
        // do nothing by default
    }

    @Override
    public void onRecordDoubleClick(RecordDoubleClickEvent event) {
        // do nothing by default
    }

}
