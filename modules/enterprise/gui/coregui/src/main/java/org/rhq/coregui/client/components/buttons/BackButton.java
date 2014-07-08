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
package org.rhq.coregui.client.components.buttons;

import com.smartgwt.client.types.Cursor;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.coregui.client.CoreGUI;

/**
 * @author Greg Hinkle
 */
public class BackButton extends Canvas {

    public BackButton() {
        super();
    }

    public BackButton(String title, final String anchor) {
        this();
        setTitle(title);
        setContents(" \u00AB " + title);
        setCursor(Cursor.POINTER);
        setID("backButton");
        setHeight(30);
        addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                CoreGUI.goToView(anchor);
            }
        });
        setStyleName("backLink");
    }
}
