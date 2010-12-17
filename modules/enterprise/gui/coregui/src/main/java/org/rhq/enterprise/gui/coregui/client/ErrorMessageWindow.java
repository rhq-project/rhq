/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * This is a convienence component that is a simple modal dialog box used to display
 * things like error messages/stack traces.
 *
 * Call {@link #show()} on the instantiated window to display it.
 *
 * @author John Mazzitelli
 */
public class ErrorMessageWindow extends LocatableWindow {
    public ErrorMessageWindow(String locatorId, String title, String message) {
        super(locatorId);

        LocatableHTMLPane htmlPane = new LocatableHTMLPane(extendLocatorId("winDetailsPane"));
        htmlPane.setMargin(10);
        htmlPane.setDefaultWidth(500);
        htmlPane.setDefaultHeight(400);
        htmlPane.setContents(message);

        setupWindow(title, htmlPane);
    }

    public ErrorMessageWindow(String locatorId, String title, Canvas item) {
        super(locatorId);
        setupWindow(title, item);
    }

    private void setupWindow(String title, Canvas item) {
        setTitle(title);
        setShowMinimizeButton(false);
        setShowMaximizeButton(true);
        setIsModal(true);
        setShowModalMask(true);
        setAutoSize(true);
        setAutoCenter(true);
        setShowResizer(true);
        setCanDragResize(true);
        centerInPage();
        addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClientEvent event) {
                markForDestroy();
            }
        });
        addItem(item);
    }
}
