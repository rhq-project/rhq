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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Greg Hinkle
 */
public class ViewId {

    private String path;

    private List<Breadcrumb> breadcrumbs;

    public ViewId(String path, Breadcrumb... breadcrumbs) {
        this.path = path;
        if ( breadcrumbs != null) {
            this.breadcrumbs = Arrays.asList(breadcrumbs);
        } else {
            this.breadcrumbs = new ArrayList<Breadcrumb>();
        }
    }

    public ViewId(String path) {
        this.path = path;
        breadcrumbs = new ArrayList<Breadcrumb>();

        breadcrumbs.add(new Breadcrumb(path));
    }

    public List<Breadcrumb> getBreadcrumbs() {
        return breadcrumbs;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return this.path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewId viewId = (ViewId) o;

        if (!path.equals(viewId.path)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
