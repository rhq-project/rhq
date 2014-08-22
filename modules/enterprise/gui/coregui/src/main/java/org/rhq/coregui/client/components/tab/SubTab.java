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

package org.rhq.coregui.client.components.tab;

import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.coregui.client.components.view.ViewFactory;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.util.enhanced.Enhanced;

/**
 * A SubTab associating a Button with a Canvas.  SubTabs for the same parent Tab must have different viewNames.
 *
 * @author Jay Shaughnessy
 */
public class SubTab implements Enhanced {
    private NamedTab parent;
    private ViewName viewName;
    private Canvas canvas;
    private ViewFactory viewFactory;
    private Button button;
    private String id;

    private SubTab actualNext;
    private SubTab visibleNext;

    public SubTab(NamedTab parent, ViewName viewName, Canvas canvas) {
        this(parent, viewName, canvas, null);
    }

    public SubTab(NamedTab parent, ViewName viewName, Canvas canvas, ViewFactory viewFactory) {
        this.parent = parent;
        this.viewName = viewName;
        this.canvas = canvas;
        this.viewFactory = viewFactory;
        this.button = null;

        this.id = parent.getViewName().getName() + "_:_" + viewName.getName();
    }

    public Canvas getCanvas() {
        if (null == canvas && null != viewFactory) {
            canvas = viewFactory.createView();
            canvas.addStyleName("subtab");
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

    public Button getButton() {
        return button;
    }

    public void setButton(Button button) {
        this.button = button;
    }

    public NamedTab getParent() {
        return parent;
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

    /**
     * @return a unique identifier for the SubTab that combines the parent Tab viewName and the SubTab viewName.
     */
    public String getId() {
        return id;
    }

    /**
     * This is the successor or tab immediately to the right of this tab when all tabs
     * are visible. The tab to which actualNext refers does not change whereas the tab to
     * which {@link #getVisibleNext visibleNext} refers can change.
     *
     * @return The successor or tab immediately to the right of this tab when all tabs are
     * visible.
     */
    public SubTab getActualNext() {
        return actualNext;
    }

    /**
     * @param actualNext The successor or tab immediately to the right of this tab when all
     * tabs are visible. The tab to which actualNext refers does not change whereas the tab
     * to which {@link #getVisibleNext visibleNext} refers can change.
     */
    public void setActualNext(SubTab actualNext) {
        this.actualNext = actualNext;
    }

    /**
     * The successor or tab immediately to the right of this tab among the set of visible
     * tabs. The tab to which visibleNext refers can change whereas the tab to which
     * {@link #getActualNext actualNext} refers will not change.
     *
     * @return The successor or tab immediately to the right of this tab among the set of
     * visible tabs.
     */
    public SubTab getVisibleNext() {
        return visibleNext;
    }

    /**
     * @param visibleNext The successor or tab immediately to the right of this tab among
     * the set of visible tabs. The tab to which visibleNext refers can change whereas the
     * tab to which {@link #getActualNext actualNext} refers will not change.
     */
    public void setVisibleNext(SubTab visibleNext) {
        this.visibleNext = visibleNext;
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
    public String toString() {
        return "SubTab[parent=" + parent.getName() + ", title=" + this.viewName.getTitle() + ", name=" + this.viewName
            + "]";
    }
}
