/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.form;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.CanvasItem;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.search.FlexSearchBar;

/**
 * @author Joseph Marques
 */
public class SearchBarItem extends CanvasItem {

    private Canvas canvas = new Canvas();
    private FlexSearchBar searchBar;

    public SearchBarItem(String name, String title, SearchSubsystem subsystem) {
        super(name, title);

        searchBar = new FlexSearchBar();
        searchBar.setSearchSubsystem(subsystem);

        searchBar.setHeight("30px");
        canvas.addChild(searchBar);

        setCanvas(canvas);
        setHeight(30);
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    public FlexSearchBar getSearchBar() {
        return searchBar;
    }

}
