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

import org.jetbrains.annotations.NotNull;
import org.rhq.core.gui.table.component.RowSelectorComponent;
import org.rhq.core.gui.table.component.SelectedRowsCommandButtonComponent;
import org.rhq.core.gui.util.FacesComponentUtility;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.util.List;

/**
 * An HTML renderer for {@link org.rhq.core.gui.table.component.RowSelectorComponent}s (i.e. rhq:rowSelector).
 *
 * @author Ian Springer
 */
public class SelectedRowsCommandButtonRenderer extends AbstractButtonRenderer {
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);

        SelectedRowsCommandButtonComponent selectedRowsCommandButton = (SelectedRowsCommandButtonComponent) component;

        ResponseWriter writer = context.getResponseWriter();

        Integer minimum = selectedRowsCommandButton.getMinimum();
        writer.writeAttribute("minimum", minimum, "minimum");

        Integer maximum = selectedRowsCommandButton.getMaximum();
        if (maximum != null) {
            writer.writeAttribute("maximum", maximum, "maximum");
        }

        UIData data = getTargetUIData(selectedRowsCommandButton);
        RowSelectorComponent rowSelector = getRowSelector(data);
        String rowSelectorClientId = rowSelector.getClientId(context);
        writer.writeAttribute("target", rowSelectorClientId, null);        
    }

    @Override
    protected void renderBooleanAttributes(ResponseWriter writer, UIComponent component) throws IOException {
        SelectedRowsCommandButtonComponent selectedRowsCommandButton = (SelectedRowsCommandButtonComponent) component;
        
        Integer minimum = selectedRowsCommandButton.getMinimum();
        // If the minimum is > 0, which is the typical case, render the button as disabled.
        // *NOTE* We are making the assumption that no rows are selected by default, which
        // is how the RowSelectorRenderer renders things.
        if (minimum > 0) {
            writer.writeAttribute("disabled", true, "disabled");
        }
    }

    @NotNull
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

    private UIData getTargetUIData(SelectedRowsCommandButtonComponent selectedRowsCommandButton) {
        UIData data;
        String dataTableId = selectedRowsCommandButton.getDataTableId();
        if (dataTableId != null) {
            data = (UIData) selectedRowsCommandButton.findComponent(dataTableId);
            if (data == null) {
                throw new IllegalStateException("UIData component (i.e. h:dataTable or rich:*dataTable) with id '" + dataTableId + "' not found within naming scope of component "
                        + selectedRowsCommandButton + ". The allRowsSelector component must either be within a UIData component or must specify the id of a UIData component within its naming scope via the 'dataTableId' attribute.");
            }
        } else {
            data = FacesComponentUtility.getAncestorOfType(selectedRowsCommandButton, UIData.class);
            if (data == null) {
                throw new IllegalStateException("Enclosing UIData component (i.e. h:dataTable or rich:*dataTable) not found for component "
                        + selectedRowsCommandButton + ". The allRowsSelector component must either be within a UIData component or must specify the id of a UIData component within its naming scope via the 'dataTableId' attribute.");
            }
        }
        return data;
    }
}