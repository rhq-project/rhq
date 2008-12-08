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
package org.rhq.enterprise.gui.common.sorting;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIGraphic;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.sun.faces.renderkit.html_basic.CommandLinkRenderer;

import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

public class SortableColumnHeaderRenderer extends CommandLinkRenderer {
    // default impl should work just fine
    public SortableColumnHeaderRenderer() {
        super();
    }

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        super.encodeEnd(context, component);
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        super.encodeChildren(context, component);
    }

    /* easy trick to override and put addition attributes in the writer */
    @Override
    protected void writeValue(UIComponent component, ResponseWriter writer) throws IOException {
        String sort = ((SortableColumnHeader) component).getSort();
        if (sort == null) {
            sort = "FORGOT_TO_ADD_SORT_ATTRIBUTE";
        }

        writer.writeAttribute("sort", sort, "sort");

        // remove previous sort images
        List<UIComponent> children = component.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            // should only be one, but let's loop-remove just in case
            if (children.get(i) instanceof UIGraphic) {
                children.remove(i);
            }

            if (children.get(i) instanceof UIOutput) {
                String value = ((UIOutput) children.get(i)).getValue().toString();
                if (value.length() > 0) {
                    if (Character.isDigit(value.charAt(0))) {
                        // also remove sort index labels, they will be reconstructed below
                        children.remove(i);
                    }
                }
            }
        }

        PageControl pc = getPageControl(component);
        // changing the sort on the column

        int i = 0;
        for (OrderingField field : pc.getOrderingFields()) {
            i++;
            if (field.getField().equals(sort)) {
                UIGraphic image = new UIGraphic();
                if (field.getOrdering().equals(PageOrdering.ASC)) {
                    if (i == 1) {
                        image.setUrl("/images/tb_sortup.gif");
                    } else {
                        image.setUrl("/images/tb_sortup_inactive.gif");
                    }
                } else {
                    if (i == 1) {
                        image.setUrl("/images/tb_sortdown.gif");
                    } else {
                        image.setUrl("/images/tb_sortdown_inactive.gif");
                    }
                }

                /*
                 * add the image at the end of all components, which will make it into part of the hyperlink
                 */
                children.add(image);

                /*
                 * also, add a tiny number to indicate what order the columns are sorted in
                 */
                UIOutput label = new HtmlOutputLabel();
                label.setValue(String.valueOf(i));
                children.add(label);

                break;
            }
        }

        super.writeValue(component, writer);
    }

    private PageControl getPageControl(UIComponent component) {
        UIData enclosingTable = getEnclosingUIData(component);

        UIComponent facet = enclosingTable.getFacet("PageControlView");
        String viewName = facet.getId();

        PageControlView currentView = PageControlView.valueOf(viewName);
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences preferences = user.getWebPreferences();

        return preferences.getPageControl(currentView);
    }

    private UIData getEnclosingUIData(UIComponent component) {
        UIComponent next = component;
        while (next.getParent() != null) {
            next = next.getParent();
            if (next instanceof UIData) {
                return (UIData) next;
            }
        }

        return null;
    }
}