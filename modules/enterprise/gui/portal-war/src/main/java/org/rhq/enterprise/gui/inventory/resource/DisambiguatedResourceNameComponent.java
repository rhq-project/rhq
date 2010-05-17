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

package org.rhq.enterprise.gui.inventory.resource;

import javax.faces.component.UIComponentBase;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * Component for displaying a resource name together with disambiguation
 * information.
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceNameComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.jboss.on.DisambiguatedResourceName";
    public static final String COMPONENT_FAMILY = "org.jboss.on.DisambiguatedResourceName";
    private static final String DISAMBIGUATION_REPORT_ATTRIBUTE = "disambiguationReport";
    private static final String RESOURCE_NAME_ATTRIBUTE = "resourceName";
    private static final String RESOURCE_ID_ATTRIBUTE = "resourceId";
    private static final String NAME_AS_LINK_ATTRIBUTE = "nameAsLink";
    
    private DisambiguationReport<?> disambiguationReport;
    private String resourceName;
    private Integer resourceId;
    private Boolean nameAsLink;

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public DisambiguationReport<?> getDisambiguationReport() {
        if (disambiguationReport == null) {
            return FacesComponentUtility.getExpressionAttribute(this, DISAMBIGUATION_REPORT_ATTRIBUTE,
                DisambiguationReport.class);
        } else {
            return disambiguationReport;
        }
    }

    public void setDisambiguationReport(DisambiguationReport<?> disambiguationReport) {
        this.disambiguationReport = disambiguationReport;
    }

    public String getResourceName() {
        if (resourceName == null) {
            return FacesComponentUtility.getExpressionAttribute(this, RESOURCE_NAME_ATTRIBUTE);
        } else {
            return resourceName;
        }
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public boolean isNameAsLink() {
        if (nameAsLink == null) {
            Boolean ret = FacesComponentUtility.getExpressionAttribute(this, NAME_AS_LINK_ATTRIBUTE, Boolean.class);
            return ret == null ? true : ret;
        } else {
            return nameAsLink;
        }
    }

    public void setNameAsLink(boolean nameAsLink) {
        this.nameAsLink = nameAsLink;
    }

    public int getResourceId() {
        if (resourceId == null) {
            Integer ret = FacesComponentUtility.getExpressionAttribute(this, RESOURCE_ID_ATTRIBUTE, Integer.class);
            return ret == null ? 0 : ret;
        } else {
            return resourceId;
        }
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }
}
