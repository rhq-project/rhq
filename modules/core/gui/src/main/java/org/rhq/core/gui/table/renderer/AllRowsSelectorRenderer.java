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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.gui.table.component.AllRowsSelectorComponent;
import org.rhq.core.gui.table.component.RowSelectorComponent;
import org.rhq.core.gui.util.FacesComponentUtility;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.util.List;

/**
 * An HTML renderer for {@link org.rhq.core.gui.table.component.AllRowsSelectorComponent}s (i.e. rhq:allRowsSelector).
 *
 * @author Ian Springer
 */
public class AllRowsSelectorRenderer extends AbstractRenderer {
    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        validateParameters(context, component);

        AllRowsSelectorComponent allRowsSelector = (AllRowsSelectorComponent) component;

        UIData data = getTargetUIData(allRowsSelector);
        RowSelectorComponent rowSelector = getRowSelector(data);
        if (rowSelector.getMode() != RowSelectorComponent.Mode.multi) {
            log.error("An allRowsSelector component was specified for dataTable component " + data
                    + ", which has a rowSelector with mode 'single', "
                    + "but allRowsSelector components can only be used with rowSelectors with mode 'multi'. "
                    + "The allRowSelector will not be rendered.");
            allRowsSelector.setRendered(false);
            return;
        }

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("input", component);

        writeIdAttributeIfNecessary(context, writer, component);

        String type = "checkbox";
        writer.writeAttribute("type", type, "type");

        String clientId = component.getClientId(context);
        writer.writeAttribute("name", clientId, "clientId");

        // TODO: Write 'checked' attribute to allow checkbox to be selected by default? Probably overkill.

        String onclick = "selectAll(this, '" + rowSelector.getClientId(context) + "')";
        String userSpecifiedOnclick = (String) rowSelector.getAttributes().get("onclick");
        if (userSpecifiedOnclick != null) {
            onclick += "; " + userSpecifiedOnclick;
        }
        rowSelector.getAttributes().put("onclick", onclick);
        writer.writeAttribute("onclick", onclick, "onclick");
        // TODO: Add support for all the other common HTML attributes.
        //RenderKitUtils.renderPassThruAttributes(writer, component, ATTRIBUTES);

        Boolean disabled = (Boolean) rowSelector.getAttributes().get("disabled");
        if (disabled != null && disabled) {
            writer.writeAttribute("disabled", "disabled", "disabled");
        }
        //RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);

        writer.endElement("input");
    }

    private RowSelectorComponent getRowSelector(UIData data) {
        List<RowSelectorComponent> rowSelectors = FacesComponentUtility.getDescendantsOfType(data,
                RowSelectorComponent.class);
        if (rowSelectors.isEmpty()) {
            throw new IllegalStateException("No rowSelector component was found within the target dataTable component " + data + ".");
        }
        if (rowSelectors.size() > 1) {
            throw new IllegalStateException("More than one rowSelector component was found within the target dataTable component " + data + ".");
        }
        RowSelectorComponent rowSelector = rowSelectors.get(0);
        return rowSelector;
    }

    private UIData getTargetUIData(AllRowsSelectorComponent allRowsSelector) {
        UIData data;
        String dataTableId = allRowsSelector.getDataTableId();
        if (dataTableId != null) {
            data = (UIData) allRowsSelector.findComponent(dataTableId);
            if (data == null) {
                throw new IllegalStateException("UIData component (i.e. h:dataTable or rich:*dataTable) with id '" + dataTableId + "' not found within naming scope of component "
                        + allRowsSelector + ". The allRowsSelector component must either be within a UIData component or must specify the id of a UIData component within its naming scope via the 'dataTableId' attribute.");
            }
        } else {
            data = FacesComponentUtility.getAncestorOfType(allRowsSelector, UIData.class);
            if (data == null) {
                throw new IllegalStateException("Enclosing UIData component (i.e. h:dataTable or rich:*dataTable) not found for component "
                        + allRowsSelector + ". The allRowsSelector component must either be within a UIData component or must specify the id of a UIData component within its naming scope via the 'dataTableId' attribute.");
            }
        }
        return data;
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        validateParameters(context, component);

        ResponseWriter writer = context.getResponseWriter();
        writer.write("</input>");
    }

    private void validateParameters(FacesContext context, UIComponent component) {
        if (context == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "context"));
        }

        if (component == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "component"));
        }
    }
}