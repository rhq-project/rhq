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

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.common.tag.FunctionTagLibrary;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A tag to show the inventory hierarchy links for the current resource or group, e.g.: _MAINFRAME_ > _JBoss Application
 * Server 4.0.4.GA_ > _AgentQueue_
 *
 * @author Ian Springer
 */
public class InventoryHierarchyTag extends TagSupport {

    private static final String SEPARATOR = " &gt; ";

    private Integer resourceId;
    private Integer groupId;
    private Integer resourceTypeId;
    private Integer parentResourceId;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();
    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getParentResourceId() {
        return parentResourceId;
    }

    public void setParentResourceId(Integer parentResourceId) {
        this.parentResourceId = parentResourceId;
    }

    public Integer getResourceTypeId() {
        return resourceTypeId;
    }

    public void setResourceTypeId(Integer resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    @Override
    public final int doStartTag() throws JspException {
        String html;
        if (this.resourceId != null) {
            html = buildResourceHTML(this.resourceId);
        } else if (this.groupId != null) {
            html = buildGroupHTML();
        } else if (this.resourceTypeId != null) {
            html = buildAutoGroupHTML();
        } else {
            throw new JspException("Neither 'resourceId' nor 'groupId' attribute is present on the tag.");
        }

        try {
            this.pageContext.getOut().write(html);
        } catch (IOException e) {
            throw new JspException(e);
        }

        return SKIP_BODY;
    }

    private String buildResourceHTML(Integer resourceId) {
        List<Resource> ancestorResources = this.resourceManager.getResourceLineage(resourceId);
        if (ancestorResources.isEmpty()) {
            throw new IllegalStateException(
                "The list of ancestor resources should always contain at least one resource - the resource whose lineage was requested.");
        }

        Resource parentResource = ancestorResources.get(ancestorResources.size() - 1);
        StringBuilder html = new StringBuilder();
        for (Resource ancestorResource : ancestorResources) {
            html.append("<a href=\"").append(buildResourceURL(ancestorResource)).append("\">");
            html.append(ancestorResource.getName()).append("</a>");
            if (ancestorResource.getId() != parentResource.getId()) // separator after every item except the last one
            {
                html.append(SEPARATOR);
            }
        }

        return html.toString();
    }

    private String buildGroupHTML() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        Subject subject;
        try {
            subject = RequestUtils.getSubject(request);
        } catch (ServletException e) {
            throw new JspException(e.getMessage());
        }

        ResourceGroup resourceGroup = this.resourceGroupManager.getResourceGroupById(subject, this.groupId, null);
        StringBuilder html = new StringBuilder();
        html.append("<a href=\"").append(buildGroupURL(resourceGroup)).append("\">");
        html.append(resourceGroup.getName()).append("</a>");
        return html.toString();
    }

    private String buildAutoGroupHTML() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        Subject subject;
        try {
            subject = RequestUtils.getSubject(request);
        } catch (ServletException e) {
            throw new JspException(e.getMessage());
        }

        StringBuilder html = new StringBuilder();
        html.append(buildResourceHTML(this.parentResourceId));
        html.append(SEPARATOR);
        try {
            ResourceType resourceType = resourceTypeManager.getResourceTypeById(subject, this.resourceTypeId);
            html.append("<a href=\"").append(buildAutoGroupURL()).append("\">");
            html.append(resourceType.getName()).append("</a>");
        } catch (ResourceTypeNotFoundException e) {
            throw new JspException(e.getMessage());
        }

        return html.toString();
    }

    private String buildResourceURL(Resource resource) {
        String url = FunctionTagLibrary.getDefaultResourceTabURL() + "?id=" + resource.getId();

        // Session-encode the URL in case the client doesn't have cookies enabled.
        return encodeURL(url);
    }

    private String buildGroupURL(ResourceGroup resourceGroup) {
        GroupCategory category = resourceGroup.getGroupCategory();
        String url = FunctionTagLibrary.getDefaultGroupTabURL() + "?category=" + category.name() + "&groupId="
            + resourceGroup.getId();

        // Session-encode the URL in case the client doesn't have cookies enabled.
        return encodeURL(url);
    }

    private String buildAutoGroupURL() {
        String url = FunctionTagLibrary.getDefaultAutoGroupTabURL() + "?id=" + parentResourceId + "&parent="
            + parentResourceId + "&type=" + resourceTypeId;

        // Session-encode the URL in case the client doesn't have cookies enabled.
        return encodeURL(url);
    }

    private String encodeURL(String url) {
        HttpServletResponse response = (HttpServletResponse) this.pageContext.getResponse();
        return response.encodeURL(url);
    }
}