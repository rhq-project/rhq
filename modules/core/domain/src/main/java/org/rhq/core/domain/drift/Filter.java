/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.domain.drift;

import java.io.Serializable;

public class Filter implements Serializable {
    private static final long serialVersionUID = 1L;

    private String path;
    private String pattern;

    /** For JAXB only **/
    public Filter() {
    }

    public Filter(String path, String pattern) {
        setPath(path);
        setPattern(pattern);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (path == null || "./".equals(path) || "/".equals(path)) {
            this.path = ".";
        } else {
            this.path = path;
        }
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        if (pattern == null) {
            this.pattern = "";
        } else {
            this.pattern = pattern;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof Filter) {
            Filter that = (Filter) obj;
            return this.path.equals(that.path) && this.pattern.equals(that.pattern);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + path.hashCode();
        result = 31 * result + pattern.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Filter[path: " + path + ", pattern: " + pattern + "]";
    }
}
