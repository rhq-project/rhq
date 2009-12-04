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

import java.util.List;

import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TabType;

/**
 * A tab in the Resource or Group view.
 *  
 * @author Ian Springer
 */
public class Tab
{
    private List<Tab> children;
    private String name;
    private String displayName;
    private String url;
    private String iconUrl;

    public Tab(TabType tab)
    {
        this.name = tab.getName();
        this.displayName = tab.getDisplayName();
        this.url = tab.getUrl();
        this.iconUrl = tab.getIconUrl();
    }

    public List<Tab> getChildren()
    {
        return children;
    }

    public void setChildren(List<Tab> children)
    {
        this.children = children;
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
}
