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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.UrlUtility;

/**
 * A renderer that renders a {@link TabComponent} component as XHTML.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
public class TabRenderer extends Renderer {

    /**
     * Encode this component.
     *
     * @param facesContext <code>FacesContext</code> for the current request
     * @param component    <code>TabComponent</code> to be encoded
     */
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        TabComponent tab = (TabComponent) component;
        processAttributes(tab);

        // Process f:param child tags...
        tab.setParameters(FacesComponentUtility.getParameters(tab));

        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement("td", tab);
        writer.writeAttribute("style", "vertical-align: bottom;", null);

        if (!tab.isSelected()) {
            writer.startElement("a", tab);
            writer.writeAttribute("style", "text-decoration: none;", null);
            writer.writeAttribute("href", buildURL(tab), "url");
        }
        writer.startElement("div", tab);

        if (!tab.isSelected()) {
            writer.writeAttribute("class", "tab-inactive tab-common", null);
            writer.writeAttribute("onmouseover", "this.className='tab-hover tab-common'", null);
            writer.writeAttribute("onmouseout", "this.className='tab-inactive tab-common'", null);
        } else {
            writer.writeAttribute("class", "tab-active tab-common", null);
        }

        /* TODO: FIGURE OUT HOW TO PUT THE IMAGE TO THE LEFT OF THE TAB NAME
        String imageUrl = tab.getImage();
        if (imageUrl != null) {
            writer.startElement("img", tab);
            writer.writeAttribute("src", imageUrl, null);
            writer.endElement("img");
            writer.write(" ");
        }
        */

        if (tab.getDisplayName() != null) {
            writer.write(tab.getDisplayName().toLowerCase());
        } else {
            writer.write(tab.getName().toLowerCase());
        }

        writer.endElement("div");
        if (!tab.isSelected()) {
            writer.endElement("a");
        }

        writer.endElement("td");
        writer.startElement("td", tab);
        writer.writeAttribute("style", "vertical-align: bottom;", null);
        writer.startElement("div", tab);
        String styleClass = "tab-spacer";
        writer.writeAttribute("class", styleClass, null);
        writer.writeAttribute("style", "width: 2px;", null);
        writer.write(" "); // won't see this because text color will match background color
        writer.endElement("div");
        writer.endElement("td");
    }

    private void processAttributes(TabComponent tab) {
        if (tab.getName() == null) {
            throw new IllegalStateException("The 'tab' element requires a 'name' attribute.");
        }

        if (tab.getUrl() == null) {
            if (tab.getChildCount() == 0) {
                throw new IllegalStateException(
                    "The 'tab' element requires a 'url' attribute when it has no child 'subtab' elements.");
            }
        }
    }

    private String buildURL(TabComponent tab) {
        String url;
        SubtabComponent defaultSubtab = null;
        if (tab.getSubtabs().isEmpty()) {
            url = tab.getUrl();
        } else {
            // NOTE: If we are not the selected tab, we inherit the URL of our first (i.e. leftmost-displayed) subtab.
            defaultSubtab = tab.getDefaultSubtab();
            assert defaultSubtab != null;
            url = defaultSubtab.getUrl();
        }

        // Create a master list of params from our default subtab's params (if we have subtabs), our params, and the
        // params of our enclosing tabBar, in that order of precedence.
        TabBarComponent tabBar = (TabBarComponent) tab.getParent();
        Map<String, String> parameters = new LinkedHashMap<String, String>(tabBar.getParameters());
        parameters.putAll(tab.getParameters());
        if (defaultSubtab != null) {
            // We need to process the subtab's f:param child tags first, since the subtab's renderer hasn't had a chance to
            // do it yet!
            defaultSubtab.setParameters(FacesComponentUtility.getParameters(defaultSubtab));
            parameters.putAll(defaultSubtab.getParameters());
        }

        url = UrlUtility.addParametersToQueryString(url, parameters);

        // Session-encode the URL in case the client doesn't have cookies enabled.
        url = FacesContext.getCurrentInstance().getExternalContext().encodeResourceURL(url);

        return url;
    }
}