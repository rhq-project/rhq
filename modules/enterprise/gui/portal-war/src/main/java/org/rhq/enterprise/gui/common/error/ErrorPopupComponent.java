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
package org.rhq.enterprise.gui.common.error;

import javax.el.ValueExpression;
import javax.faces.component.UIComponentBase;

/**
 * Backing UI component for the error popup handling.
 *
 * @author Jason Dobies
 */
public class ErrorPopupComponent extends UIComponentBase {
    // Constants  --------------------------------------------

    public static final String COMPONENT_TYPE = "org.jboss.on.ErrorPopup";
    public static final String COMPONENT_FAMILY = "org.jboss.on.ErrorPopup";

    // Attributes  --------------------------------------------

    private String popupId;
    private String errorMessage;

    // UIComponentBase Implementation  --------------------------------------------

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    // Accessors  --------------------------------------------

    public String getPopupId() {
        ValueExpression valueExp = getValueExpression("popupId");
        if (valueExp != null) {
            popupId = (String) valueExp.getValue(getFacesContext().getELContext());
        }

        return popupId;
    }

    public void setPopupId(String id) {
        this.popupId = id;
    }

    public String getErrorMessage() {
        ValueExpression valueExp = getValueExpression("errorMessage");
        if (valueExp != null) {
            errorMessage = (String) valueExp.getValue(getFacesContext().getELContext());
        }

        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}