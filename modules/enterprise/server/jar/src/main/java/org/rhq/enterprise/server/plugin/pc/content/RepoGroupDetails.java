/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc.content;

/**
 * Represents a repo group being introduced by a repo source.
 *
 * @author Jason Dobies
 *
 * @see RepoSource
 */
public class RepoGroupDetails {

    private String name;
    private String typeName;
    private String description;

    public RepoGroupDetails(String name, String typeName) {
        setName(name);
        setTypeName(typeName);
    }

    /**
     * Returns the name of the group.
     *
     * @return will not be <code>null</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the group being represented.
     *
     * @param name cannot be <code>null</code>
     */
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        this.name = name;
    }

    /**
     * Returns the name of the type of group this instance represents.
     *
     * @return will not be <code>null</code>
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Sets the name of the group type this repo belongs to.
     *
     * @param typeName cannot be <code>null</code>
     */
    public void setTypeName(String typeName) {
        if (typeName == null) {
            throw new IllegalArgumentException("typeName cannot be null");
        }

        this.typeName = typeName;
    }

    /**
     * Returns the description of what this type represents.
     *
     * @return may be <code>null</code>
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description the group type should display to users.
     *
     * @param description may be <code>null</code>
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RepoGroupDetails[");
        sb.append("name='").append(this.name).append('\'');
        sb.append(", typeName='").append(this.typeName).append('\'');
        sb.append(']');
        return sb.toString();
    }
}
