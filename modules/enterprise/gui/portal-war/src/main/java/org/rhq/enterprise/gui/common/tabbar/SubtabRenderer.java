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
package org.rhq.enterprise.gui.common.tabbar;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.UrlUtility;

/**
 * A renderer that renders a {@link SubtabComponent} component as XHTML.
 *
 * @author Ian Springer
 */
public class SubtabRenderer extends Renderer {
    static final String IMAGES_PATH = "/images";
    static final int SUBTAB_IMAGE_WIDTH = 104;
    static final int SUBTAB_IMAGE_HEIGHT = 25;

    /**
     * Encode this component.
     *
     * @param facesContext <code>FacesContext</code> for the current request
     * @param component    <code>SubtabComponent</code> to be encoded
     */
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        SubtabComponent subtab = (SubtabComponent) component;
        processAttributes(subtab);

        TabComponent parentTab = (TabComponent) subtab.getParent();
        if (!parentTab.isSelected()) {
            // Subtabs for tabs other than the selected tab do not need to rendered.
            return;
        }

        // Process f:param child tags...
        subtab.setParameters(FacesComponentUtility.getParameters(subtab));

        StringWriter stringWriter = new StringWriter();

        //ResponseWriter writer = facesContext.getResponseWriter().cloneWithWriter(stringWriter);
        ResponseWriter writer = new HtmlResponseWriter(stringWriter);
        writer.startElement("td", subtab);
        if (!subtab.isSelected()) {
            writer.startElement("a", subtab);
            String url = buildURL(subtab);
            writer.writeAttribute("href", url, "url");
        }

        writer.startElement("img", subtab);
        String imageBasePath = IMAGES_PATH + "/Sub" + parentTab.getName() + "_" + subtab.getName();
        String imageQualifier = (subtab.isSelected()) ? "on" : "off";
        String imageURL = imageBasePath + "_" + imageQualifier + ".gif";
        writer.writeAttribute("src", imageURL, null);
        if (!subtab.isSelected()) {
            writer.writeAttribute("onmouseover", "imageSwap(this, '" + imageBasePath + "', '_over')", null);
            writer.writeAttribute("onmouseout", "imageSwap(this, '" + imageBasePath + "', '_off')", null);
        }

        writer.writeAttribute("alt", (subtab.getAlt() != null) ? subtab.getAlt() : "", "alt");
        writer.writeAttribute("width", SUBTAB_IMAGE_WIDTH, null);
        writer.writeAttribute("height", SUBTAB_IMAGE_HEIGHT, null);
        writer.writeAttribute("border", 0, null);
        writer.endElement("img");
        if (!subtab.isSelected()) {
            writer.endElement("a");
        }

        writer.endElement("td");
        subtab.setRendererOutput(stringWriter.toString());

        // NOTE: The TabBarRenderer will take care of writing our output to the response at a bit later, at the
        //       appropriate time.
        stringWriter.close();
    }

    private void processAttributes(SubtabComponent subtab) {
        if (subtab.getName() == null) {
            throw new IllegalStateException("The subtab element requires a 'name' attribute.");
        }

        if (subtab.getUrl() == null) {
            throw new IllegalStateException("The subtab element requires a 'url' attribute.");
        }
    }

    private String buildURL(SubtabComponent subtab) {
        String url = subtab.getUrl();

        // Create a master list of params from our params and the params of our enclosing tab/tabBar, in that order of
        // precedence.
        TabBarComponent tabBar = (TabBarComponent) subtab.getParent().getParent();
        Map<String, String> parameters = new LinkedHashMap<String, String>(tabBar.getParameters());
        TabComponent tab = (TabComponent) subtab.getParent();
        parameters.putAll(tab.getParameters());
        parameters.putAll(subtab.getParameters());

        url = UrlUtility.addParametersToQueryString(url, parameters);

        // Session-encode the URL in case the client doesn't have cookies enabled.
        url = FacesContext.getCurrentInstance().getExternalContext().encodeResourceURL(url);

        return url;
    }
}