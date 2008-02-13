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
package org.rhq.core.domain.resource.composite;

import java.util.ArrayList;
import java.util.List;

public class RecentlyAddedResourceComposite {
    private int id;
    private String name;
    private long ctime;
    private boolean showChildren;
    private List<RecentlyAddedResourceComposite> children;

    public RecentlyAddedResourceComposite(int id, String name, long ctime) {
        this.id = id;
        this.name = name;
        this.ctime = ctime;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCtime() {
        return ctime;
    }

    public boolean isShowChildren() {
        return showChildren;
    }

    public void setShowChildren(boolean show) {
        this.showChildren = show;
    }

    public List<RecentlyAddedResourceComposite> getChildren() {
        return (children != null) ? children : new ArrayList<RecentlyAddedResourceComposite>();
    }

    public void setChildren(List<RecentlyAddedResourceComposite> children) {
        this.children = children;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof RecentlyAddedResourceComposite)) {
            return false;
        }

        return this.id == ((RecentlyAddedResourceComposite) obj).id;
    }
}