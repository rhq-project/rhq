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
package org.rhq.enterprise.gui.common.quicknav;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.UrlUtility;

/**
 * A renderer that renders an {@link IconComponent} component as XHTML.
 *
 * @author Ian Springer
 */
public class IconRenderer extends Renderer {
    private static final String QUICKNAV_CELL_STYLE_CLASS = "quicknav-cell";
    private static final String QUICKNAV_BLOCK_STYLE_CLASS = "quicknav-block";

    private static final String IMAGES_PATH = "/images";

    private static final String ICON_IMAGE_WIDTH = "16";
    private static final String ICON_IMAGE_HEIGHT = "16";

    /**
     * Encode this component.
     *
     * @param facesContext <code>FacesContext</code> for the current request
     * @param component    <code>IconComponent</code> to be encoded
     */
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        IconComponent icon = (IconComponent) component;
        processAttributes(icon);

        // Process f:param child tags...
        icon.setParameters(FacesComponentUtility.getParameters(icon));

        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement("td", icon);
        writer.writeAttribute("class", QUICKNAV_CELL_STYLE_CLASS, null);
        writer.startElement("div", icon);
        writer.writeAttribute("class", QUICKNAV_BLOCK_STYLE_CLASS, null);
        if (icon.isVisible()) {
            writer.startElement("a", icon);
            writer.writeAttribute("href", buildURL(icon), "url");
            writer.startElement("img", icon);
            String imageBasePath = IMAGES_PATH + "/icons/" + icon.getName();
            String imageURL = imageBasePath + "_grey_16.png";
            writer.writeAttribute("src", imageURL, null);
            writer.writeAttribute("alt", icon.getAlt(), "alt");
            writer.writeAttribute("title", icon.getAlt(), "alt");
            writer.writeAttribute("width", ICON_IMAGE_WIDTH, null);
            writer.writeAttribute("height", ICON_IMAGE_HEIGHT, null);
            writer.writeAttribute("border", 0, null);
            writer.endElement("img");
            writer.endElement("a");
        }

        writer.endElement("div");
        writer.endElement("td");
        writer.append("\n");
    }

    private void processAttributes(IconComponent icon) {
        if (icon.getName() == null) {
            throw new IllegalStateException("The 'icon' element requires a 'name' attribute.");
        }

        if (icon.getUrl() == null) {
            throw new IllegalStateException("The 'icon' element requires a 'url' attribute.");
        }

        // NOTE: The 'alt' attribute is required, because it is important that every icon has a tooltip that tells the
        //       user what tab it will take them to.
        if (icon.getAlt() == null) {
            throw new IllegalStateException("The 'icon' element requires a 'alt' attribute.");
        }
    }

    private String buildURL(IconComponent icon) {
        String url = icon.getUrl();

        // Create a master list of params from our params and the
        // params of our enclosing quickNav, in that order of precedence.
        QuickNavComponent quickNav = (QuickNavComponent) icon.getParent();
        Map<String, String> parameters = new LinkedHashMap<String, String>(quickNav.getParameters());
        parameters.putAll(icon.getParameters());

        url = UrlUtility.addParametersToQueryString(url, parameters);

        // Session-encode the URL in case the client doesn't have cookies enabled.
        url = FacesContext.getCurrentInstance().getExternalContext().encodeResourceURL(url);

        return url;
    }
}