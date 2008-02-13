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
 */
public class TabBarRenderer extends Renderer {
    private static final String TAB_SPACER_CELL_STYLE_CLASS = "TabCell";
    private static final String SUBTAB_SPACER_CELL_STYLE_CLASS = "SubTabCell";

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
        writer.writeAttribute("cellspacing", "0", null);
        writer.writeAttribute("cellpadding", "0", null);

        writer.startElement("tr", tabBar);
        writeSpacerCell(writer, tabBar, null, TAB_SPACER_CELL_STYLE_CLASS, 20, 1);
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
        writeSpacerCell(writer, tabBar, "100%", TAB_SPACER_CELL_STYLE_CLASS, 1, 1);

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
        writer.writeAttribute("colspan", tabBar.getTabs().size() + 2, null);

        // <table width="100%" border="0" cellspacing="0" cellpadding="0">
        writer.startElement("table", tabBar);
        writer.writeAttribute("width", "100%", null);
        writer.writeAttribute("border", "0", null);
        writer.writeAttribute("cellspacing", "0", null);
        writer.writeAttribute("cellpadding", "0", null);

        writer.startElement("tr", tabBar);
        writeSpacerCell(writer, tabBar, null, SUBTAB_SPACER_CELL_STYLE_CLASS, 5, SubtabRenderer.SUBTAB_IMAGE_HEIGHT);

        // Write out the actual subtabs, which already rendered themselves earlier.
        List<SubtabComponent> subtabs = tabBar.getSelectedTab().getSubtabs();
        for (SubtabComponent subtab : subtabs) {
            if (subtab.getRendererOutput() == null) {
                throw new IllegalStateException("Subtabs for selected tab '" + tabBar.getSelectedTab().getName()
                    + "' were not rendered - the tab is most likely not legitimate for the current resource.");
            }

            writer.write(subtab.getRendererOutput());
        }

        // Add a spacer cell to fill up any remaining horizontal space remaining in the row of subtabs.
        writeSpacerCell(writer, tabBar, "100%", SUBTAB_SPACER_CELL_STYLE_CLASS, 1, SubtabRenderer.SUBTAB_IMAGE_HEIGHT);

        writer.endElement("tr");
        writer.endElement("table");
        writer.endElement("td");
    }

    private void writeSpacerCell(ResponseWriter writer, TabBarComponent tabBar, String cellWidth, String styleClass,
        int width, int height) throws IOException {
        writer.startElement("td", tabBar);
        if (cellWidth != null) {
            writer.writeAttribute("width", cellWidth, null);
        }

        writer.writeAttribute("class", styleClass, null);

        // <img src="/images/spacer.gif" width="x" height="1" alt="" border="0"/>
        writer.startElement("img", tabBar);
        writer.writeAttribute("src", "/images/spacer.gif", null);
        writer.writeAttribute("width", width, null);
        writer.writeAttribute("height", height, null);
        writer.writeAttribute("alt", "", null);
        writer.writeAttribute("border", "0", null);
        writer.endElement("img");

        writer.endElement("td");
    }
}