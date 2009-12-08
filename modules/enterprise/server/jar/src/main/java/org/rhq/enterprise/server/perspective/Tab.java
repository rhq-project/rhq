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
package org.rhq.enterprise.server.perspective;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourceActivatorSetType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TabType;

/**
 * A tab in the Resource or Group view.
 *  
 * @author Ian Springer
 */
public class Tab implements Serializable, Cloneable
{
    private static final long serialVersionUID = 1L;

    private List<Tab> children;
    private String qualifiedName;
    private String name;
    private String displayName;
    private String url;
    private String iconUrl;
    transient private List<ResourceActivatorSetType> currentResourceOrGroupActivatorSet;

    public Tab(TabType tab)
    {
        this.qualifiedName = tab.getName();
        int lastDotIndex = this.qualifiedName.lastIndexOf(".");
        this.name = this.qualifiedName.substring(lastDotIndex + 1);
        this.displayName = tab.getDisplayName();
        this.url = tab.getUrl();
        if (tab.getApplication() != null) {
            this.url += "&tab=" + this.qualifiedName;
        }
        this.iconUrl = tab.getIconUrl();
        this.currentResourceOrGroupActivatorSet = (tab.getCurrentResourceOrGroupActivatorSet() != null) ?
                tab.getCurrentResourceOrGroupActivatorSet() : Collections.<ResourceActivatorSetType>emptyList();
        this.children = new ArrayList<Tab>();
    }

    @NotNull
    public List<Tab> getChildren()
    {
        return children;
    }

    public void setChildren(List<Tab> children)
    {
        this.children = (children != null) ? children : new ArrayList<Tab>();
    }

    public String getQualifiedName()
    {
        return qualifiedName;
    }

    public String getName()
    {
        return name;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getUrl()
    {
        return url;
    }

    public String getIconUrl()
    {
        return iconUrl;
    }

    @NotNull
    public List<ResourceActivatorSetType> getCurrentResourceOrGroupActivatorSets()
    {
        return currentResourceOrGroupActivatorSet;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tab tab = (Tab)o;

        if (!name.equals(tab.name)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString() {        
        return this.getClass().getSimpleName() + "[name=" + this.name + ", displayName=" + this.displayName + ", url=" + this.url + ", iconUrl="
            + this.iconUrl + "]";
    }
    
    @Override
    public Tab clone() throws CloneNotSupportedException {
        Object obj = null;
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            obj = in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Tab tab = (Tab)obj;
        if (tab != null) {
            tab.currentResourceOrGroupActivatorSet = this.currentResourceOrGroupActivatorSet;
            List<Tab> subtabs = tab.getChildren();
            if (subtabs != null) {
                for (int i = 0, subtabsSize = subtabs.size(); i < subtabsSize; i++)
                {
                    Tab subtab = subtabs.get(i);
                    subtab.currentResourceOrGroupActivatorSet = this.children.get(i).currentResourceOrGroupActivatorSet;
                }
            }
        }
        return tab;
    }
}
