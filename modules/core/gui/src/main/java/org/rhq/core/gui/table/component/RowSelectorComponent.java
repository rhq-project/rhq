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
package org.rhq.core.gui.table.component;

import org.ajax4jsf.component.UIDataAdaptor;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;

import javax.el.ValueExpression;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIData;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

/**
 * An HTML component that allows rows in data tables (i.e. h:dataTables or rich:*dataTables) to be selected via either
 * checkboxes (if in multi-select mode) or via radio buttons (if in single-select mode).
 *
 * NOTE: It is preferred to use the disabled attribute of this component instead of the rendered attribute. This will
 * provide consistency to the rendered table such as regardless of what items appear in the table, the check-able
 * column will always be rendered with a consistent width.
 *
 * @author Ian Springer
 */
public class RowSelectorComponent extends UIInput {
    private Mode mode;

    public static final String COMPONENT_TYPE = "org.rhq.RowSelector";
    public static final String COMPONENT_FAMILY = "org.rhq.RowSelector";

    private static final Mode DEFAULT_MODE = Mode.multi;

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public Mode getMode() {
        if (this.mode != null) {
            return this.mode;
        }
        Mode mode = (Mode) getBinding("mode");
        this.mode = (mode != null) ? mode : DEFAULT_MODE;
        return this.mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
    @Override
    public Object getValue() {
        if (getValueExpression("value") == null) {
            throw new IllegalStateException("rowSelector 'value' attribute must be set to an EL expression.");
        }
        return super.getValue();
    }

    /**
     * By default, getClientId() would return something like
     * "myForm:myDataTable:10001:myRowSelector", where "myForm:myDataTable:10001" is the client id of our enclosing
     * data table, and "10001" is the index of the current row. For each row, we want ourselves to be rendered as a
     * HTML checkbox with the *same* "name" attribute as the checkboxes for the other rows, so it is easier to
     * obtain the set of checkboxes via JavaScript. Since the client id is used as the checkbox "name" attribute, we
     * need to make sure to return a client id that does not include the rowIndex
     * (e.g. "myForm:myDataTable:myRowSelector". Isn't JSF fun?
     * 
     * @param context the JSF context
     * @return our client id
     */
    @Override
    public String getClientId(FacesContext context) {
        // Get our enclosing data table.
        UIData data = FacesComponentUtility.getAncestorOfType(this, UIData.class);

        String baseClientId;        
        if (data instanceof UIDataAdaptor) {
            // It's a RichFaces data table, which conveniently provides a public method that returns its base client id.
            UIDataAdaptor dataAdaptor = (UIDataAdaptor) data;
            baseClientId = dataAdaptor.getBaseClientId(context);            
        } else {
            // It's not a RichFaces data table.
            int originalRowIndex = data.getRowIndex();
            // Temporarily set its row index to -1 to force it to return a client id for itself that doesn't include the
            // row index.
            data.setRowIndex(-1);
            baseClientId = data.getClientId(context);
            data.setRowIndex(originalRowIndex);
        }

        // Now append our id.
        String id = getId();
        if (id == null) {
            id = context.getViewRoot().createUniqueId();
            setId(id);
        }
        return baseClientId + NamingContainer.SEPARATOR_CHAR + id; 
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = this.mode;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        this.mode = (Mode) values[1];
    }

    public Object getBinding(String attr) {
        if (attr == null) {
            throw new NullPointerException("passed attribute is null");
        }

        ValueExpression valueExpression = this.getValueExpression(attr);
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        Object attribValue = (valueExpression != null) ? FacesExpressionUtility.getValue(valueExpression, Object.class)
            : null;
        return attribValue;
    }

    public enum Mode {
        /**
         * Only one row can be selected at a time.
         */
        single,
        /**
         * Multiple rows can be selected simultaneously.
         */
        multi
    }
}
