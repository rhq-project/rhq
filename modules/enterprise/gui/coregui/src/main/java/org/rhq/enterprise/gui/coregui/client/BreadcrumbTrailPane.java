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
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;

/**
 * GWT widget for the breadcrumb trail, which is displayed at the top of each page.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class BreadcrumbTrailPane extends ToolStrip {

    public BreadcrumbTrailPane() {
        super();
        setHeight(28);
        setAlign(VerticalAlignment.CENTER);
        setWidth100();
        setMembersMargin(15);

        setOverflow(Overflow.HIDDEN);

    }

    public void refresh(ViewPath viewPath) {
        try {
            // before rebuilding the trail, remove the current members, and destroy them to avoid ID conflicts
            for (Canvas currentMember : getMembers()) {
                removeMember(currentMember);
                currentMember.destroy();
            }

            LayoutSpacer ls = new LayoutSpacer();
            ls.setWidth(5);
            addMember(ls);

            boolean first = true;
            StringBuilder path = new StringBuilder();

            for (ViewId viewId : viewPath.getViewPath()) {

                if (!first && !viewId.getBreadcrumbs().isEmpty()) {
                    addMember(getSpacer());
                }

                if (!first) {
                    path.append("/");
                } else {
                    first = false;
                }

                boolean firstBC = true;
                for (Breadcrumb breadcrumb : viewId.getBreadcrumbs()) {

                    if (!firstBC) {
                        addMember(getSpacer());
                    } else {
                        firstBC = false;
                    }

                    addMember(getCrumb(breadcrumb, path.toString()));
                }
                path.append(viewId.getPath());
            }

            addMember(new LayoutSpacer());

        } catch (Throwable t) {
            System.err.println("Failed to refresh bread crumb HTML - cause: " + t);
        }

        String title = "RHQ";
        if (!viewPath.getViewPath().isEmpty()) {
            title += ": "
                + viewPath.getViewPath().get(viewPath.getViewPath().size() - 1).getBreadcrumbs().get(0)
                    .getDisplayName();

        }
        Window.setTitle(title);

        redraw();
    }

    private Label getCrumb(Breadcrumb crumb, String path) {
        Label l = null;
        if (crumb.isHyperlink()) {
            l = new LocatableLabel(crumb.getName(), "<a href=\"#" + path.toString() + crumb.getName() + "\">"
                + crumb.getDisplayName() + "</a>");
        } else {
            l = new Label(crumb.getDisplayName());
        }
        if (crumb.getIcon() != null) {
            l.setIcon(crumb.getIcon());
            l.setIconSize(16);
        }
        l.setValign(VerticalAlignment.CENTER);
        l.setWrap(false);
        l.setAutoWidth();
        return l;
    }

    private Img getSpacer() {
        return new Img("header/breadcrumb_space.png", 28, 28);
    }
}
