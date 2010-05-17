/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.gui.legacy.taglib.display;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.rhq.core.domain.resource.composite.DisambiguationReport;

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceNameTag extends TagSupport {

    private static final long serialVersionUID = 1L;
    
    private DisambiguationReport<?> disambiguatedReport;
    private String resourceName;
    private Integer resourceId;
    private Boolean nameAsLink;

    public DisambiguationReport<?> getDisambiguatedReport() {
        return disambiguatedReport;
    }

    public void setDisambiguatedReport(DisambiguationReport<?> disambiguatedReport) {
        this.disambiguatedReport = disambiguatedReport;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    public Boolean getNameAsLink() {
        return nameAsLink;
    }

    public void setNameAsLink(Boolean nameAsLink) {
        this.nameAsLink = nameAsLink;
    }

    @Override
    public int doEndTag() throws JspException {
        JspWriter writer = pageContext.getOut();

        try {
            if (disambiguatedReport != null) {

                DisambiguationReport.ResourceType resourceType = disambiguatedReport.getResourceType();

                //default to true
                boolean renderLink = nameAsLink == null || nameAsLink.booleanValue();

                writeResource(writer, renderLink, resourceId, resourceName, resourceType);
            }
            
            return super.doEndTag();
        } catch (IOException e) {
            throw new JspTagException(e);
        }
    }
    
    public static void writeResource(JspWriter writer, boolean renderLink, int resourceId, String resourceName, DisambiguationReport.ResourceType resourceType) throws IOException {
        if (!resourceType.isSingleton()) {
            writer.append(resourceType.getName()).append(" ");
            
            if (resourceType.getPlugin() != null) {
                writer.append("(").append(resourceType.getPlugin())
                    .append(" plugin) ");
            }
        }
        
        if (renderLink) {
            writer.append("<a href=\"/rhq/resource/summary/overview.xhtml?id=");
            writer.print(resourceId);
            writer.append("\">");
        }
        
        writer.append(resourceName);
     
        if (renderLink) {
            writer.append("</a>");
        }
        
        if (resourceType.isSingleton() && resourceType.getPlugin() != null) {
            writer.append(" ").append(resourceType.getPlugin())
                .append(" plugin)");
        }
    }
}
