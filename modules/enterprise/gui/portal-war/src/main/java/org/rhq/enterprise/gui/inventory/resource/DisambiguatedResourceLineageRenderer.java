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
import java.util.Iterator;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.rhq.core.domain.resource.composite.DisambiguationReport;

/**
 * Renderer for {@link DisambiguatedResourceLineageComponent}
 * 
 * @author Lukas Krejci
 */
public class DisambiguatedResourceLineageRenderer extends Renderer {
    private static final String RESOURCE_URL = "/rhq/resource/summary/summary.xhtml";
    
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        DisambiguatedResourceLineageComponent lineageComponent = (DisambiguatedResourceLineageComponent) component;
        
        String separator = lineageComponent.getSeparator();
        List<DisambiguationReport.Resource> parents = lineageComponent.getParents();
        boolean renderLinks = lineageComponent.getRenderLinks();
        
        if (parents != null && parents.size() > 0) {
            ResponseWriter writer = context.getResponseWriter();
            
            Iterator<DisambiguationReport.Resource> parentsIt = parents.iterator();
            
            if (renderLinks) {
                encodeUrl(writer, parentsIt.next());
                while(parentsIt.hasNext()) {
                    writer.writeText(separator, null);
                    encodeUrl(writer, parentsIt.next());
                }
            } else {
                encodeSimple(writer, parentsIt.next());
                while(parentsIt.hasNext()) {
                    writer.writeText(separator, null);
                    encodeSimple(writer, parentsIt.next());
                }
            }
        }
    }
    
    public static void encodeUrl(ResponseWriter writer, DisambiguationReport.Resource parent) throws IOException {
        encodePreName(writer, parent);
        writer.startElement("a", null);
        writer.writeAttribute("href", getUrl(parent), null);
        writeName(writer, parent);
        writer.endElement("a");
        encodePostName(writer, parent);
    }
    
    public static void encodeSimple(ResponseWriter writer, DisambiguationReport.Resource parent) throws IOException {
        encodePreName(writer, parent);
        writeName(writer, parent);
        encodePostName(writer, parent);
    }
    
    private static String getUrl(DisambiguationReport.Resource parent) {
        return RESOURCE_URL + "?id=" + parent.getId();
    }
    
    private static void encodePreName(ResponseWriter writer, DisambiguationReport.Resource parent) throws IOException {
        if (!parent.getType().isSingleton()) {
            writer.startElement("span", null);
            writer.writeAttribute("class", "disambiguated-resource-type", null);
            writer.writeText(parent.getType().getName(), null);
            writer.writeText(" ", null);
            if (parent.getType().getPlugin() != null) {
                writer.startElement("span", null);
                writer.writeAttribute("class", "disambiguated-resource-plugin", null);
                writer.writeText("(", null);
                writer.writeText(parent.getType().getPlugin(), null);
                writer.writeText(" plugin) ", null);
                writer.endElement("span");
            }
            writer.endElement("span");
        }
    }

    private static void encodePostName(ResponseWriter writer, DisambiguationReport.Resource parent) throws IOException {
        if (parent.getType().isSingleton() && parent.getType().getPlugin() != null) {
            writer.startElement("span", null);
            writer.writeAttribute("class", "disambiguated-resource-plugin", null);
            writer.writeText(" (", null);
            writer.writeText(parent.getType().getPlugin(), null);
            writer.writeText(" plugin) ", null);
            writer.endElement("span");
        }
    }
    
    private static void writeName(ResponseWriter writer, DisambiguationReport.Resource parent) throws IOException {
        writer.startElement("span", null);
        writer.writeAttribute("class", "disambiguated-resource-name", null);
        writer.writeText(parent.getName(), null);
        writer.endElement("span");
    }
}
