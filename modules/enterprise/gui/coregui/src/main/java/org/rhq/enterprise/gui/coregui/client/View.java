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

import com.smartgwt.client.widgets.Canvas;

/**
 * TODO
 *
 * @author Ian Springer
 */
public class View {
    private ViewId id;
    private Canvas canvas;
    private Breadcrumb breadcrumb;

    public View(ViewId id) {
        this(id, null, null);
    }

    public View(ViewId id, Canvas canvas) {
        this(id, canvas, null);
    }

    public View(ViewId id, Breadcrumb breadcrumb) {
        this(id, null, breadcrumb);
    }

    public View(ViewId id, Canvas canvas, Breadcrumb breadcrumb) {
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
        this.canvas = canvas;
    }

    public ViewId getId() {
        return id;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public Breadcrumb getBreadcrumb() {
        return breadcrumb;
    }

    @Override
    public String toString() {
        return "View[" +
                "id=" + id +
                ", canvas=" + canvas +
                ", breadcrumb=" + breadcrumb +
                ']';
    }
}
