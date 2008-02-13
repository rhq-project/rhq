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
package org.rhq.enterprise.gui.inventory.resource;

import java.io.IOException;
import java.util.List;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A renderer that renders a {@link ResourceLineageComponent} component as XHTML.
 *
 * @author Ian Springer
 */
public class ResourceLineageRenderer extends Renderer {
    private static final String BASE_RESOURCE_URL = "/jon/resource/inventory/view.xhtml";
    private static final String SEPARATOR = " > ";

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    /**
     * Encode the beginning of the given {@link ResourceLineageComponent}.
     *
     * @param facesContext the JSF context for the current request
     * @param component    the {@link ResourceLineageComponent} to be encoded
     */
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        ResourceLineageComponent resourceLineage = (ResourceLineageComponent) component;
        ResponseWriter writer = facesContext.getResponseWriter();
        List<Resource> ancestorResources = this.resourceManager.getResourceLineage(resourceLineage.getResourceId());
        if (ancestorResources.isEmpty()) {
            throw new IllegalStateException(
                "The list of ancestor resources should always contain at least one resource - the resource whose lineage was requested.");
        }

        Resource parentResource = ancestorResources.get(ancestorResources.size() - 1);
        for (Resource ancestorResource : ancestorResources) {
            writer.startElement("a", resourceLineage);
            writer.writeAttribute("href", buildURL(ancestorResource), null);
            writer.writeText(ancestorResource.getName(), null);
            writer.endElement("a");
            if (ancestorResource.getId() != parentResource.getId()) // separator after every item except the last one
            {
                writer.writeText(SEPARATOR, null);
            }
        }
    }

    private String buildURL(Resource resource) {
        String url = BASE_RESOURCE_URL + "?id=" + resource.getId();

        // Session-encode the URL in case the client doesn't have cookies enabled.
        url = FacesContext.getCurrentInstance().getExternalContext().encodeResourceURL(url);
        return url;
    }
}