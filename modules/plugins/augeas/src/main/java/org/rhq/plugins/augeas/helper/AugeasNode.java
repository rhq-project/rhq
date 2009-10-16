/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.augeas.helper;

import java.net.URI;

/**
 * An abstract representation of a node in an Augeas configuration tree (e.g. /files/etc/hosts/3/canonical).
 *
 * @author Ian Springer
 */
public class AugeasNode {
    public static final char SEPARATOR_CHAR = '/';
    public static final String SEPARATOR = new String(new char[] {SEPARATOR_CHAR});

    private String path;

    public AugeasNode(String path) {
        if (path == null) {
	        throw new IllegalArgumentException("'path' parameter must not be null.");
	    }
        if (path.charAt(0) != SEPARATOR_CHAR) {
            throw new IllegalArgumentException("Specified path (" + path + ") is not absolute.");
        }

        // Remove redundant "." and ".." components and redundant slashes.
        this.path = normalize(path);
    }

    public AugeasNode(AugeasNode parent, String name) {
        if (parent == null) {
            throw new IllegalArgumentException("'parentNode' parameter must not be null.");
        }
        if (name == null) {
	        throw new IllegalArgumentException("'name' parameter must not be null.");
	    }
        if (name.charAt(0) == SEPARATOR_CHAR) {
            throw new IllegalArgumentException("Specified path (" + path + ") is not relative.");
        }

        this.path = parent.getPath() + SEPARATOR_CHAR + name;
    }

    public AugeasNode(String parentPath, String name) {
        this(new AugeasNode(parentPath), name);
    }

    private AugeasNode(AugeasNode child) {
        String path = child.getPath();
        if (path.equals(SEPARATOR)) {
            // special case - parent of "/" is "/".
            this.path = path;
        } else {
            char lastChar = path.charAt(path.length() -1);
            if (lastChar == SEPARATOR_CHAR) {
                path = path.substring(0, path.length() - 1);
            }
            int lastSlashIndex = path.lastIndexOf(SEPARATOR_CHAR);
            this.path = path.substring(0, lastSlashIndex);
        }
    }

    public String getName() {
        int lastSlashIndex = this.path.lastIndexOf(SEPARATOR_CHAR);
        return (lastSlashIndex == 0) ? this.path : this.path.substring(lastSlashIndex + 1);
    }

    public String getPath() {
        return this.path;
    }

    public AugeasNode getParent() {
        return new AugeasNode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AugeasNode that = (AugeasNode)obj;

        if (this.path != null ? !this.path.equals(that.path) : that.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return this.path != null ? this.path.hashCode() : 0;
    }

    @Override
    public String toString() {
        return this.path;
    }

    private static String normalize(String path) {
        URI uri = URI.create(path);
        return uri.normalize().getPath();
    }
}
