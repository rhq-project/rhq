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

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.rhq.core.domain.resource.composite.DisambiguationReport;

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceNameRenderer extends Renderer {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        DisambiguatedResourceNameComponent nameComponent = (DisambiguatedResourceNameComponent) component;
        
        ResponseWriter writer = context.getResponseWriter();
        
        DisambiguationReport<?> report = nameComponent.getDisambiguationReport();
        String resourceName = nameComponent.getResourceName();
        int resourceId = nameComponent.getResourceId();
        
        DisambiguationReport.ResourceType resourceType = report.getResourceType();
        
        DisambiguationReport.Resource resource = new DisambiguationReport.Resource(resourceId, resourceName, resourceType);
        
        if (nameComponent.getNameAsLink()) {
            DisambiguatedResourceLineageRenderer.encodeUrl(writer, resource);
        } else {
            DisambiguatedResourceLineageRenderer.encodeSimple(writer, resource);
        }        
    }
}
