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

import org.jetbrains.annotations.NotNull;
import org.rhq.core.gui.util.FacesExpressionUtility;

import javax.el.ValueExpression;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;

/**
 * An HTML component that allows rows in data tables (i.e. h:dataTables or rich:*dataTables) to be selected via either
 * checkboxes (if in multi-select mode) or via radio buttons (if in single-select mode). TODO
 *
 * @author Ian Springer
 */
public class SelectedRowsCommandButtonComponent extends HtmlCommandButton {
    private String dataTableId;
    private Integer minimum;
    private Integer maximum;

    public static final String COMPONENT_TYPE = "org.rhq.SelectedRowsCommandButton";
    public static final String COMPONENT_FAMILY = "org.rhq.SelectedRowsCommandButton";

    private static final Integer DEFAULT_MINIMUM = 1;

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

    public Integer getMinimum() {
        if (this.minimum != null) {
            return this.minimum;
        }
        Integer value = (Integer) getBinding("minimum");
        this.minimum = (value != null) ? value : DEFAULT_MINIMUM;
        return this.minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Integer getMaximum() {
        if (this.maximum != null) {
            return this.maximum;
        }
        this.maximum = (Integer) getBinding("maximum");
        return this.maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = this.dataTableId;
        values[2] = this.minimum;
        values[3] = this.maximum;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        this.dataTableId = (String) values[1];
        this.minimum = (Integer) values[2];
        this.maximum = (Integer) values[3];
    }

    private Object getBinding(@NotNull String attributeName) {
        ValueExpression valueExpression = this.getValueExpression(attributeName);
        return (valueExpression != null) ? FacesExpressionUtility.getValue(valueExpression, Object.class) : null;
    }    
}