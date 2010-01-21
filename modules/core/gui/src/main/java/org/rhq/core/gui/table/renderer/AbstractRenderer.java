package org.rhq.core.gui.table.renderer;

import com.sun.faces.util.MessageUtils;
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
public abstract class AbstractRenderer extends Renderer {
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
