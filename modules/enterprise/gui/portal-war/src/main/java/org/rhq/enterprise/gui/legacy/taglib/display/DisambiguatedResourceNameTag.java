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
import java.io.Writer;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.enterprise.gui.common.tag.FunctionTagLibrary;

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceNameTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    private DisambiguationReport<?> disambiguationReport;
    private String resourceName;
    private Integer resourceId;
    private Boolean nameAsLink;
    private String url;

    public DisambiguationReport<?> getDisambiguationReport() {
        return disambiguationReport;
    }

    public void setDisambiguationReport(DisambiguationReport<?> disambiguationReport) {
        this.disambiguationReport = disambiguationReport;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
            if (disambiguationReport != null) {

                DisambiguationReport.ResourceType resourceType = disambiguationReport.getResourceType();

                //default to true
                boolean renderLink = nameAsLink == null || nameAsLink.booleanValue();

                String renderedUrl = null;
                if (renderLink) {
                    renderedUrl = this.url;
                    if (renderedUrl == null) {
                        renderedUrl = getDefaultResourceUrl(resourceId);
                    }
                }
                writeResource(writer, renderedUrl, resourceName, resourceType, true);
            }

            return super.doEndTag();
        } catch (IOException e) {
            throw new JspTagException(e);
        }
    }

    public static void writeResource(Writer writer, String url, String resourceName,
        DisambiguationReport.ResourceType resourceType, boolean htmlOutput) throws IOException {
        if (!resourceType.isSingleton()) {
            if (htmlOutput) {
                writer.append("<span class=\"disambiguated-resource-type\">");
            }

            writer.append(resourceType.getName()).append(" ");

            if (resourceType.getPlugin() != null) {
                if (htmlOutput) {
                    writer.append("<span class=\"disambiguated-resource-plugin\">");
                }
                writer.append("(").append(resourceType.getPlugin()).append(" plugin) ");
                if (htmlOutput) {
                    writer.append("</span>");
                }
            }

            if (htmlOutput) {
                writer.append("</span>");
            }
        }

        if (url != null) {
            writer.append("<a href=\"").append(url).append("\">");
        }

        if (htmlOutput) {
            writer.append("<span class=\"disambiguated-resource-name\">");
        }
        writer.append(resourceName);
        if (htmlOutput) {
            writer.append("</span>");
        }

        if (url != null) {
            writer.append("</a>");
        }

        if (resourceType.isSingleton() && resourceType.getPlugin() != null) {
            if (htmlOutput) {
                writer.append("<span class=\"disambiguated-resource-plugin\">");
            }

            writer.append(" (").append(resourceType.getPlugin()).append(" plugin)");

            if (htmlOutput) {
                writer.append("</span>");
            }
        }
    }

    public static String getDefaultResourceUrl(int resourceId) {
        return FunctionTagLibrary.getDefaultResourceTabURL() + "?id=" + resourceId;
    }
}
