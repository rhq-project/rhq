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

import javax.faces.context.FacesContext;

public class AllSelect extends Select {
    private String target;

    public static final String COMPONENT_TYPE = "org.jboss.on.AllSelect";
    public static final String COMPONENT_FAMILY = "org.jboss.on.AllSelect";

    public AllSelect() {
        /*
         * This is always a checkbox type for now.  Perhaps in the future we can turn it into a small icon/image
         * instead; if so, it would need a new renderer.
         */
        setType("checkbox");
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /*
     * This is the static string (not a value-binding expression) that represents the name (not id) of the Select
     * component against which this should be registered.
     *
     * Checking this component will select all instances having this name. Unchecking this component will deselect all
     * instances having this name.
     */
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /*
     * For easier maint, the name of the javascript method should mirror the component. However, the implementation
     * should be maintained outside of the component so that the component can be updated independent of the javascript,
     * and vica versa.
     */
    @Override
    public String getOnclick() {
        return "selectAll(this, '" + target + "');";
    }

    /*
     * Since this component extends AllSelect, and since AllSelect may or may not be enabled (to support the case where
     * we want to supress the current configuration history element from, say, being deleted) this method should return
     * false so it is always enabled.
     */
    @Override
    public Boolean isDisabled() {
        return false;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = target;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        target = (String) values[1];
    }
}