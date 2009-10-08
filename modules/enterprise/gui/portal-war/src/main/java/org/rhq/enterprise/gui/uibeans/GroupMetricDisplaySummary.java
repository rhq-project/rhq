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
/*
 * GroupMetricDisplaySummary.java
 *
 * Created on Apr 8, 2003
 */
package org.rhq.enterprise.gui.uibeans;

import java.io.Serializable;

/**
 * This bean encapsulates the data used to display a list of group metrics
 */
public class GroupMetricDisplaySummary extends MetricConfigSummary implements Serializable {
    private int totalMembers;
    private int activeMembers = 0;

    /**
     * Default constructor.
     */
    public GroupMetricDisplaySummary() {
    }

    /**
     * Constructor with id, name and category.
     */
    public GroupMetricDisplaySummary(int id, String name, String category) {
        super(id, name, category);
    }

    /**
     * Increment the number of active members.
     */
    public void incrementMember() {
        this.activeMembers++;
    }

    public int getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(int i) {
        activeMembers = i;
    }

    public int getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(int i) {
        totalMembers = i;
    }
}

// EOF
