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
import javax.faces.context.FacesContext;
import com.sun.faces.util.MessageUtils;

public class AllSelectRenderer extends SelectRenderer {
    /*
     * @see SelectRenderer.decode(FacesContext, UIComponent)
     */
    public void decode(FacesContext context, UIComponent component) {
        if (context == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "context"));
        }

        if (component == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "component"));
        }

        AllSelect allSelect;
        if (component instanceof AllSelect) {
            allSelect = (AllSelect) component;
        } else {
            return;
        }

        String clientId = allSelect.getClientId(context);

        Map map = context.getExternalContext().getRequestParameterMap();
        if (map.containsKey(clientId)) {
            String newValue = (String) map.get(clientId);
            allSelect.setSubmittedValue(newValue);
        }
    }

    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);
    }

    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
    }

    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        super.encodeEnd(context, component);
    }
}