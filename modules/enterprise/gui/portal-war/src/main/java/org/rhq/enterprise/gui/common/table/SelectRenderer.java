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
package org.rhq.enterprise.gui.common.table;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import com.sun.faces.util.MessageUtils;

public class SelectRenderer extends Renderer {
    /*
     * In hindsight, this method is probably useless because there is currently no state to deserialize.  The AllSelect
     * component and the Select component are both completely stateless; in other words, if you move to another page of
     * data or click on one of the table columns to change the sort, none of the currently checked items will remain
     * checked.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void decode(FacesContext context, UIComponent component) {
        if (context == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "context"));
        }

        if (component == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "component"));
        }

        Select select = null;
        if (component instanceof Select) {
            select = (Select) component;
        } else {
            return;
        }

        Map map = context.getExternalContext().getRequestParameterMap();
        String name = select.getName();
        if (map.containsKey(name)) {
            String value = (String) map.get(name);
            if ((value != null) && (component instanceof UIInput)) {
                ((UIInput) component).setSubmittedValue(value);
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        if (context == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "context"));
        }

        if (component == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "component"));
        }

        Select select = (Select) component;

        if (component.isRendered()) {
            ResponseWriter writer = context.getResponseWriter();

            writer.write("<input ");
            boolean isRadio = (select.getType() != null) && select.getType().equalsIgnoreCase("radio");
            if (isRadio) {
                writer.write("type=\"radio\"");
            } else {
                writer.write("type=\"checkbox\"");
            }

            writer.write(" id=\"" + component.getClientId(context) + "\"");
            writer.write(" name=\"" + select.getName() + "\"");

            /*
             * if (aUICustomSelectOneRadio.getStyleClass() != null   &&
             * aUICustomSelectOneRadio.getStyleClass().trim().length() > 0) { writer.write(" class=\"" +
             * aUICustomSelectOneRadio.getStyleClass().trim() + "\""); } if (aUICustomSelectOneRadio.getStyle() != null
             * && aUICustomSelectOneRadio.getStyle().trim().length() > 0) { writer.write(" style=\"" +
             * aUICustomSelectOneRadio.getStyle().trim() + "\""); }
             */

            if (select.getValue() != null) {
                writer.write(" value=\"" + select.getValue() + "\"");
            }

            if ((select.getOnclick() != null) && (select.getOnclick().length() > 0)) {
                writer.write(" onclick=\"" + select.getOnclick() + "\"");
            }

            if ((select.getOnchange() != null) && (select.getOnchange().length() > 0)) {
                writer.write(" onchange=\"" + select.getOnchange() + "\"");
            }

            if (select.isDisabled()) {
                writer.write(" disabled=\"disabled\"");
            }

            writer.write(">");
        }
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        Select select = (Select) component;

        if (component.isRendered()) {
            ResponseWriter writer = context.getResponseWriter();

            if (select.getLabel() != null) {
                writer.write(select.getLabel());
            }
        }

        for (UIComponent child : component.getChildren()) {
            child.encodeAll(context);
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        if (component.isRendered()) {
            ResponseWriter writer = context.getResponseWriter();

            writer.write("</input>");
        }
    }
}