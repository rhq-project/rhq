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

package org.rhq.enterprise.server.plugin.pc.content;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pradeep Kilambi
 */
public class DistributionDetails {
    private String label;
    private String description;

    private String distType;
    private String distpath;
    private List<DistributionFileDetails> files;

    public DistributionDetails(String labelIn, String disttypeIn) {
        setLabel(labelIn);
        setDistributionType(disttypeIn);
    }

    public DistributionDetails(String labelIn, String distpathIn, String disttypeIn) {
        setLabel(labelIn);
        setDistributionPath(distpathIn);
        setDistributionType(disttypeIn);
    }

    public void addFile(DistributionFileDetails file) {
        if (files == null) {
            files = new ArrayList<DistributionFileDetails>();
        }
        files.add(file);
    }

    public void addFiles(List<DistributionFileDetails> filesIn) {
        files.addAll(filesIn);
    }

    public List<DistributionFileDetails> getFiles() {
        return files;
    }

    public void setFiles(List<DistributionFileDetails> filesIn) {
        files = filesIn;
    }

    /**
     * Returns the identifying name of the distribution.
     *
     * @return cannot be <code>null</code>
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the name of the distribution being represented.
     *
     * @param label cannot be <code>null</code>
     * @throws IllegalArgumentException is name is <code>null</code>
     */
    public void setLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("label cannot be null");
        }

        this.label = label;
    }

    /**
     * Returns the distribution type.
     *
     * @return may be <code>null</code>
     */
    public String getDistributionType() {
        return distType;
    }

    /**
     *
     * @param distType may be <code>null</code>
     */
    public void setDistributionType(String distType) {
        this.distType = distType;
    }

    /**
     * Returns a user-readable description of what the distribution contains.
     *
     * @return may be <code>null</code>
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the name of the user-readable description of the distribution.
     *
     * @param description may be <code>null</code>
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Return the distribution path on filesystem
     * @return
     */
    public String getDistributionPath() {
        return distpath;
    }

    /**
     * sets the distribution path value for a single distribution instance
     * @param distpath distribution path
     */
    public void setDistributionPath(String distpath) {
        this.distpath = distpath;
    }

    public String toString() {
        return "DistributionType = " + distType + ", label = " + label + ", path = " + distpath;
    }
}
