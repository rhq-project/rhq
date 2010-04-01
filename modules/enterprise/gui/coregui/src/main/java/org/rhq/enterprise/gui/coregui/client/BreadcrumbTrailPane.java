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
package org.rhq.enterprise.gui.coregui.client;

import com.google.gwt.user.client.Window;
import com.smartgwt.client.types.BkgndRepeat;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.HTMLPane;

import java.util.ArrayList;
import java.util.List;

/**
 * GWT widget for the breadcrumb trail, which is displayed at the top of each page.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class BreadcrumbTrailPane extends HTMLPane {

    private List<Breadcrumb> breadcrumbs = new ArrayList<Breadcrumb>();

    public BreadcrumbTrailPane() {
        setHeight(28);
        setBackgroundColor("#E6E3E3");
        setBackgroundImage("[skin]images/SectionHeader/header_opened_stretch.png");   //ToolStrip/background.png");
        setBackgroundRepeat(BkgndRepeat.REPEAT_X);
        setPadding(5);
        setOverflow(Overflow.CLIP_V);
    }

    public List<Breadcrumb> getBreadcrumbs() {
        return this.breadcrumbs;
    }

    public void setBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    public void refresh() {
        try {
            boolean first = true;
            StringBuilder path = new StringBuilder();
            StringBuilder content = new StringBuilder();
            content.append("<div class=\"BreadCrumb\">");
            for (int i = 0, trailSize = this.breadcrumbs.size(); i < trailSize; i++) {
                if (!first) {
                    path.append("/");
                    content.append(" > ");
                } else {
                    first = false;
                }

                Breadcrumb breadcrumb = breadcrumbs.get(i);
                path.append(breadcrumb.getName());

                if ((i == (trailSize - 1) || !breadcrumb.isHyperlink())) {
                    // last item in trail is the current page and so should not be a link
                    content.append(breadcrumb.getDisplayName());
                } else {
                    content.append("<a href=\"#");
                    // NOTE: We have to call toString() below, because GWT chokes if you try to append a StringBuilder.                           
                    content.append(path.toString());
                    content.append("\">");
                    content.append(breadcrumb.getDisplayName());
                    content.append("</a>");
                }
                content.append("\n");
            }
            content.append("</div>");

            setContents(content.toString());
        } catch (Throwable t) {
            System.err.println("Failed to refresh bread crumb HTML - cause: " + t);
        }

        String title = "RHQ";
        if (!breadcrumbs.isEmpty()) {
            title += ": " + this.breadcrumbs.get(this.breadcrumbs.size() - 1);
        }
        Window.setTitle(title);

        redraw();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("BreadCrumb[");
        result.append(this.breadcrumbs);
        result.append("]");
        return result.toString();
    }
}
