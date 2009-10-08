/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.uibeans;

import java.io.Serializable;

/**
 * Contains the information to display for a metric's configuration
 */
public class MetricConfigSummary implements Serializable {
    private int id; // The template ID
    private String name;
    private String description;
    private String category;
    private long interval; // In milliseconds

    /**
     * Default Constructor
     */
    public MetricConfigSummary() {
    }

    /**
     * Constructor with id, name and category.
     */
    public MetricConfigSummary(int id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        id = i;
    }

    public String getName() {
        return name;
    }

    public void setName(String string) {
        name = string;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String string) {
        category = string;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String string) {
        description = string;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long l) {
        interval = l;
    }
}