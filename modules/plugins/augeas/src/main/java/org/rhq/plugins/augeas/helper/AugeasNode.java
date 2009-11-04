/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.augeas.helper;

/**
 * An abstract representation of a node in an Augeas configuration tree (e.g. /files/etc/hosts/3/canonical).
 *
 * @author Ian Springer
 */
public class AugeasNode {
    public static final char SEPARATOR_CHAR = '/';
    public static final char VARIABLE_CHAR = '$';
    public static final String SEPARATOR = new String(new char[] { SEPARATOR_CHAR });

    private String path;

    public static String patchNodeName(String name) {
        //first split by /
        StringBuilder bld = new StringBuilder();
        int start = 0;
        do {
            int nextSlash = name.indexOf(SEPARATOR, start);
            int nextOpenBracket = name.indexOf('[', start);
            int nextCloseBracket = name.indexOf(']', start);
            
            if (nextSlash > nextCloseBracket) {
                if (nextOpenBracket == -1) {
                    bld.append(name.substring(start, nextSlash + 1).replaceAll(" ", "\\\\ "));
                } else {
                    bld.append(name.substring(start, nextOpenBracket).replaceAll(" ", "\\\\ "))
                        .append(name, nextOpenBracket, nextCloseBracket + 1);
                }
            } else {
                if (nextSlash == -1) nextSlash = name.length();
                if (nextOpenBracket != -1) {
                    bld.append(name.substring(start, nextOpenBracket).replaceAll(" ", "\\\\ "))
                    .append(name, nextOpenBracket, nextCloseBracket + 1);
                    nextSlash = nextCloseBracket;
                } else {
                    bld.append(name.substring(start).replaceAll(" ", "\\\\ "));
                }
            }
            
            start = nextSlash + 1;
        } while (start > 0 && start < name.length());
        
        return bld.toString();
    }
    
    public AugeasNode(String path) {
        if (path == null) {
            throw new IllegalArgumentException("'path' parameter must not be null.");
        }
        if (path.charAt(0) != SEPARATOR_CHAR && path.charAt(0) != VARIABLE_CHAR) {
            throw new IllegalArgumentException("Specified path (" + path + ") is not absolute.");
        }

        // Remove redundant "." and ".." components and redundant slashes.
        // TODO: This is temporary until augeas-java fixes a bug.
        this.path = patchNodeName(path);
    }

    public AugeasNode(AugeasNode parent, String name) {
        if (parent == null) {
            throw new IllegalArgumentException("'parentNode' parameter must not be null.");
        }
        if (name == null) {
            throw new IllegalArgumentException("'name' parameter must not be null.");
        }
        /*
        if (name.charAt(0) == SEPARATOR_CHAR) {
            throw new IllegalArgumentException("Specified path (" + path + ") is not relative.");
        }*/

        // TODO: This is temporary until augeas-java fixes a bug.
        this.path = parent.getPath() + SEPARATOR_CHAR + patchNodeName(name);
    }

    public AugeasNode(String parentPath, String name) {
        this(new AugeasNode(parentPath), name);
    }

    public String getName() {
        int lastSlashIndex = this.path.lastIndexOf(SEPARATOR_CHAR);
        return (lastSlashIndex == 0) ? this.path : this.path.substring(lastSlashIndex + 1);
    }

    public String getPath() {
        return this.path;
    }

    public AugeasNode getParent() {
        String parentPath = getParentPath();
        if (parentPath.indexOf(SEPARATOR_CHAR) == -1 && parentPath.startsWith(AugeasVariable.VARIABLE_NAME_PREFIX)) {
            return new AugeasVariable(parentPath.substring(AugeasVariable.VARIABLE_NAME_PREFIX.length()));
        } else {
            return new AugeasNode(parentPath);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        AugeasNode that = (AugeasNode) obj;

        if (this.path != null ? !this.path.equals(that.path) : that.path != null)
            return false;

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

    protected String getParentPath() {
        String path = getPath();
        if (path.equals(SEPARATOR)) {
            // special case - parent of "/" is "/".
            return path;
        } else {
            char lastChar = path.charAt(path.length() - 1);
            if (lastChar == SEPARATOR_CHAR) {
                path = path.substring(0, path.length() - 1);
            }
            int lastSlashIndex = path.lastIndexOf(SEPARATOR_CHAR);
            return path.substring(0, lastSlashIndex);
        }
    }
}
