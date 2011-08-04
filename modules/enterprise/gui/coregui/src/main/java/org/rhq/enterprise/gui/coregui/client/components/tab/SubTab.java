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

package org.rhq.enterprise.gui.coregui.client.components.tab;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableButton;

/**
 * A Locatable SubTab associating a Button with a Canvas.
 *  
 * @author Jay Shaughnessy
 */
public class SubTab implements Locatable {
    private String locatorId;
    private ViewName viewName;
    private Canvas canvas;
    private ViewFactory viewFactory;
    private LocatableButton button;

    public SubTab(String locatorId, ViewName viewName, Canvas canvas) {
        this.locatorId = locatorId;
        this.viewName = viewName;
        this.canvas = canvas;
        this.button = null;
    }

    public SubTab(String locatorId, ViewName viewName, Canvas initialCanvas, ViewFactory viewFactory) {
        this.locatorId = locatorId;
        this.viewName = viewName;
        this.canvas = initialCanvas;
        this.viewFactory = viewFactory;
        this.button = null;
    }

    public String getLocatorId() {
        return locatorId;
    }

    public Canvas getCanvas() {
        if (null == canvas && null != viewFactory) {
            canvas = viewFactory.createView();
        }
        return canvas;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    public void setViewFactory(ViewFactory viewFactory) {
        this.viewFactory = viewFactory;
    }

    public LocatableButton getButton() {
        return button;
    }

    public void setButton(LocatableButton button) {
        this.button = button;
    }

    public ViewName getViewName() {
        return viewName;
    }

    public String getName() {
        return viewName.getName();
    }

    public String getTitle() {
        return viewName.getTitle();
    }

    public void destroyButton() {
        if (null != button) {
            button.removeFromParent();
            button.destroy();
            button = null;
        }
    }

    public void destroyCanvas() {
        if (null != canvas) {
            canvas.removeFromParent();
            canvas.destroy();
            canvas = null;
        }
    }

    public void destroy() {
        destroyCanvas();
        destroyButton();
    }

    @Override
    public String extendLocatorId(String extension) {
        return this.locatorId + "_" + extension;
    }

    @Override
    public String toString() {
        return "SubTab[title=" + this.viewName.getTitle() + ", name=" + this.viewName.getName() + ", locatorId="
            + this.locatorId + "]";
    }
}
