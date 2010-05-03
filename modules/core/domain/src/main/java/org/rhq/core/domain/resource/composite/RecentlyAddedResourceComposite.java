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
package org.rhq.core.domain.resource.composite;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class RecentlyAddedResourceComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int parentId;
    private String name;
    private long ctime;
    private boolean showChildren;
    private List<RecentlyAddedResourceComposite> children;
    private String resourceTypeName;
    
    public RecentlyAddedResourceComposite() {    
    }
    
    public RecentlyAddedResourceComposite(int id, String name, String resourceTypeName, long ctime) {
        this.id = id;
        this.name = name;
        this.resourceTypeName = resourceTypeName;
        this.ctime = ctime;
    }

    public int getId() {
        return id;
    }

    public int getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
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
        for (RecentlyAddedResourceComposite child : children) {
            child.parentId = id;
        }
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