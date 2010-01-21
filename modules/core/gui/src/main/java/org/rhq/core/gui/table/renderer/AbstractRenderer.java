/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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
package org.rhq.core.gui.table.renderer;

import com.sun.faces.util.MessageUtils;
import org.ajax4jsf.renderkit.HeaderResourceProducer2;
import org.ajax4jsf.renderkit.ProducerContext;
import org.ajax4jsf.resource.InternetResource;
import org.ajax4jsf.resource.InternetResourceBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import java.io.IOException;

/**
 *
 */
public abstract class AbstractRenderer extends Renderer implements HeaderResourceProducer2 {
	private final Log log = LogFactory.getLog(this.getClass());

    private InternetResourceBuilder resourceBuilder;

    protected void initializeComponentId(FacesContext context, UIComponent component) {
        if (component.getId() == null) {
            String id = context.getViewRoot().createUniqueId();
            component.setId(id);
        }
    }

    protected void writeIdAttributeIfNecessary(FacesContext context,
                                                 ResponseWriter writer,
                                                 UIComponent component) {
        initializeComponentId(context, component);
        if (!component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
            try {
                writer.writeAttribute("id", component.getClientId(context), "id");
            } catch (IOException e) {
                String message = MessageUtils.getExceptionMessageString
                      (MessageUtils.CANT_WRITE_ID_ATTRIBUTE_ERROR_MESSAGE_ID,
                       e.getMessage());
                log.warn(message);
            }
        }        
    }

	/**
	 * Hook method to return array of script resources to store in head.
	 */
	protected InternetResource[] getScripts() {
		return null;
	}

	/**
	 * Hook method to return array of styles resources to store in head.
	 */
	protected InternetResource[] getStyles() {
		return null;
	}

	/**
	 * @param context
	 * @param component
	 * @param resources
	 * @throws IOException
	 */
	protected void encodeResourcesArray(FacesContext context,
			UIComponent component, InternetResource[] resources)
			throws IOException {
		if (resources != null) {
            for (InternetResource resource : resources) {
                resource.encode(context, component);
            }
		}
	}

	public void encodeToHead(FacesContext context, UIComponent component, ProducerContext pc) throws IOException {
		if (pc.isProcessScripts()) {
			encodeResourcesArray(context, component, getScripts());
	    }
		if (pc.isProcessStyles()) {
			encodeResourcesArray(context, component, getStyles());
		}
	}

    /**
	 * Base stub method for produce internet resource ( image, script ... )
	 * since resources must be implemented in "lightweight" pattern, it instances
	 * put in internal map to caching.
	 * @param resourceURI - relative ( to renderer class ) uri to resource in jar or
	 * key for generate ( in Java2D , for example ).
	 * @return - resource instance for this uri.
	 * @throws org.ajax4jsf.resource.ResourceNotFoundException - if reqested resource not instantiated.
	 */
	protected InternetResource getResource(String resourceURI ) throws FacesException {
		return getResourceBuilder().createResource(this,resourceURI);
	}

	private InternetResourceBuilder getResourceBuilder(){
		if (this.resourceBuilder == null) {
			this.resourceBuilder = InternetResourceBuilder.getInstance();
		}
		return this.resourceBuilder;
	}


}
