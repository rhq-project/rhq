/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client.help;

import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.FullHTMLPane;
import org.rhq.coregui.client.components.view.ViewName;

public class RhAccessView extends FullHTMLPane implements BookmarkableView {

    public RhAccessView(String url) {
        super(url);
    }

    public RhAccessView() {
        this("/rha/support.html#/case/search");
    }

    public static final ViewName VIEW_ID = new ViewName("Support", "Red Hat Access");

    public static final ViewName PAGE_SEARCH = new ViewName("Search", "Search");
    public static final ViewName PAGE_MY_CASES = new ViewName("MyCases", "My Cases");
    public static final ViewName PAGE_NEW_CASE = new ViewName("NewCase", "Open Case");

    @Override
    public void renderView(ViewPath viewPath) {
        if (viewPath.isEnd()) {
            return;
        }
        String viewId = viewPath.getCurrent().getPath();
        if (PAGE_SEARCH.getName().equals(viewId)) {
            setContentsURL("/rha/support.html#/case/search");
        }
        else if (PAGE_MY_CASES.getName().equals(viewId)) {
            setContentsURL("/rha/support.html#/case/list");
        }
        else if (PAGE_NEW_CASE.getName().equals(viewId)) {
            setContentsURL("/rha/support.html#/case/new");
        }
    }

}
