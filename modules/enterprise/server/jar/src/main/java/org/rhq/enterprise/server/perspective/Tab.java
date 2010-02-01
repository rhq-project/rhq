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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import org.rhq.enterprise.server.perspective.activator.ActivatorHelper;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TabType;

/**
 * A tab in the Resource or Group view.
 *  
 * @author Ian Springer
 */
public class Tab extends RenderedExtension implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String qualifiedName;
    private List<Tab> children;

    public Tab(TabType rawTab, String perspectiveName) {
        super(rawTab, perspectiveName, rawTab.getUrl());
        this.name = getSimpleName(rawTab.getName());
        this.qualifiedName = rawTab.getName();
        if (rawTab.getApplication() != null) {
            this.url += "&tab=" + this.qualifiedName;
        }
        this.children = new ArrayList<Tab>();
        this.debugMode = ActivatorHelper.initResourceActivators(rawTab.getActivators(), getActivators());
    }

    @NotNull
    public List<Tab> getChildren() {
        return children;
    }

    public void setChildren(List<Tab> children) {
        this.children = (children != null) ? children : new ArrayList<Tab>();
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getName() {
        return name;
    }

    private static String getSimpleName(String qualifiedName) {
        int lastDotIndex = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(lastDotIndex + 1);
    }

    /**
     * Note that this will clone the children list but not the child Tab objects themselves.
     * @see java.lang.Object#clone()
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
