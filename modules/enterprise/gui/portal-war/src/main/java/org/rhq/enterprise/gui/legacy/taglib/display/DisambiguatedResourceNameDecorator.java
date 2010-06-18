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
import java.io.StringWriter;

import org.rhq.core.domain.resource.composite.DisambiguationReport;

/**
 * For use in display:column elements
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceNameDecorator extends BaseDecorator {

    private int resourceId;
    private String resourceName;
    private String url;
    private Boolean nameAsLink;
    private DisambiguationReport<?> disambiguationReport;

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
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

    public DisambiguationReport<?> getDisambiguationReport() {
        return disambiguationReport;
    }

    public void setDisambiguationReport(DisambiguationReport<?> disambiguationReport) {
        this.disambiguationReport = disambiguationReport;
    }

    public String decorate(Object obj) {
        DisambiguationReport<?> report = getDisambiguationReport();
        if (report == null) {
            report = (DisambiguationReport<?>) obj;
        }

        try {
            //default to true
            boolean renderLink = nameAsLink == null || nameAsLink.booleanValue();

            String renderedUrl = null;
            if (renderLink) {
                renderedUrl = this.url;
                if (renderedUrl == null) {
                    renderedUrl = DisambiguatedResourceNameTag.getDefaultResourceUrl(resourceId);
                }
            }
            
            StringWriter writer = new StringWriter();
            DisambiguatedResourceNameTag.writeResource(writer, renderedUrl, resourceName, report.getResourceType(), true);

            return writer.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
