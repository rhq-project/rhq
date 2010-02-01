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

import com.sun.faces.renderkit.html_basic.CommandLinkRenderer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.table.component.SortableColumnHeaderComponent;
import org.rhq.core.gui.table.model.PagedListDataModel;
import org.rhq.core.gui.util.FacesComponentUtility;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

/**
 * @author Ian Springer
 * @author Joseph Marques 
 */
public class SortableColumnHeaderRenderer  extends CommandLinkRenderer {
    private static final String BASE_IMAGE_URL = "http://127.0.0.1:7080/images";

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        rendererParamsNotNull(context, component);

        String sortBy = ((SortableColumnHeaderComponent) component).getSortBy();
        if (sortBy == null) {
            log.error("Required attribute 'sortBy' missing for " + component.getClass().getSimpleName()
                    + " component with id" + component.getId() + " on page "
                    + FacesContext.getCurrentInstance().getViewRoot().getViewId() + ".");
            return;
        }

        ResponseWriter writer = context.getResponseWriter();

        PageControl pageControl = getPageControl(component);
        boolean sortFieldFound = false;
        for (int i = 0, orderingFieldsSize = pageControl.getOrderingFields().size(); i < orderingFieldsSize; i++) {
            OrderingField field = pageControl.getOrderingFields().get(i);
            if (field.getField().equals(sortBy)) {
                sortFieldFound = true;
                writer.startElement("img", component);
                String imageFileName = null;
                switch (field.getOrdering()) {
                    case ASC:
                        imageFileName = (i == 0) ? "tb_sortup.gif" : "tb_sortup_inactive.gif";
                        break;
                    case DESC:
                        imageFileName = (i == 0) ? "tb_sortdown.gif" : "tb_sortdown_inactive.gif";
                        break;
                }
                String imageUrl = BASE_IMAGE_URL + "/" + imageFileName;
                writer.writeAttribute("src", imageUrl, null);
                writer.writeAttribute("border", 0, null);
                writer.endElement("img");

                // Add a tiny number to indicate the column's sort precedence, "1" being the highest precedence.
                Integer precedence = i + 1;
                writer.writeText(precedence, null);

                break;
            }
        }

        if (!sortFieldFound) {
            log.error("Value of attribute 'sortBy' for " + component.getClass().getSimpleName()
                    + " component with id" + component.getId() + " on page "
                    + FacesContext.getCurrentInstance().getViewRoot().getViewId() + " not valid for " + pageControl
                    + ".");
        }
        super.encodeEnd(context, component);
    }

    private PageControl getPageControl(UIComponent component) {
        UIData enclosingTable = getEnclosingData(component);
        PagedListDataModel pagedListDataModel = (PagedListDataModel)enclosingTable.getValue();
        return pagedListDataModel.getPageControl();
    }

    private UIData getEnclosingData(UIComponent component) {
        UIData data = FacesComponentUtility.getAncestorOfType(component, UIData.class);
        if (data == null) {
            throw new IllegalStateException("No data!");
        }
        return data;
    }
}