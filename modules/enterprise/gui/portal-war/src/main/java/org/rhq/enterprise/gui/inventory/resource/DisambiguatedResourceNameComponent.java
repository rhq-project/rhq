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

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceNameComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.jboss.on.DisambiguatedResourceName";
    public static final String COMPONENT_FAMILY = "org.jboss.on.DisambiguatedResourceName";

    private DisambiguationReport<?> disambiguationReport;
    private String resourceName;
    private int resourceId;
    private boolean nameAsLink = true;
    
    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

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

    public boolean isNameAsLink() {
        return nameAsLink;
    }

    public void setNameAsLink(boolean nameAsLink) {
        this.nameAsLink = nameAsLink;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }
}
