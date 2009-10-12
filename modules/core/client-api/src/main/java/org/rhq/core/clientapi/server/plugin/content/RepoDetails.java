/*
* RHQ Management Platform
* Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.clientapi.server.plugin.content;

/**
 * Contains the information used to describe a single repo being introduced to the system through the
 * {@link RepoSource} interface. The parent-child relationship between repos is done through the call to
 * {@link #setParentRepoName(String)}.
 *
 * @author Jason Dobies
 */
public class RepoDetails {

    private String name;
    private String parentRepoName;
    private String description;

    public RepoDetails(String name) {
        setName(name);
    }

    public RepoDetails(String name, String parentRepoName, String description) {
        setName(name);
        setParentRepoName(parentRepoName);
        setDescription(description);
    }

    /**
     * Returns the identifying name of the repo.
     *
     * @return cannot be <code>null</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the repo being represented.
     *
     * @param name cannot be <code>null</code>
     * @throws IllegalArgumentException is name is <code>null</code>
     */
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        this.name = name;
    }

    /**
     * Returns the name (identifier) of the parent repo for this repo. The name returned here should match up with
     * the name returned the call to {@link #getName()} on the parent repo.
     *
     * @return may be <code>null</code>
     */
    public String getParentRepoName() {
        return parentRepoName;
    }

    /**
     * Sets the name of the parent repo to associate this repo with. See {@link #getParentRepoName()} for more
     * details.
     *
     * @param parentRepoName may be <code>null</code>
     */
    public void setParentRepoName(String parentRepoName) {
        this.parentRepoName = parentRepoName;
    }

    /**
     * Returns a user-readable description of what the repo contains.
     *
     * @return may be <code>null</code>
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the name of the user-readable description of the repo contents.
     *
     * @param description may be <code>null</code>
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
