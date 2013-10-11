/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.coregui.client.drift.util;

import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.coregui.client.PopupWindow;
import org.rhq.coregui.client.util.StringUtility;

/**
 * A collection of utility methods for working with unified diffs.
 */
public class DiffUtility {

    /**
     * Formats the specified unified diff output as HTML.
     *
     * @param deltas
     * @param oldVersion
     * @param newVersion
     *
     * @return the specified unified diff output formatted as HTML
     */
    public static String formatAsHtml(List<String> deltas, int oldVersion, int newVersion) {
        StringBuilder html = new StringBuilder();

        String originalFilename = deltas.get(0);
        html.append("<b>").append(StringUtility.escapeHtml(originalFilename)).
            append(":").append(oldVersion).append("</b><br/>");
        String revisedFilename = deltas.get(1);
        html.append("<b>").append(StringUtility.escapeHtml(revisedFilename)).
            append(":").append(newVersion).append("</b><br/>");

        List<String> lines = deltas.subList(2, deltas.size());
        for (String line : lines) {
            String escapedLine = StringUtility.escapeHtml(line);
            if (line.startsWith("@@")) {
                html.append("<font color=\"blue\">").append(escapedLine).append("</font>");
            } else if (line.startsWith("-")) {
                html.append("<font color=\"red\">").append(escapedLine).append("</font>");
            } else if (line.startsWith("+")) {
                html.append("<font color=\"green\">").append(escapedLine).append("</font>");
            } else {
                html.append(escapedLine);
            }
            html.append("<br/>");
        }

        return html.toString();
    }

    /**
     * Create a non-modal window containing unified diff HTML.
     *
     * @param contents
     * @param path
     * @param oldVersion
     * @param newVersion
     *
     * @return a non-modal window containing unified diff HTML
     */
    public static Window createDiffViewerWindow(String contents, String path, int oldVersion, int newVersion) {
        VLayout layout = new VLayout();
        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setHeight100();

        CanvasItem canvasItem = new CanvasItem();
        canvasItem.setColSpan(2);
        canvasItem.setShowTitle(false);
        canvasItem.setWidth("*");
        canvasItem.setHeight("*");

        Canvas canvas = new Canvas();
        canvas.setContents(contents);
        canvasItem.setCanvas(canvas);

        form.setItems(canvasItem);
        layout.addMember(form);

        PopupWindow window = new PopupWindow(layout);
        window.setTitle(path + ":" + oldVersion + ":" + newVersion);
        window.setIsModal(false);

        return window;
    }

    private DiffUtility() {
    }

}
