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

import javax.el.ValueExpression;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import org.rhq.core.gui.util.FacesExpressionUtility;

public class Select extends UIInput {
    private String name;
    private String type;
    private String label;
    private Object value;
    private Boolean disabled;
    private String onchange;

    public static final String COMPONENT_TYPE = "org.jboss.on.Select";
    public static final String COMPONENT_FAMILY = "org.jboss.on.Select";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /*
     * Currently not used; the renderer needs to be updated to support labeling the radio/checkbox
     */
    public String getLabel() {
        if (label != null) {
            return label;
        }

        return (String) getBinding("label");
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /*
     * The attribute off of which other components are registered, such as AllSelect and SelectCommandButton
     */
    public String getName() {
        if (name != null) {
            return name;
        }

        return (String) getBinding("name");
    }

    public void setName(String name) {
        this.name = name;
    }

    /*
     * Each of this component's instances, when selected, will fire a javascript method which enabled / disables itself
     * conditionally
     */
    public String getOnclick() {
        return "updateButtons('" + getName() + "');";
    }

    public String getOnchange() {
        return this.onchange;
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    /*
     * This is the content that will be stored behind the radio/checkbox dom element; when the form is submitted, this
     * is the value that will be passed to the managed bean.
     */
    @Override
    public Object getValue() {
        if (value != null) {
            return value;
        }

        return getBinding("value");
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    /*
     * "radio" or "checkbox" - i.e. select one or select many
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /*
     * It is preferred to use the disabled attribute of this component instead of the rendered attribute.  This will
     * provide consistency to the rendered table such as regardless of what items appear in the table, the check-able
     * column will always be rendered with a consistent thickness.
     */
    public Boolean isDisabled() {
        if (disabled != null) {
            return disabled;
        }

        try {
            disabled = (Boolean) getBinding("disabled");
        } catch (NullPointerException npe) {
            return false;
        }

        return (disabled != null) && disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[6];
        values[0] = super.saveState(context);
        values[1] = name;
        values[2] = label;
        values[3] = value;
        values[4] = disabled;
        values[5] = type;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        name = (String) values[1];
        label = (String) values[2];
        value = values[3];
        disabled = (Boolean) values[4];
        type = (String) values[5];
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