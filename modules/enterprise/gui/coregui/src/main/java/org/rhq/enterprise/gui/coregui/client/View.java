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
package org.rhq.enterprise.gui.coregui.client;

/**
 * A view in the GUI which has a bookmarkable URL. The fragment portion of the URL is what
 * uniquely identifies the view. View id's are hierarchical and are therefore represented as
 * paths (e.g. "http://localhost:7080/coregui/CoreGUI.html#Resource/10001" is a view URL,
 * whose view id is "Resource/1001". The URL of its parent view is
 * ""http://localhost:7080/coregui/CoreGUI.html#Resource", whose view id is "Resource".
 *
 * @author Ian Springer
 */
public class View {
    private ViewId id;
    private ViewRenderer descendantViewRenderer;
    private Breadcrumb breadcrumb;
    private View parent;

    public View(ViewId id) {
        this(id, null, null);
    }

    public View(ViewId id, ViewRenderer descendantViewRenderer) {
        this(id, descendantViewRenderer, null);
    }

    public View(ViewId id, Breadcrumb breadcrumb) {
        this(id, null, breadcrumb);
    }

    public View(ViewId id, ViewRenderer descendantViewRenderer, Breadcrumb breadcrumb) {
        if (id == null) {
            throw new IllegalArgumentException("Id is null.");
        }
        this.id = id;
        if (breadcrumb != null) {
            if (!breadcrumb.getName().equals(id.getName())) {
                throw new IllegalArgumentException("Breadcrumb name is not equal to id name.");
            }
            this.breadcrumb = breadcrumb;
        } else {
            this.breadcrumb = new Breadcrumb(id.getName());
        }
        this.descendantViewRenderer = descendantViewRenderer;
    }

    /**
     * Returns this view's unique id.
     *
     * @return this view's unique id
     */
    public ViewId getId() {
        return this.id;
    }

    /**
     * Returns a view renderer that should be used to render descendant views, or null if
     * the view renderer that rendered this view should also be used to render descendant
     * views.
     *
     * @return a view renderer that should be used to render descendant views, or null if
     *         the view renderer that rendered this view should also be used to render
     *         descendant views
     */
    public ViewRenderer getDescendantViewRenderer() {
        if (this.descendantViewRenderer != null) {
            return this.descendantViewRenderer;
        } else if (this.parent != null) {
            return this.parent.getDescendantViewRenderer();
        } else {
            return null;
        }
    }

    /**
     * Returns info that should be used when rendering a breadcrumb for this view in the
     * breadcrumb trail, or null if a default breadcrumb should be used.
     *
     * @return info that should be used when rendering a breadcrumb for this view in the
     *         breadcrumb trail, or null if a default breadcrumb should be used
     */
    public Breadcrumb getBreadcrumb() {
        return this.breadcrumb;
    }

    public View getParent() {
        return this.parent;
    }

    public void setParent(View parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "View[" +
                "id=" + id +
                ", descendantViewRenderer=" +
                ((this.descendantViewRenderer != null) ? this.descendantViewRenderer.getClass().getName() : null) +
                ", breadcrumb=" + this.breadcrumb +
                ']';
    }
}
