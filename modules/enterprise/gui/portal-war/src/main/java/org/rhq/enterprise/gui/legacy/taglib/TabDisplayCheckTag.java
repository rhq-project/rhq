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
package org.rhq.enterprise.gui.legacy.taglib;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.inventory.resource.ResourceUIBean;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * A tag that will take a tab name and set the a specified context variable to a boolean that indicates whether or not
 * that tab should be displayed based on the facets the current resource or compatible group exposes.
 *
 * @author Ian Springer
 */
public class TabDisplayCheckTag extends VarSetterBaseTag {
    private String tabName;

    public String getTabName() {
        return this.tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    /**
     * This evaluates <em>value</em> as a struts expression, then outputs the resulting string to the <em>
     * pageContext</em>'s out.
     */
    @Override
    public int doStartTag() throws JspException {
        boolean displayTab;
        ResourceFacets resourceFacets = getResourceFacets();
        if (resourceFacets != null) {
            Tab tab = Tab.valueOf(this.tabName);
            switch (tab) {
            case Monitor:
            case Inventory:
            case Alert: {
                // Always display the Inventory, Monitor, and Alert tabs (even if no metrics are defined for a resource
                // type, the Monitor and Alert tabs can still be used to monitor availablity).
                displayTab = true;
                break;
            }

            case Configuration: {
                displayTab = resourceFacets.isConfiguration();
                break;
            }

            case Control: {
                displayTab = resourceFacets.isOperation();
                break;
            }

            case Content: {
                displayTab = resourceFacets.isContent();
                break;
            }

            default: {
                throw new IllegalStateException("Unrecognized tab name: " + tab);
            }
            }
        } else {
            displayTab = true;
        }

        setScopedVariable(displayTab);
        return SKIP_BODY;
    }

    private Subject getSubject(HttpServletRequest request) throws JspException {
        Subject subject;
        try {
            subject = RequestUtils.getSubject(request);
        } catch (ServletException e) {
            throw new JspException("Failed to lookup the current user from the request context.", e);
        }

        return subject;
    }

    @Nullable
    private ResourceFacets getResourceFacets() throws JspException {
        ResourceFacets resourceFacets = null;
        HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
        Subject subject = getSubject(request);
        Resource resource = RequestUtils.getResource(request);
        if (resource != null) {
            ResourceUIBean resourceUIBean = new ResourceUIBean(resource, subject);
            resourceFacets = resourceUIBean.getFacets();
        } else {
            ResourceGroup group = RequestUtils.getResourceGroup(request);
            if (group != null) {
                // TODO: Add support for retrieving the ResourceFacts for a group.
                resourceFacets = ResourceFacets.ALL;
            }
        }

        return resourceFacets;
    }

    /**
     * Reset the values of the tag.
     */
    @Override
    public void release() {
        super.release();
        tabName = null;
    }

    /**
     * NOTE: These tab names correspond to the names defined in the ".tabs.resource.common" Tiles definition in
     * WEB-INF/tiles/resource-commmon-def.xml.
     */
    enum Tab {
        Monitor, Inventory, Configuration, Control, Alert, Content
    }
}