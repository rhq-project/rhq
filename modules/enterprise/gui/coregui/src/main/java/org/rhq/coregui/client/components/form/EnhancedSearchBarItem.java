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
package org.rhq.coregui.client.components.form;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.CanvasItem;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.coregui.client.searchbar.EnhancedSearchBar;

/**
 * Wrap the SearchBar component in CanvasItem so it can be put into a DynamicForm.
 * @author Mike Thompson
 */
public class EnhancedSearchBarItem extends CanvasItem {

    private EnhancedSearchBar searchBar;

    public EnhancedSearchBarItem(String name, SearchSubsystem subsystem, String initialSearchText) {
        super(name, null);
        setShowTitle(false);
        searchBar = new EnhancedSearchBar(subsystem, initialSearchText);
        setCanvas(searchBar);
    }

    @Override
    public Canvas getCanvas() {
        return searchBar;
    }

    public EnhancedSearchBar getSearchBar() {
        return searchBar;
    }
}
