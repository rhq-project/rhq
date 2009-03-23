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
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * A renderer that renders a {@link TabBarComponent} component as XHTML.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
public class TabBarRenderer extends Renderer {
    /**
     * Encode the beginning of this component.
     *
     * @param facesContext <code>FacesContext</code> for the current request
     * @param component    <code>TabBarComponent</code> to be encoded
     */
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        TabBarComponent tabBar = (TabBarComponent) component;
        processAttributes(tabBar);
        if (tabBar.getTabs().isEmpty()) {
            throw new IllegalStateException("The 'tabBar' element requires at least one 'tab' child element.");
        }

        // Process f:param child tags...
        tabBar.setParameters(FacesComponentUtility.getParameters(tabBar));

        ResponseWriter writer = facesContext.getResponseWriter();

        // <table width="100%" border="0" cellspacing="0" cellpadding="0">
        writer.startElement("table", tabBar);
        writer.writeAttribute("width", "100%", null);
        writer.writeAttribute("border", "0", null);
        writer.writeAttribute("border-spacing", "0", null);
        writer.writeAttribute("cellspacing", "0", null);
        writer.writeAttribute("cellpadding", "0", null);

        writer.startElement("tr", tabBar);
        writeCSSEdgeSpacerCell(writer, tabBar, true, false);
    }

    /**
     * Encode the ending of this component.
     *
     * @param facesContext <code>FacesContext</code> for the current request
     * @param component    <code>TabBarComponent</code> to be encoded
     */
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        TabBarComponent tabBar = (TabBarComponent) component;
        ResponseWriter writer = facesContext.getResponseWriter();

        // Add a spacer cell to fill up any remaining horizontal space remaining in the row of tabs.
        writeCSSEdgeSpacerCell(writer, tabBar, false, false);

        writer.endElement("tr");
        writeSubTabs(writer, tabBar);
        writer.endElement("tr");
        writer.endElement("table");
    }

    private void processAttributes(TabBarComponent tabBar) {
        if (tabBar.getSelectedTabName() == null) {
            throw new IllegalStateException("The 'tabBar' element requires a 'selectedTabName' attribute.");
        }

        tabBar.selectTab(tabBar.getSelectedTabName());
    }

    private void writeSubTabs(ResponseWriter writer, TabBarComponent tabBar) throws IOException {
        writer.startElement("tr", tabBar);

        // <td colspan="x">
        writer.startElement("td", tabBar);

        int tabs = 1; // the spacer
        for (TabComponent tab : tabBar.getTabs()) {
            if (tab.isRendered()) {
                tabs++;
            }
        }

        writer.writeAttribute("colspan", tabs * 2, null);

        // <table width="100%" border="0" cellspacing="0" cellpadding="0">
        writer.startElement("table", tabBar);
        writer.writeAttribute("class", "subtab-bar", null);
        writer.writeAttribute("width", "100%", null);
        writer.writeAttribute("border", "0", null);
        writer.writeAttribute("cellspacing", "0", null);
        writer.writeAttribute("cellpadding", "0", null);

        writer.startElement("tr", tabBar);
        writeCSSEdgeSpacerCell(writer, tabBar, true, true);

        // Write out the actual subtabs, which already rendered themselves earlier.
        List<SubtabComponent> subtabs = tabBar.getSelectedTab().getSubtabs();
        for (SubtabComponent subtab : subtabs) {
            if (subtab.getRendererOutput() == null) {
                throw new IllegalStateException("Subtabs for selected tab '" + tabBar.getSelectedTab().getName()
                    + "' were not rendered - the tab is most likely not legitimate for the current Resource or Group.");
            }

            writer.write(subtab.getRendererOutput());
        }

        // Add a spacer cell to fill up any remaining horizontal space remaining in the row of subtabs.
        writeCSSEdgeSpacerCell(writer, tabBar, false, true);

        writer.endElement("tr");
        writer.endElement("table");
        writer.endElement("td");
    }

    private void writeCSSEdgeSpacerCell(ResponseWriter writer, TabBarComponent tabBar, boolean isLeft, boolean isSub)
        throws IOException {
        writer.startElement("td", tabBar);
        writer.writeAttribute("style", "vertical-align: bottom;", null);
        if (!isLeft) { // last column takes up remainder of width
            writer.writeAttribute("width", "100%", null);
        }

        writer.startElement("div", tabBar);
        String prefix = (isSub) ? "sub" : "";
        String styleClass = prefix + "tab-spacer";

        writer.writeAttribute("class", styleClass, null);
        writer.write("."); // won't see this because text color will match background color
        writer.endElement("div");

        writer.endElement("td");
    }
}