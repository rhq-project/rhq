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
 * TODO
 *
 * @author Ian Springer
 */
public class ViewId {
    public static final char PATH_SEPARATOR = '/';
    public static final ViewId ROOT_VIEW_ID = new ViewId("", null);

    private String name;
    private ViewId parent;
    private String path;

    /**
     * Create a view id with the specified base name and parent. The name must not contain any slashes. The parent
     * should not be null, unless this is the root view id.
     *
     * @param name the base name of the view represented by this id
     * @param parent the id of the view represented by this id's parent view
     */
    public ViewId(String name, ViewId parent) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null.");
        }
        if (name.indexOf(PATH_SEPARATOR) >= 0) {
            throw new IllegalArgumentException("Name contains illegal character '" + PATH_SEPARATOR + "'. ");
        }
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return this.name;
    }

    public ViewId getParent() {
        return this.parent;
    }

    public String getPath() {
        if (this.path == null) {
            StringBuilder buffer = new StringBuilder();
            if (this.parent != null) {
                String parentPath = this.parent.getPath();
                buffer.append(parentPath);
                if (!parentPath.equals(ROOT_VIEW_ID.getPath())) {
                    buffer.append(PATH_SEPARATOR);
                }
            }
            buffer.append(this.name);
            this.path = buffer.toString();
        }
        return this.path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewId viewId = (ViewId) o;

        if (!getPath().equals(viewId.getPath())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public String toString() {
        return getPath();
    }
}
