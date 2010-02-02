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

import org.rhq.core.gui.util.FacesExpressionUtility;

import javax.el.ValueExpression;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

/**
 * An HTML component that allows rows in rich:*dataTables to be selected via either checkboxes (if in multi-select mode)
 * or via radio buttons (if in single-select mode).
 *
 * NOTE: It is preferred to use the disabled attribute of this component instead of the rendered attribute. This will
 * provide consistency to the rendered table such as regardless of what items appear in the table, the check-able
 * column will always be rendered with a consistent width.
 *
 * @author Ian Springer
 */
public class AllRowsSelectorComponent extends UIInput {
    private String dataTableId;

    public static final String COMPONENT_TYPE = "org.rhq.AllRowsSelector";
    public static final String COMPONENT_FAMILY = "org.rhq.AllRowsSelector";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public String getDataTableId() {
        if (this.dataTableId != null) {
            return this.dataTableId;
        }
        this.dataTableId = (String) getBinding("dataTableId");
        return this.dataTableId;
    }

    public void setDataTableId(String dataTableId) {
        this.dataTableId = dataTableId;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = this.dataTableId;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        this.dataTableId = (String) values[1];
    }

    public Object getBinding(String attr) {
        if (attr == null) {
            throw new NullPointerException("passed attribute is null");
        }

        ValueExpression valueExpression = this.getValueExpression(attr);
        Object attribValue = (valueExpression != null) ? FacesExpressionUtility.getValue(valueExpression, Object.class)
            : null;
        return attribValue;
    }
}