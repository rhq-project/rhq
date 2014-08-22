/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.coregui.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.widgets.BaseWidget;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * @author Ian Springer
 */
public class WidgetUtility {
    
    private WidgetUtility() {
    }

    public static void printWidgetTree(Widget widget) {
        printWidgetTree(widget, 0);
    }

    private static void printWidgetTree(Widget widget, int level) {
        try {
            printWidget(widget, level);
        } catch (Exception e) {
            System.err.println("=====================================================================================");
            System.err.println("Failed to print widget [" + widget + "]: " + e);
            System.err.println("=====================================================================================");
        }

        // Recurse.
        List<Widget> children;
        try {
            children = getChildren(widget);
        } catch (Exception e) {
            System.err.println("=====================================================================================");
            System.err.println("Failed to get children of widget [" + widget + "]: " + e);
            System.err.println("=====================================================================================");
            return;
        }

        for (Widget child : children) {
            printWidgetTree(child, level + 1);
        }
    }

    private static void printWidget(Widget widget, int level) {
        String simpleClassName;
        try {
            String className = widget.getClass().getName();
            simpleClassName = className.substring(className.lastIndexOf(".") + 1);
        } catch (Exception e) {
            simpleClassName = "?";
        }
        String id;
        try {
            id = (widget.getElement() != null) ? widget.getElement().getId() : "?";
        } catch (Exception e) {
            id = "?";
        }
        String title;
        try {
            title = widget.getTitle();
        } catch (Exception e) {
            title = "?";
        }

        StringBuilder flags = new StringBuilder();

        if (widget.isAttached()) {
            if (flags.length() != 0) {
                flags.append(", ");
            }
            flags.append("attached");
        }
        if (widget.isVisible()) {
            if (flags.length() != 0) {
                flags.append(", ");
            }
            flags.append("visible");
        }

        if (widget instanceof BaseWidget) {
            BaseWidget baseWidget = (BaseWidget) widget;

            if (baseWidget.isCreated()) {
                if (flags.length() != 0) {
                    flags.append(", ");
                }
                flags.append("created");
            }
            if (baseWidget.isConfigOnly()) {
                if (flags.length() != 0) {
                    flags.append(", ");
                }
                flags.append("configOnly");
            }
        }

        if (widget instanceof Canvas) {
            Canvas canvas = (Canvas) widget;

            if (canvas.isDrawn()) {
                if (flags.length() != 0) {
                    flags.append(", ");
                }
                flags.append("drawn");
            }
            if (canvas.isDirty()) {
                if (flags.length() != 0) {
                    flags.append(", ");
                }
                flags.append("dirty");
            }
            if (canvas.isDisabled()) {
                if (flags.length() != 0) {
                    flags.append(", ");
                }
                flags.append("disabled");
            }
        }

        if (widget instanceof HLayout) {
            if (flags.length() != 0) {
                flags.append(", ");
            }
            flags.append("hlayout");
        } else if (widget instanceof VLayout) {
            if (flags.length() != 0) {
                flags.append(", ");
            }
            flags.append("vlayout");
        } else if (widget instanceof Layout) {
            if (flags.length() != 0) {
                flags.append(", ");
            }
            flags.append("layout");
        }

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("  ");
        }

        Log.info(indent + simpleClassName + "[id=" + id + ", title=" + title + ", flags=[" + flags + "]]");
    }

    private static List<Widget> getChildren(Widget widget) {
        List<Widget> children;
        if (widget instanceof TabSet) {
            TabSet tabSet = (TabSet) widget;
            Tab[] tabs = tabSet.getTabs();
            children = new ArrayList<Widget>();
            for (Tab tab : tabs) {
                children.add(tab.getPane());
            }
        } else if (widget instanceof Canvas) {
            Canvas canvas = (Canvas) widget;
            Canvas[] childrenArray = canvas.getChildren();
            children = new ArrayList<Widget>();
            children.addAll(Arrays.asList(childrenArray));
        } else {
            children = new ArrayList<Widget>();
        }
        return children;
    }
}
